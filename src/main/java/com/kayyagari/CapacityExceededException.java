package com.kayyagari;

/**
 * An exception that gets thrown by LoadBalancer when
 * it cannot serve requests due to the overload conditions.
 * 
 * Note: Intentionally made this a sub-type of RuntimeException.
 *       I do not think any client catches an LoadBalancer's exceptions on the client side ;) 
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class CapacityExceededException extends RuntimeException {
    public CapacityExceededException(String msg) {
        super(msg);
    }
}
