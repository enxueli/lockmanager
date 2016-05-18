package com.paul.lckmgr.common;

import java.io.Serializable;

/**
 * Created by paul on 5/13/16.
 */
public class Request implements Serializable {
    private String lockName;
    private String lockHolder;
    private RequestType requestType;

    public enum RequestType {
        TRYLOCK, LOCK, UNLOCK
    }

    public Request(String lockName, String lockHolder, RequestType requestType) {
        this.lockName = lockName;
        this.lockHolder = lockHolder;
        this.requestType = requestType;
    }

    public String getLockName() {
        return lockName;
    }

    public String getLockHolder() {
        return lockHolder;
    }

    public RequestType getRequestType() {
        return requestType;
    }
}
