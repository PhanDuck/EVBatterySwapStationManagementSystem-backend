package com.evbs.BackEndEvBs.constants;

public class StationStatus {

    public static final String ACTIVE = "Active";
    public static final String INACTIVE = "Inactive";
    public static final String MAINTENANCE = "Maintenance";
    public static final String CLOSED = "Closed";
    public static final String UNDER_CONSTRUCTION = "Under Construction";

    // Get all statuses as array (useful for validation)
    public static String[] getAllStatuses() {
        return new String[]{
                ACTIVE,
                INACTIVE,
                MAINTENANCE,
                CLOSED,
                UNDER_CONSTRUCTION
        };
    }

    // Check if status is valid
    public static boolean isValidStatus(String status) {
        for (String validStatus : getAllStatuses()) {
            if (validStatus.equals(status)) {
                return true;
            }
        }
        return false;
    }

    // Check if station is operational
    public static boolean isOperational(String status) {
        return ACTIVE.equals(status);
    }
}