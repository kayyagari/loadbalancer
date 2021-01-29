package com.kayyagari;

import java.util.List;

public interface LoadBalancingStrategy {
    Provider next(List<Provider> providers);
}
