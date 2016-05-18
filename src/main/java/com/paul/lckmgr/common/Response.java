package com.paul.lckmgr.common;

/**
 * Created by paul on 5/13/16.
 */
public class Response {
    private String lockName;
    private String lockHolder;
    private ResponseStatus responseStatus;

    public enum ResponseStatus {
        OK, FAIL
    }

    public Response(String lockName, String lockHolder, ResponseStatus responseStatus) {
        this.lockName = lockName;
        this.lockHolder = lockHolder;
        this.responseStatus = responseStatus;
    }

    public String getLockName() {
        return lockName;
    }

    public String getLockHolder() {
        return lockHolder;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }
}
