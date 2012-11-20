package edu.mit.printAtMIT.model.touchstone.internal;

public class MobileRequestState {
    public final MobileRequest request;
    public final int state;
    public final boolean followRedirects;

    public MobileRequestState(int state, MobileRequest request) {
        this.state = state;
        this.request = request;
        this.followRedirects = true;
    }
    
        public MobileRequestState(int state, MobileRequest request, boolean followRedirects) {
        this.state = state;
        this.request = request;
        this.followRedirects = followRedirects;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MobileRequestState other = (MobileRequestState) obj;
        if (this.request != other.request && (this.request == null || !this.request.equals(other.request))) {
            return false;
        }
        if (this.state != other.state) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.request != null ? this.request.hashCode() : 0);
        hash = 83 * hash + this.state;
        return hash;
    }
    
    
}