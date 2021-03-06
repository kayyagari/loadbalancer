package com.kayyagari;

import java.util.UUID;

/**
 * A Provider that responds from get() method after the the given delay in milliseconds.
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class SlowResponseProviderImpl implements Provider {
    private String id;
    private long delay;

    public SlowResponseProviderImpl(long delayMillis) {
        this.id = UUID.randomUUID().toString();
        this.delay = delayMillis;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String get() {
        try {
            Thread.sleep(delay);
        }
        catch(InterruptedException e) {
            // ignore
        }

        return id;
    }

    @Override
    public boolean check() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SlowResponseProviderImpl other = (SlowResponseProviderImpl) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
