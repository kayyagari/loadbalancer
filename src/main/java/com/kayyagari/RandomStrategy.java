package com.kayyagari;

import java.util.List;
import java.util.Random;

/**
 * An implementation of random load-balancing strategy
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class RandomStrategy implements LoadBalancingStrategy {

    private Random rnd = new Random(System.currentTimeMillis());

    @Override
    public synchronized Provider next(List<Provider> providers) {
        int index = rnd.nextInt(providers.size());
        // return index will always be between 0 - (size()-1)
        return providers.get(index);
    }

    @Override
    public String toString() {
        return "random";
    }
}
