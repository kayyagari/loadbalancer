package com.kayyagari;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

/**
 * Checks the functionality of the LoadBalancer with a mix of Provider implementations
 * and a large number of requests sent in parallel.
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class LoadBalancerStressTest {
    private LoadBalancer lb;

    @Before
    public void setup() {
        lb = new LoadBalancer(new RoundRobinStrategy());
        for(int i=0; i < 6; i++) {
            lb.add(new SimpleProviderImpl(i+1));
        }

        lb.add(new SlowResponseProviderImpl(5));
        lb.add(new SlowResponseProviderImpl(5));

        lb.add(new PeriodicallyUnhealthyProviderImpl(5));
        lb.add(new PeriodicallyUnhealthyProviderImpl(9));
    }
    
    @Test
    public void testWithSeveralThreads() throws Exception {
        AtomicInteger exclusionCount = new AtomicInteger();
        AtomicInteger nullMsgCount = new AtomicInteger();

        AtomicInteger totalMsgCount = new AtomicInteger();
        int maxThreads = 100;
        AtomicBoolean stop = new AtomicBoolean();

        Runnable excludedMonitor = new Runnable() {
            @Override
            public void run() {
                try {
                    while(!stop.get()) {
                        Thread.sleep(5000);
                        int n = lb.getInactiveProviders().size();
                        exclusionCount.addAndGet(n);
                    }
                }
                catch(InterruptedException e) {
                    // ignore
                }
            }
        };

        new Thread(excludedMonitor).start();

        for(int i=0; i<maxThreads; i++) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    while(!stop.get()) {
                        try {
                            totalMsgCount.incrementAndGet();
                            String val = lb.get().get();
                            if(val == null) {
                                nullMsgCount.incrementAndGet();
                            }
                        }
                        catch(CancellationException | ExecutionException | InterruptedException | CapacityExceededException e) {
                            // ignore them
                        }
                    }
                }
            };
            new Thread(r).start();
        }
        
        Thread.sleep(15 * 1000);
        
        stop.set(true);

        System.out.println("Total number of requests sent in parallel " + totalMsgCount.get());

        // there should be ZERO null messages
        assertFalse(nullMsgCount.get() > 0);
        
        // at least one of the Providers might have been excluded automatically
        assertTrue(exclusionCount.get() > 0);
        
        // at least 8 Providers should remain active the remaining two are of type PeriodicallyUnhealthyProviderImpl 
        // so they may or may not be present at this instant of time
        assertTrue(lb.getProviders().size() >= 8);
    }
}
