package com.kayyagari;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * The tests in this class cover the requirements mentioned in Steps 1 to 8.
 * 
 * LoadBalancerStressTest covers some additional concurrency related tests. 
 * 
 * @author Kiran Ayyagari (kayyagari@apache.org)
 * @see LoadBalancerStressTest
 */
public class LoadBalancerTest {
    private LoadBalancer lb;

    private String prefix = SimpleProviderImpl.class.getSimpleName() + "-";

    @Before
    public void setup() {
        lb = new LoadBalancer(new RoundRobinStrategy());
        
        //Step 2 – Register a list of providers
        for(int i=0; i < LoadBalancer.MAX_NUM_PROVIDERS; i++) {
            lb.add(new SimpleProviderImpl(i+1));
        }
        
        assertEquals(LoadBalancer.MAX_NUM_PROVIDERS, lb.getProviders().size());
        assertEquals(0, lb.getInactiveProviders().size());
    }

    // Step 3 – Random invocation
    @Test
    public void testRandomGet() throws Exception {
        lb.changeStrategy(new RandomStrategy());
        String randomVal = lb.get().get();
        assertTrue(randomVal.startsWith(prefix));
        
        // test to ensure that RandomStrategy doesn't fail with IndexOutOfBoundsException
        for(int i=0; i < 1000; i++) {
            randomVal = lb.get().get();
            assertTrue(randomVal.startsWith(prefix));
        }
    }

    // Step 4 – Round Robin invocation
    @Test
    public void testRoundRobinGet() throws Exception {
        int[] expected = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4};
        for(int e : expected) {
            String randomVal = lb.get().get();
            assertEquals(randomVal, prefix + e);
        }
    }
    
    // Step 5 – Manual node exclusion / inclusion
    @Test
    public void testManualExcludeInclude() {
        String id = prefix + 1;
        boolean excluded = lb.exclude(id);
        assertTrue(excluded);
        Provider p = lb.getInactiveProviders().get(id);
        assertNotNull(p);
        assertFalse(lb.getProviders().contains(p));
        assertEquals(LoadBalancer.MAX_NUM_PROVIDERS - 1, lb.getProviders().size());
        assertEquals(1, lb.getInactiveProviders().size());
        
        boolean included = lb.include(id);
        assertTrue(included);
        assertTrue(lb.getProviders().contains(p));
        assertEquals(LoadBalancer.MAX_NUM_PROVIDERS, lb.getProviders().size());
        assertTrue(lb.getInactiveProviders().isEmpty());
    }
    
    // Step 6 – Heart beat checker
    // and
    // Step 7 – Improving Heart beat checker
    @Test
    public void testHealthCheck() throws Exception {
        // remove a SimpleProvider
        lb.removeActiveProviderAt(9);
        
        // then add an unhealthy provider
        PeriodicallyUnhealthyProviderImpl unhealthyProv = new PeriodicallyUnhealthyProviderImpl(6);
        lb.add(unhealthyProv);
        assertEquals(LoadBalancer.MAX_NUM_PROVIDERS, lb.getProviders().size());
        assertTrue(lb.getInactiveProviders().isEmpty());

        Thread.sleep(7 * 1000); // sleep until the provider gets excluded
        
        assertEquals(LoadBalancer.MAX_NUM_PROVIDERS - 1, lb.getProviders().size());
        assertEquals(1, lb.getInactiveProviders().size());
        assertTrue(lb.getInactiveProviders().containsKey(unhealthyProv.id()));

        // sleep for some more time for the provider *to be considered* healthy again
        // it gets healthier much before but a node will be included only when it 
        // reports healthy for two consecutive checks 
        Thread.sleep(5 * 1000);
        
        assertEquals(LoadBalancer.MAX_NUM_PROVIDERS, lb.getProviders().size());
        assertEquals(0, lb.getInactiveProviders().size());
        assertFalse(lb.getInactiveProviders().containsKey(unhealthyProv.id()));
    }
    
    // Step 8 – Cluster Capacity Limit
    @Test
    public void testClusterCapacity() throws Exception {
        lb = new LoadBalancer(new RoundRobinStrategy());
        
        for(int i=0; i < LoadBalancer.MAX_NUM_PROVIDERS; i++) {
            lb.add(new SlowResponseProviderImpl(10)); // this provider sleeps for 10 milliseconds before returning from get()
        }
        
        _testClusterCapcity(101);

        // wait for >= 1sec so that all the tasks submitted will execute there by decrementing pendingReqCount all the way to ZERO
        // without this the subsequent call to _testClusterCapcity() will fail immediately
        Thread.sleep(1000);

        // remove one provider and test
        lb.removeActiveProviderAt(9);
        _testClusterCapcity(91);
    }
    
    private void _testClusterCapcity(int expectedFailureAtReqNo) {
        int i = 1;
        try {
            for(; i<= expectedFailureAtReqNo; i++ ) {
                lb.get();
            }
            fail("an exception is expected due to LoadBalancer being under excessive load");
        }
        catch(CapacityExceededException ce) {
            //ce.printStackTrace();
            assertTrue(true);
            assertEquals(expectedFailureAtReqNo, i);
        }
    }
}
