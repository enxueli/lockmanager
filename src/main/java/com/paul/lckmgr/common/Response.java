package com.paul.lckmgr.common;

/**
 * Created by paul on 5/17/16.
 */
public class Response {
    private String clientId;
    private ResponseStatus responseStatus;

    private enum ResponseStatus {
        OK, FAIL
    }

    public Response(String clientId, ResponseStatus responseStatus) {
        this.clientId = clientId;
        this.responseStatus = responseStatus;
    }

    public String getClientId() {
        return clientId;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }
}
