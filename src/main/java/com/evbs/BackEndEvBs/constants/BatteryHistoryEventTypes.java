package com.evbs.BackEndEvBs.constants;

public class BatteryHistoryEventTypes {

    // Battery Swap Events
    public static final String SWAP_OUT = "SWAP_OUT";          // Battery được swap ra khỏi station
    public static final String SWAP_IN = "SWAP_IN";            // Battery được trả về station
    public static final String SWAP_COMPLETE = "SWAP_COMPLETE"; // Hoàn thành swap

    // Charging Events
    public static final String CHARGING_START = "CHARGING_START"; // Bắt đầu sạc
    public static final String CHARGING_END = "CHARGING_END";   // Kết thúc sạc
    public static final String CHARGING_FAILED = "CHARGING_FAILED"; // Sạc thất bại

    // Maintenance Events
    public static final String MAINTENANCE_START = "MAINTENANCE_START"; // Bắt đầu bảo trì
    public static final String MAINTENANCE_END = "MAINTENANCE_END";   // Kết thúc bảo trì
    public static final String MAINTENANCE_SCHEDULED = "MAINTENANCE_SCHEDULED"; // Lên lịch bảo trì

    // Health & Status Events
    public static final String HEALTH_CHECK = "HEALTH_CHECK";   // Kiểm tra sức khỏe
    public static final String SOH_UPDATE = "SOH_UPDATE";       // Cập nhật State of Health
    public static final String STATUS_CHANGE = "STATUS_CHANGE"; // Thay đổi trạng thái

    // Location Events
    public static final String LOCATION_CHANGE = "LOCATION_CHANGE"; // Thay đổi vị trí
    public static final String STATION_ASSIGNED = "STATION_ASSIGNED"; // Gán vào station
    public static final String STATION_REMOVED = "STATION_REMOVED"; // Gỡ khỏi station

    // Lifecycle Events
    public static final String BATTERY_CREATED = "BATTERY_CREATED"; // Tạo mới battery
    public static final String BATTERY_RETIRED = "BATTERY_RETIRED"; // Nghỉ hưu
    public static final String BATTERY_DECOMMISSIONED = "BATTERY_DECOMMISSIONED"; // Ngừng sử dụng

    // System Events
    public static final String SYSTEM_UPDATE = "SYSTEM_UPDATE"; // Cập nhật hệ thống
    public static final String AUTOMATIC_CHECK = "AUTOMATIC_CHECK"; // Kiểm tra tự động

    // Get all event types as list (useful for validation)
    public static String[] getAllEventTypes() {
        return new String[]{
                SWAP_OUT, SWAP_IN, SWAP_COMPLETE,
                CHARGING_START, CHARGING_END, CHARGING_FAILED,
                MAINTENANCE_START, MAINTENANCE_END, MAINTENANCE_SCHEDULED,
                HEALTH_CHECK, SOH_UPDATE, STATUS_CHANGE,
                LOCATION_CHANGE, STATION_ASSIGNED, STATION_REMOVED,
                BATTERY_CREATED, BATTERY_RETIRED, BATTERY_DECOMMISSIONED,
                SYSTEM_UPDATE, AUTOMATIC_CHECK
        };
    }

    // Check if event type is valid
    public static boolean isValidEventType(String eventType) {
        for (String validType : getAllEventTypes()) {
            if (validType.equals(eventType)) {
                return true;
            }
        }
        return false;
    }
}