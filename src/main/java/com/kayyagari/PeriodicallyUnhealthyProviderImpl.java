package com.kayyagari;

import java.util.UUID;

/**
 * A Provider that becomes "sick" regularly at the given interval
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class PeriodicallyUnhealthyProviderImpl implements Provider {
    private String id;
    private long intervalMillis;
    private long prevCheckAt;

    public PeriodicallyUnhealthyProviderImpl(int intervalSec) {
        this.id = UUID.randomUUID().toString();
        this.intervalMillis = intervalSec * 1000L;
        this.prevCheckAt = System.currentTimeMillis();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public boolean check() {
        long now = System.currentTimeMillis();
        if((now - prevCheckAt) >= intervalMillis) {
            prevCheckAt = now;
            return false;
        }

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
        PeriodicallyUnhealthyProviderImpl other = (PeriodicallyUnhealthyProviderImpl) obj;
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
