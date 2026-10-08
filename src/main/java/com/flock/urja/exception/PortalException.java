package com.flock.urja.exception;

public class PortalException extends RuntimeException {
    public PortalException(String msg) { super(msg); }
    public PortalException(String msg, Throwable cause) { super(msg, cause); }
}
