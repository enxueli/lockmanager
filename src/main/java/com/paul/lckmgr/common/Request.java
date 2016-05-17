package com.paul.lckmgr.common;

import java.io.Serializable;

/**
 * Created by paul on 5/17/16.
 */
public class Request implements Serializable {
    private String clientId;
    private String lockName;
    private RequestType requestType;

    public enum RequestType {
        TRYLOCK, LOCK, UNLOCK
    }

    public Request(String clientId, String lockName, RequestType requestType) {
        this.clientId = clientId;
        this.lockName = lockName;
        this.requestType = requestType;
    }

    public String getClientId() {
        return clientId;
    }

    public String getLockName() {
        return lockName;
    }

    public RequestType getRequestType() {
        return requestType;
    }
}
