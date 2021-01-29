package com.kayyagari;

// intentionally made it a type of RuntimeException
public class CapacityExceededException extends RuntimeException {
    public CapacityExceededException(String msg) {
        super(msg);
    }
}
