package com.kayyagari;

/**
 * The interface for all providers.
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public interface Provider {
    /**
     * Returns a value generated by the provider
     */
    String get();

    /**
     * Returns the state of provider's health
     *
     * @return true if healthy, false otherwise
     */
    boolean check();

    /**
     * Returns the ID of the provider
     */
    String id();
}
