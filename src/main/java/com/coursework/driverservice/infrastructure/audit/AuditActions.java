package com.coursework.driverservice.infrastructure.audit;

public final class AuditActions {

    public static final String TRIP_CREATED = "TRIP_CREATED";
    public static final String TRIP_COMPLETED = "TRIP_COMPLETED";
    public static final String TRIP_CANCELLED = "TRIP_CANCELLED";
    public static final String WORK_SESSION_STARTED = "WORK_SESSION_STARTED";
    public static final String WORK_SESSION_CLOSED = "WORK_SESSION_CLOSED";
    public static final String USER_CREATED = "USER_CREATED";

    private AuditActions() {
    }
}
