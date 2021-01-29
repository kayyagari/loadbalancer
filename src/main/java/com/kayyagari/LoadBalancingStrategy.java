package com.kayyagari;

import java.util.List;

/**
 * Interface definition for loadbalancing strategy
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public interface LoadBalancingStrategy {
    Provider next(List<Provider> providers);
}
