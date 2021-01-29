package com.kayyagari;

/**
 * A simple implementation of the Provider.
 * The constructor was made to accept an integer for the sake of testability.
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class SimpleProviderImpl implements Provider {
    private String id;
    
    // the nameSuffix is to help in unit tests
    // there is no other significance to it
    public SimpleProviderImpl(int nameSuffix) {
        this.id = this.getClass().getSimpleName() + "-" + nameSuffix;
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
        SimpleProviderImpl other = (SimpleProviderImpl) obj;
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
