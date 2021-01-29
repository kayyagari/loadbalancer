package com.kayyagari;

import java.util.List;

/**
 * An implementation of round-robin load-balancing strategy
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class RoundRobinStrategy implements LoadBalancingStrategy {

    /** index of the next provider to be selected */
    private int nextIdx = 0;

    // must be synchronized for thread safety
    public synchronized Provider next(List<Provider> providers) {
        // this check is to accommodate any newly added providers
        int len = providers.size();
        if(nextIdx == len) {
            nextIdx = 0;
        }

        Provider p = providers.get(nextIdx);
        nextIdx++;
        
        return p;
    }

    @Override
    public String toString() {
        return "round-robin";
    }
}
