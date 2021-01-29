package com.kayyagari;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalancer {
    /** the active providers */
    private List<Provider> providers;

    // actually here I need a thread-safe Set but ConcurrentHashMap is better than CopyOnWriteArraySet
    private Map<String, Provider> inactiveProviders;
    
    /** the strategy to be used for balancing load */
    private LoadBalancingStrategy strategy;

    /** threadpool for checking the heartbeats of providers */
    private ScheduledThreadPoolExecutor healthCheckExecutor;

    /** threadpool for processing the incoming requests */
    private ThreadPoolExecutor requestPool;

    public static final int MAX_NUM_PROVIDERS = 10;

    public static final int MAX_REQ_PER_PROVIDER = 10;

    /** used for monitoring the pending request in a naive way */
    private AtomicInteger pendingReqCount = new AtomicInteger(0);

    /** used for storing the max request handling capacity of the LoadBalancer */
    private AtomicInteger maxReqCapacity = new AtomicInteger(0);

    public LoadBalancer(LoadBalancingStrategy strategy) {
        this.providers = new CopyOnWriteArrayList<>();
        this.inactiveProviders = new ConcurrentHashMap<>();
        this.strategy = strategy;
        
        // half the size of the total number of providers, this still 
        // doesn't eliminate possibility of stuck threads if a provider takes too long to return from check() method
        healthCheckExecutor = new ScheduledThreadPoolExecutor(MAX_NUM_PROVIDERS/2);
        
        requestPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_NUM_PROVIDERS);
    }

    // this method should not be synchronized
    public Future<String> get() {
        if(pendingReqCount.get() >= maxReqCapacity.get()) {
            throw new CapacityExceededException("processing capacity exceeded, max capacity = " + maxReqCapacity);
        }

        pendingReqCount.incrementAndGet();
        Callable<String> c = new Callable<String>() {

            @Override
            public String call() throws Exception {
                String val = null;
                if(!providers.isEmpty()) {
                    val = strategy.next(providers).get();
                }
                pendingReqCount.decrementAndGet();
                return val; 
            }
            
        };

        return requestPool.submit(c);
    }

    public synchronized void add(Provider p) {
        if(providers.size() == MAX_NUM_PROVIDERS) {
            throw new IllegalStateException("provider list is full, cannot add new providers. Only a maximum of 10 providers are allowed");
        }
        
        HealthAwareProviderWrapper hw = new HealthAwareProviderWrapper(p, this);
        providers.add(hw);
        updateMaxReqCapacity();
    }

    private synchronized boolean exclude(Provider p) {
         boolean excluded = providers.remove(p);
         if(excluded) {
             inactiveProviders.put(p.id(), p);
             updateMaxReqCapacity();
         }
         
         return excluded;
    }

    private synchronized boolean include(Provider p) {
        Provider included = inactiveProviders.remove(p.id());
        if(included != null) {
            providers.add(p);
            updateMaxReqCapacity();
        }

        return (included != null);
    }

    // public API for manual inclusion
    public synchronized boolean include(String id) {
        Provider p = inactiveProviders.remove(id);
        if(p != null) {
            HealthAwareProviderWrapper hw = (HealthAwareProviderWrapper)p;
            providers.add(p);
            updateMaxReqCapacity();
            hw.resetFlags(false);
        }
        
        return (p != null);
    }

    // public API for manual exclusion
    public synchronized boolean exclude(String id) {
        for(Provider p : providers) {
            if(p.id().equals(id)) {
                providers.remove(p);
                HealthAwareProviderWrapper hw = (HealthAwareProviderWrapper)p;
                inactiveProviders.put(id, p);
                updateMaxReqCapacity();
                hw.resetFlags(true);
                return true;
            }
        }
        
        return false;
    }

    /**
     * updates the max request capacity. This should be called whenever the active providers list changes 
     */
    private synchronized void updateMaxReqCapacity() {
        maxReqCapacity.set(providers.size() * MAX_REQ_PER_PROVIDER);
    }

    /**
     * A wrapper for Provider instances to help in handling the automatic exclusion and inclusion of
     * wrapped Providers based on their health. 
     */
    private static class HealthAwareProviderWrapper implements Provider {
        private Provider wrapped;
        private LoadBalancer lb;
        private int successCount;
        private boolean excluded;

        private ScheduledFuture<?> future;
        private Runnable command = new Runnable() {
            @Override
            public void run() {
                check();
            }
        };

        private HealthAwareProviderWrapper(Provider wrapped, LoadBalancer lb) {
            this.wrapped = wrapped;
            this.lb = lb;
            enableHealthCheck();
        }

        private void enableHealthCheck() {
            future = lb.healthCheckExecutor.scheduleWithFixedDelay(command, 0, 2, TimeUnit.SECONDS);
        }

        // need to synchronize due to the requirement - Step 5 – Manual node exclusion / inclusion
        @Override
        public synchronized boolean check() {
            boolean h = wrapped.check();
            if(h) {
                //System.out.println(wrapped.id() + " is healthy");
                successCount++;
                if(excluded && successCount == 2) {
                    lb.include(this);
                }
            }
            else {
                //System.out.println(wrapped.id() + " is *unhealthy*");
                if(!excluded) {
                    lb.exclude(this);
                    excluded = true;
                }
            }
            
            // prevent overflow
            if(successCount > 2) {
                successCount = 0;
            }

            return h;
        }
        
        // resets the successCount and excluded flags
        // called by the include(String) and exclude(String) methods 
        public synchronized void resetFlags(boolean excluded) {
            future.cancel(true);
            successCount = 0;
            this.excluded = excluded;
            enableHealthCheck();
        }

        public synchronized boolean cancel() {
            return future.cancel(true);
        }

        @Override
        public String get() {
            return wrapped.get();
        }
        
        @Override
        public String id() {
            return wrapped.id();
        }
    }
    
    // ----- helper methods for unit testing
    
    /*default protected*/ List<Provider> getProviders() {
        return providers;
    }

    /*default protected*/ Map<String, Provider> getInactiveProviders() {
        return inactiveProviders;
    }
    
    /*default protected*/ void changeStrategy(LoadBalancingStrategy strategy) {
        this.strategy = strategy;
    }
    
    /*default protected*/ synchronized void removeActiveProviderAt(int index) {
        HealthAwareProviderWrapper hw = (HealthAwareProviderWrapper) providers.remove(index);
        hw.cancel();
        updateMaxReqCapacity();        
    }
}
