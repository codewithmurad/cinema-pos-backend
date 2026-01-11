package com.telecamnig.cinemapos.utility;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {
    
    private Constants() {
        // prevent instantiation
    }
    
    // ==================== ROLE & AUTH CONSTANTS ====================
    
    /**
     * Prefix for Spring Security roles (ROLE_* convention)
     */
    public static final String ROLE_PREFIX = "ROLE_";
    
    // ==================== LAYOUT & SEATING CONSTANTS ====================
    
//    /**
//     * Canvas dimensions for seat layout rendering (pixels)
//     * Defines the virtual canvas size where seats are positioned
//     */
//    public static final int CANVAS_WIDTH = 1200;
//    public static final int CANVAS_HEIGHT = 800;
//    
//    /**
//     * Standard seat dimensions for visual representation (pixels)
//     */
//    public static final int SEAT_WIDTH = 40;
//    public static final int SEAT_HEIGHT = 40;
//    
//    /**
//     * Spacing between seats and rows for layout calculation (pixels)
//     */
//   // public static final int SEAT_SPACING = 80;
//   //  public static final int ROW_SPACING = 50;
//    public static final int SEAT_SPACING = 60; // reduced so seats are visually closer (was 80)
//    public static final int ROW_SPACING = 100;  // increased vertical space to allow walking aisle
//    
//    /**
//     * Screen position constants for layout JSON
//     */
//    public static final int SCREEN_POSITION_X = 400;
//    public static final int SCREEN_POSITION_Y = 50;
//    public static final int SCREEN_WIDTH = 400;
//    public static final int SCREEN_HEIGHT = 30;
//    
//    /**
//     * Aisle dimensions and positioning
//     */
//    public static final int AISLE_WIDTH = 60;
//    public static final int AISLE_HEIGHT_NORMAL = 600;
//    public static final int AISLE_HEIGHT_VIP = 400;
//    public static final int AISLE_Y_POSITION = 100;
//    
//    /**
//     * Base X coordinates for different layout orientations
//     */
//    public static final int BASE_X_LEFT = 100;
//    public static final int BASE_X_RIGHT = 900;
//    public static final int BASE_X_VIP_LEFT = 200;
//    public static final int BASE_X_VIP_RIGHT = 800;
    
 // Canvas dimensions for seat layout rendering (pixels)
    public static final int CANVAS_WIDTH = 800;   // super compact (was 860)
    public static final int CANVAS_HEIGHT = 710;  // slightly reduced vertically

    // Standard seat dimensions
    public static final int SEAT_WIDTH = 40;
    public static final int SEAT_HEIGHT = 40;

    // Seat and row spacing
    public static final int SEAT_SPACING = 55; 
    public static final int ROW_SPACING = 90;  

    // Screen positioning (kept centered)
    public static final int SCREEN_WIDTH = 400;
    public static final int SCREEN_HEIGHT = 30;
    public static final int SCREEN_POSITION_X = (CANVAS_WIDTH - SCREEN_WIDTH) / 2;
    public static final int SCREEN_POSITION_Y = 38;

    // Aisle positioning
    public static final int AISLE_WIDTH = 60;
    public static final int AISLE_HEIGHT_NORMAL = 600;
    public static final int AISLE_HEIGHT_VIP = 240;
    public static final int AISLE_Y_POSITION = 60;
    
    public static final int AISLE_VERTICAL_SHIFT_REGULAR = 18;

    // Base X coordinates for seat placement (keeps seat alignment perfect)
    public static final int BASE_X_LEFT = 100;                 
    public static final int BASE_X_RIGHT = CANVAS_WIDTH - BASE_X_LEFT;
    public static final int BASE_X_VIP_LEFT = 200;
    public static final int BASE_X_VIP_RIGHT = CANVAS_WIDTH - BASE_X_VIP_LEFT;
    
    /**
     * VIP sofa configuration constants
     */
    public static final int SOFA_SPACING = 120;
    public static final int SOFA_SEAT_OFFSET = 20;
    public static final int SOFA_SEAT_WIDTH = 35;
    public static final int SOFA_SEAT_HEIGHT = 35;
    public static final int AISLE_VERTICAL_OFFSET_VIP = 40; // tune this

    
    // ==================== JSON METADATA KEY CONSTANTS ====================
    
    /**
     * Keys for seat metaJson (individual seat positioning)
     */
    public static final String META_KEY_X = "x";
    public static final String META_KEY_Y = "y";
    public static final String META_KEY_WIDTH = "w";
    public static final String META_KEY_HEIGHT = "h";
    public static final String META_KEY_TYPE = "type";
    public static final String META_KEY_ICON = "icon";
    public static final String META_KEY_ROTATION = "rotation";
    
    /**
     * Values for metaJson properties
     */
    public static final String META_ICON_SOFA = "sofa";
    public static final String META_ICON_SEAT = "seat";
    public static final String META_ICON_WHEELCHAIR = "wheelchair";
    
    /**
     * Keys for screen layoutJson (cinema hall structure)
     */
    public static final String LAYOUT_KEY_VERSION = "version";
    public static final String LAYOUT_KEY_WIDTH = "width";
    public static final String LAYOUT_KEY_HEIGHT = "height";
    public static final String LAYOUT_KEY_BACKGROUND = "background";
    public static final String LAYOUT_KEY_SCREEN_POSITION = "screenPosition";
    public static final String LAYOUT_KEY_AISLES = "aisles";
    public static final String LAYOUT_KEY_ROWS = "rows";
    public static final String LAYOUT_KEY_SOFAS = "sofas";
    public static final String LAYOUT_KEY_METADATA = "metadata";
    
    /**
     * Sub-keys for screen position in layoutJson
     */
    public static final String POSITION_KEY_X = "x";
    public static final String POSITION_KEY_Y = "y";
    public static final String POSITION_KEY_WIDTH = "width";
    public static final String POSITION_KEY_HEIGHT = "height";
    
    /**
     * Keys for aisle definitions in layoutJson
     */
    public static final String AISLE_KEY_ID = "id";
    public static final String AISLE_KEY_LABEL = "label";
    public static final String AISLE_KEY_COLOR = "color";
    
    /**
     * Keys for row definitions in layoutJson
     */
    public static final String ROW_KEY_ID = "id";
    public static final String ROW_KEY_LABEL = "label";
    public static final String ROW_KEY_SEAT_TYPE = "seatType";
    
    /**
     * Keys for screen metadata in layoutJson
     */
    public static final String META_KEY_TOTAL_SEATS = "totalSeats";
    public static final String META_KEY_CAPACITY = "capacity";
    public static final String META_KEY_REGULAR_SEATS = "regularSeats";
    public static final String META_KEY_GOLD_SEATS = "goldSeats";
    public static final String META_KEY_PREMIUM_SEATS = "premiumSeats";
    public static final String META_KEY_SOFA_GROUPS = "sofaGroups";
    public static final String META_KEY_SOFA_SEATS = "sofaSeats";
    
    /**
     * Layout version and styling constants
     */
    public static final String LAYOUT_VERSION = "1.0";
    public static final String BACKGROUND_NORMAL = "#1a1a1a";
    public static final String BACKGROUND_VIP = "#2d1b69";
    public static final String AISLE_COLOR_NORMAL = "#333333";
    public static final String AISLE_COLOR_VIP = "#4a3c6e";
    
 // ==================== SCREEN CONFIGURATION CONSTANTS ====================

    // base row Y position (front row A). We'll compute other rows from this.
    public static final int BASE_ROW_Y = 620; // front row (A)
    
    /**
     * Normal screen row labels and Y positions (pixels from top)
     * Row A is closest to screen (highest Y), Row G is farthest (lowest Y)
     * We're computing them so it's easier to tweak ROW_SPACING centrally.
     */
    public static final Map<String, Integer> NORMAL_SCREEN_ROW_POSITIONS;
    
    static {
        Map<String, Integer> map = new HashMap<>();
        // Order: A (front) -> G (back)
        // A = BASE_ROW_Y, B = BASE_ROW_Y - ROW_SPACING, etc.
        map.put("A", BASE_ROW_Y);
        map.put("B", BASE_ROW_Y - ROW_SPACING);
        map.put("C", BASE_ROW_Y - ROW_SPACING * 2);
        map.put("D", BASE_ROW_Y - ROW_SPACING * 3);
        map.put("E", BASE_ROW_Y - ROW_SPACING * 4);
        map.put("F", BASE_ROW_Y - ROW_SPACING * 5);
        map.put("G", BASE_ROW_Y - ROW_SPACING * 6);
        NORMAL_SCREEN_ROW_POSITIONS = Collections.unmodifiableMap(map);
    }

    /**
     * VIP screen row labels and Y positions (pixels from top)
     */
    public static final Map<String, Integer> VIP_SCREEN_ROW_POSITIONS;
    static {
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 500); // Front row
        map.put("B", 400); // Middle row
        map.put("C", 300); // Back row
        VIP_SCREEN_ROW_POSITIONS = Collections.unmodifiableMap(map);
    }

    /**
     * Normal screen seat configuration by row
     * Format: Map<RowLabel, Map<"count"|"type"|"positions", Value>>
     * positions = LIST OF COLUMN INDICES (i.e. column index used in coordinate calculation)
     * Note: Labels (A1/A2/...) will be taken from NORMAL_SCREEN_SEAT_LABELS if present,
     * otherwise we use the same numbers as positions.
     */
    public static final Map<String, Map<String, Object>> NORMAL_SCREEN_SEAT_CONFIG;
    static {
        Map<String, Map<String, Object>> config = new HashMap<>();
        
        // Row A uses columns [1,2,6] (labels will be default -> A1,A2,A6)
        config.put("A", createRowConfig(3, SeatType.REGULAR, Arrays.asList(1, 2, 6)));
        config.put("B", createRowConfig(9, SeatType.REGULAR, null));
        config.put("C", createRowConfig(9, SeatType.REGULAR, null));
        config.put("D", createRowConfig(9, SeatType.GOLD, null));
        config.put("E", createRowConfig(9, SeatType.GOLD, null));
        // Row F uses columns 3..9
        config.put("F", createRowConfig(7, SeatType.PREMIUM, Arrays.asList(3,4,5,6,7,8,9)));

        /*
         * Row G: We want labels G7 & G8, but placed above F8 & F9 respectively.
         * So we set column indices to [2,3,4,8,9] (positions where seats sit).
         * And we provide custom labels mapping so numbers become [2,3,4,7,8] (G2,G3,G4,G7,G8).
         */
        config.put("G", createRowConfig(5, SeatType.PREMIUM, Arrays.asList(2,3,4,8,9)));
        
        NORMAL_SCREEN_SEAT_CONFIG = Collections.unmodifiableMap(config);
    }
    
    
    /**
     * Map: rowLabel -> explicit label numbers for seats in that row.
     * If present, createRowSeats will use these label numbers (left to right)
     * while using NORMAL_SCREEN_SEAT_CONFIG positions for column indices.
     *
     * Example: G -> [2,3,4,7,8] means we will create seats labeled G2,G3,G4,G7,G8
     * but their column indices (for X pos) will be taken from NORMAL_SCREEN_SEAT_CONFIG G positions
     * (which we set to [2,3,4,8,9]) so G7 will be placed at column index 8 (above F8).
     */
    public static final Map<String, List<Integer>> NORMAL_SCREEN_SEAT_LABELS;
    static {
        Map<String, List<Integer>> labels = new HashMap<>();

        NORMAL_SCREEN_SEAT_LABELS = Collections.unmodifiableMap(labels);
    }

    /**
     * VIP screen sofa configuration by row
     * Format: Map<RowLabel, Map<"sofaCount"|"seatsPerSofa", Value>>
     */
    public static final Map<String, Map<String, Object>> VIP_SCREEN_SOFA_CONFIG;
    static {
        Map<String, Map<String, Object>> config = new HashMap<>();
        
        config.put("A", createSofaConfig(3, 2)); // 6 seats
        config.put("B", createSofaConfig(2, 2)); // 4 seats
        config.put("C", createSofaConfig(2, 2)); // 4 seats
        
        VIP_SCREEN_SOFA_CONFIG = Collections.unmodifiableMap(config);
    }

    // Helper methods for creating config maps
    private static Map<String, Object> createRowConfig(int count, SeatType type, List<Integer> positions) {
        Map<String, Object> config = new HashMap<>();
        config.put("count", count);
        config.put("type", type);
        config.put("positions", positions);
        return config;
    }

    private static Map<String, Object> createSofaConfig(int sofaCount, int seatsPerSofa) {
        Map<String, Object> config = new HashMap<>();
        config.put("sofaCount", sofaCount);
        config.put("seatsPerSofa", seatsPerSofa);
        return config;
    }
    
    /**
     * Screen capacity constants for validation and metadata
     */
    public static final int NORMAL_SCREEN_CAPACITY = 51;
    public static final int NORMAL_SCREEN_REGULAR_SEATS = 21;
    public static final int NORMAL_SCREEN_GOLD_SEATS = 18;
    public static final int NORMAL_SCREEN_PREMIUM_SEATS = 12;
    public static final int VIP_SCREEN_CAPACITY = 14;
    public static final int VIP_SCREEN_SOFA_GROUPS = 7;
    public static final int VIP_SCREEN_SOFA_SEATS = 14;

    public enum UserStatus {
        INACTIVE(0, "Inactive"),
        ACTIVE(1, "Active"), 
        DELETED(2, "Deleted");

        private final int code;
        private final String label;

        UserStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public static UserStatus fromCode(int code) {
            for (UserStatus status : UserStatus.values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid user status code: " + code);
        }

        public static boolean isValidStatusCode(int code) {
            for (UserStatus status : UserStatus.values()) {
                if (status.code == code) {
                    return true;
                }
            }
            return false;
        }
    }
    
    

    public enum UserRole {

        ROLE_COUNTER("ROLE_COUNTER"),
        ROLE_ADMIN("ROLE_ADMIN");

        private final String role;

        private UserRole(String role) {
            this.role = role;
        }

        /** full role name (e.g. "ROLE_ADMIN") */
        public String value() {
            return this.role;
        }

        /** short name (e.g. "ADMIN") — useful for hasRole("ADMIN") */
        public String shortName() {
            return this.role.replace(ROLE_PREFIX, "");
        }

        public static boolean isValidRole(String roleValue) {
            if (roleValue == null || roleValue.isEmpty()) return false;
            for (UserRole role : UserRole.values()) {
                if (role.value().equalsIgnoreCase(roleValue.trim())) return true;
            }
            return false;
        }

        public static UserRole fromValue(String roleValue) {
            for (UserRole role : UserRole.values()) {
                if (role.value().equalsIgnoreCase(roleValue.trim())) return role;
            }
            throw new IllegalArgumentException(ApiResponseMessage.INVALID_ROLE + roleValue);
        }
    }
    
    public enum MovieStatus {
        UPCOMING(0, "Upcoming"),
        ACTIVE(1, "Active"),
        INACTIVE(2, "Inactive"),
        ARCHIVED(3, "Archived"),
        DELETED(4, "Deleted");

        private final int code;
        private final String value;

        MovieStatus(int code, String value) {
            this.code = code;
            this.value = value;
        }

        public int getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }

        public static MovieStatus fromCode(int code) {
            for (MovieStatus status : values()) {
                if (status.code == code)
                    return status;
            }
            throw new IllegalArgumentException("Invalid movie status code: " + code);
        }

        public static MovieStatus fromValue(String value) {
            for (MovieStatus status : values()) {
                if (status.value.equalsIgnoreCase(value))
                    return status;
            }
            throw new IllegalArgumentException("Invalid movie status value: " + value);
        }

        public static boolean isValidCode(int code) {
            for (MovieStatus status : values()) {
                if (status.code == code) return true;
            }
            return false;
        }

        public static boolean isValidValue(String value) {
            for (MovieStatus status : values()) {
                if (status.value.equalsIgnoreCase(value)) return true;
            }
            return false;
        }
    }
    
    /**
     * Enum representing the operational status of a Screen (auditorium).
     *
     * <p>We use integer codes for database storage (0/1/2) for compactness and
     * faster indexing, while still keeping type safety at the Java level.</p>
     *
     * <ul>
     *   <li><b>ACTIVE (1)</b> – Screen is enabled and can be used for scheduling shows.</li>
     *   <li><b>INACTIVE (0)</b> – Screen exists but is temporarily disabled (e.g. under maintenance).</li>
     *   <li><b>DELETED (2)</b> – Screen logically deleted; hidden from the POS UI.</li>
     * </ul>
     *
     * <p>Note: We intentionally avoid physical deletion from the DB for audit/history consistency.
     * A status = DELETED means soft delete.</p>
     */
    public enum ScreenStatus {

        INACTIVE(0, "Inactive"),
        ACTIVE(1, "Active"),
        DELETED(2, "Deleted");

        private final int code;
        private final String label;

        ScreenStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /**
         * Returns the integer code to persist in DB.
         */
        public int getCode() {
            return code;
        }

        /**
         * Returns the human-readable label for UI or logs.
         */
        public String getLabel() {
            return label;
        }

        /**
         * Utility method to convert DB int -> enum.
         * Returns INACTIVE if code does not match known values.
         */
        public static ScreenStatus fromCode(int code) {
            for (ScreenStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return INACTIVE;
        }
    }
    
    public enum SeatStatus {

        INACTIVE(0, "Inactive"),
        ACTIVE(1, "Active"),
        DELETED(2, "Deleted");

        private final int code;
        private final String label;

        SeatStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        /**
         * Returns the integer code to persist in DB.
         */
        public int getCode() {
            return code;
        }

        /**
         * Returns the human-readable label for UI or logs.
         */
        public String getLabel() {
            return label;
        }

        /**
         * Utility method to convert DB int -> enum.
         * Returns INACTIVE if code does not match known values.
         */
        public static SeatStatus fromCode(int code) {
            for (SeatStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return INACTIVE;
        }
    }
    
    /**
     * Status of a scheduled Show (screening).
     *
     * Stored as integer codes in DB if you prefer compact storage, but we keep the Java enum
     * for readability and logic. Use ShowStatus.fromCode(...) to convert if you persist ints.
     */
    public enum ShowStatus {
        SCHEDULED(0, "Scheduled"),
        RUNNING(1, "Running"),
        COMPLETED(2, "Completed"),
        CANCELLED(3, "Cancelled");

        private final int code;
        private final String label;

        ShowStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() {
            return code;
        }
        public String getLabel() {
            return label;
        }

        public static ShowStatus fromCode(int code) {
            for (ShowStatus s : values()) {
                if (s.code == code) return s;
            }
            return SCHEDULED;
        }
    }
    
    /**
     * Runtime state of a ShowSeat.
     *
     * Mapped to DB as int (use the getCode() value), but the entity stores the textual state
     * as string for readability; choose whichever mapping you prefer. Below we keep codes and labels.
     */
    public enum ShowSeatState {
        AVAILABLE(0, "AVAILABLE"),
        HELD(1, "HELD"),
        SOLD(2, "SOLD");

        private final int code;
        private final String label;

        ShowSeatState(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() { return code; }
        public String getLabel() { return label; }

        public static ShowSeatState fromCode(int code) {
            for (ShowSeatState s : values()) {
                if (s.code == code) return s;
            }
            return AVAILABLE;
        }

        public static ShowSeatState fromLabel(String label) {
            if (label == null) return AVAILABLE;
            for (ShowSeatState s : values()) {
                if (s.label.equalsIgnoreCase(label)) return s;
            }
            return AVAILABLE;
        }
    }
    
    // Add to your Constants class
    public enum BookingStatus {
        ISSUED(0, "ISSUED"),
        CANCELLED(1, "CANCELLED"), 
        REFUNDED(2, "REFUNDED");

        private final int code;
        private final String label;

        BookingStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() { return code; }
        public String getLabel() { return label; }

        public static BookingStatus fromCode(int code) {
            for (BookingStatus status : values()) {
                if (status.code == code) return status;
            }
            return ISSUED;
        }
    }

    public enum PaymentMode {
        CASH(0, "CASH"),
        POS(1, "POS");

        private final int code;
        private final String label;

        PaymentMode(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() { return code; }
        public String getLabel() { return label; }
    }
    
    public enum ParentalRating {
        G(0, "G", "General Audience - All ages admitted"),
        PG(1, "PG", "Parental Guidance Suggested - Some material may not be suitable for children"),
        PG_13(2, "PG-13", "Parents Strongly Cautioned - Some material may be inappropriate for children under 13"),
        R(3, "R", "Restricted - Under 17 requires accompanying parent or adult guardian"),
        NC_17(4, "NC-17", "Adults Only - No one 17 and under admitted"),
        // Nigerian specific ratings
        U(5, "U", "Universal - Suitable for all"),
        _12A(6, "12A", "Suitable for 12 years and over"),
        _15(7, "15", "Suitable only for 15 years and over"),
        _18(8, "18", "Suitable only for adults");

        private final int code;
        private final String value;
        private final String description;

        ParentalRating(int code, String value, String description) {
            this.code = code;
            this.value = value;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static ParentalRating fromCode(int code) {
            for (ParentalRating rating : values()) {
                if (rating.code == code)
                    return rating;
            }
            throw new IllegalArgumentException("Invalid parental rating code: " + code);
        }

        public static ParentalRating fromValue(String value) {
            for (ParentalRating rating : values()) {
                if (rating.value.equalsIgnoreCase(value))
                    return rating;
            }
            throw new IllegalArgumentException("Invalid parental rating value: " + value);
        }

        public static boolean isValidCode(int code) {
            for (ParentalRating rating : values()) {
                if (rating.code == code) return true;
            }
            return false;
        }

        public static boolean isValidValue(String value) {
            for (ParentalRating rating : values()) {
                if (rating.value.equalsIgnoreCase(value)) return true;
            }
            return false;
        }
    }

    /**
     * Seat Type enum - Defines different seat categories for pricing and styling.
     * 
     * DESIGN DECISION:
     * - NO PRICES in enum - Prices are dynamic per show/movie
     * - Used for seat categorization and frontend styling only
     * - Admin sets actual prices during show scheduling
     */
    public enum SeatType {
        
        // ========== NORMAL SCREEN SEATS ==========
        REGULAR(0, "Regular", "Standard cinema seat"),
        PREMIUM(1, "Premium", "Enhanced comfort seat with better view"),
        GOLD(2, "Gold", "Premium recliner seat with extra legroom"),
        
        // ========== VIP SCREEN SEATS ==========
        VIP_SOFA(3, "VIP Sofa", "Luxury sofa seating with butler service"),
        
        // ========== SPECIAL SEATS ==========
        ACCESSIBLE(4, "Accessible", "Wheelchair accessible seating"),
        COMPANION(5, "Companion", "Companion seat for accessible seating"),
        BALCONY(6, "Balcony", "Premium balcony seating");

        private final int code;
        private final String value;
        private final String description;

        SeatType(int code, String value, String description) {
            this.code = code;
            this.value = value;
            this.description = description;
        }

        // ========== GETTER METHODS ==========
        
        public int getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        // ========== UTILITY METHODS ==========
        
        public static SeatType fromCode(int code) {
            for (SeatType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid seat type code: " + code);
        }

        public static SeatType fromValue(String value) {
            if (value == null) return null;
            for (SeatType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid seat type value: " + value);
        }

        public static boolean isValidCode(int code) {
            for (SeatType type : values()) {
                if (type.code == code) return true;
            }
            return false;
        }

        public static boolean isValidValue(String value) {
            if (value == null) return false;
            for (SeatType type : values()) {
                if (type.value.equalsIgnoreCase(value)) return true;
            }
            return false;
        }

        /**
         * Get seat types for normal screens (excluding VIP)
         */
        public static SeatType[] getNormalScreenTypes() {
            return new SeatType[]{REGULAR, PREMIUM, GOLD, ACCESSIBLE, COMPANION};
        }

        /**
         * Get seat types for VIP screens
         */
        public static SeatType[] getVipScreenTypes() {
            return new SeatType[]{VIP_SOFA, BALCONY};
        }

        /**
         * Check if seat type is for VIP screens
         */
        public static boolean isVipSeatType(SeatType type) {
            return type == VIP_SOFA || type == BALCONY;
        }

        /**
         * Check if seat type requires companion (accessible seats)
         */
        public static boolean requiresCompanion(SeatType type) {
            return type == ACCESSIBLE;
        }

        @Override
        public String toString() {
            return value + " (" + description + ")";
        }
    }
    
    public enum SystemEventType {
        INFO("INFO"),
        WARNING("WARNING"),
        ERROR("ERROR"),
        MAINTENANCE("MAINTENANCE");

        private final String value;

        SystemEventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
    
    public enum SystemEventAction {
        REFRESH_SHOWS("REFRESH_SHOWS"),
        REFRESH_SHOW_STATUS("REFRESH_SHOW_STATUS"),
        REMOVE_SHOW("REMOVE_SHOW");

        private final String value;

        SystemEventAction(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static SystemEventAction fromValue(String value) {
            for (SystemEventAction action : values()) {
                if (action.value.equalsIgnoreCase(value)) {
                    return action;
                }
            }
            throw new IllegalArgumentException("Unknown action: " + value);
        }
    }
    
    // ************** FOOD RELATED CONSTANTS **************
    
    public enum FoodCategoryStatus {

        INACTIVE(0, "Inactive"),
        ACTIVE(1, "Active"),
        DELETED(2, "Deleted");

        private final int code;
        private final String label;

        FoodCategoryStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public static FoodCategoryStatus fromCode(int code) {
            for (FoodCategoryStatus s : values()) {
                if (s.code == code) return s;
            }
            return INACTIVE;
        }
    }
    
    public enum FoodStatus {

        INACTIVE(0, "Inactive"),
        ACTIVE(1, "Active"),
        DELETED(2, "Deleted");

        private final int code;
        private final String label;

        FoodStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public static FoodStatus fromCode(int code) {
            for (FoodStatus s : values()) {
                if (s.code == code) return s;
            }
            return INACTIVE;
        }
    }
    
    public enum FoodOrderStatus {

        CREATED(0, "Created"),
        PAID(1, "Paid"),
        CANCELLED(2, "Cancelled"),
        REFUNDED(3, "Refunded");

        private final int code;
        private final String label;

        FoodOrderStatus(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public static FoodOrderStatus fromCode(int code) {
            for (FoodOrderStatus s : values()) {
                if (s.code == code) return s;
            }
            return CREATED;
        }
    }
        
}
