package com.telecamnig.cinemapos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecamnig.cinemapos.dto.SeatPosition;
import com.telecamnig.cinemapos.entity.Screen;
import com.telecamnig.cinemapos.entity.ScreenSeat;
import com.telecamnig.cinemapos.repository.ScreenRepository;
import com.telecamnig.cinemapos.repository.ScreenSeatRepository;
import com.telecamnig.cinemapos.utility.Constants;
import com.telecamnig.cinemapos.utility.Constants.ScreenStatus;
import com.telecamnig.cinemapos.utility.Constants.SeatStatus;
import com.telecamnig.cinemapos.utility.Constants.SeatType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ScreenDataInitializer - CommandLineRunner for pre-populating cinema screens and seats.
 * (Updated fixes: sequential labels for partial rows; deterministic VIP mirroring;
 *  keep B row centered while C uses outer positions)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScreenDataInitializer implements CommandLineRunner {

    private final ScreenRepository screenRepository;
    private final ScreenSeatRepository screenSeatRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        try {
            if (screenRepository.count() == 0) {
                log.info("🚀 Starting cinema screens and seats initialization...");
                
                createNormalScreen1();
                createNormalScreen2();
                createNormalScreen3();
                createNormalScreen4();
                createVipScreen1();
                createVipScreen2();
                
                log.info("✅ Successfully initialized all 6 screens with seats");
            } else {
                log.info("✅ Screens already initialized, skipping data initialization");
            }
        } catch (Exception e) {
            log.error("❌ Critical failure during screen data initialization", e);
        }
    }

    // ========== Normal screens ==========
    private void createNormalScreen1() throws JsonProcessingException {
        Screen screen = createScreen("R1", "Screen 1", "REGULAR", buildNormalScreenLayout("R1"));
        createNormalScreenSeats(screen.getId(), false);
        log.info("🎬 Created Screen R1 (Regular layout)");
    }
    private void createNormalScreen2() throws JsonProcessingException {
        Screen screen = createScreen("R2", "Screen 2", "REGULAR", buildNormalScreenLayout("R2"));
        createNormalScreenSeats(screen.getId(), true);
        log.info("🎬 Created Screen R2 (Mirrored layout)");
    }
    private void createNormalScreen3() throws JsonProcessingException {
        Screen screen = createScreen("R3", "Screen 3", "REGULAR", buildNormalScreenLayout("R3"));
        createNormalScreenSeats(screen.getId(), true);
        log.info("🎬 Created Screen R3 (Mirrored layout)");
    }
    private void createNormalScreen4() throws JsonProcessingException {
        Screen screen = createScreen("R4", "Screen 4", "REGULAR", buildNormalScreenLayout("R4"));
        createNormalScreenSeats(screen.getId(), false);
        log.info("🎬 Created Screen R4 (Regular layout)");
    }

    // ========== VIP screens ==========
    /**
     * IMPORTANT: To match the seat arrangement you described (aisle on the left for VIP1),
     * we flip the mirrored flag here:
     * - VIP1 uses isMirrored = true (aisle left)
     * - VIP2 uses isMirrored = false (aisle right)
     */

    private void createVipScreen1() throws JsonProcessingException {
        boolean isMirrored = true;  // VIP1 -> aisle at left per your arrangement
        VipCanvasInfo info = computeVipCanvasInfo(isMirrored);
        Screen screen = createScreen("VIP1", "VIP Screen 1", "VIP", buildVipScreenLayout(isMirrored, info));
        createVipScreenSeats(screen.getId(), isMirrored, info);
        log.info("🛋️ Created VIP Screen 1 (aisle-left orientation)");
    }

    private void createVipScreen2() throws JsonProcessingException {
        boolean isMirrored = false; // VIP2 -> aisle at right (mirror)
        VipCanvasInfo info = computeVipCanvasInfo(isMirrored);
        Screen screen = createScreen("VIP2", "VIP Screen 2", "VIP", buildVipScreenLayout(isMirrored, info));
        createVipScreenSeats(screen.getId(), isMirrored, info);
        log.info("🛋️ Created VIP Screen 2 (aisle-right orientation)");
    }


    // ========== Screen creation helper ==========
    private Screen createScreen(String code, String name, String category, String layoutJson) {
        Screen screen = Screen.builder()
                .code(code)
                .name(name)
                .category(category)
                .layoutJson(layoutJson)
                .status(ScreenStatus.ACTIVE.getCode())
                .build();
        return screenRepository.save(screen);
    }

    // ========== Normal layout builder ==========
    private String buildNormalScreenLayout(String screenCode) throws JsonProcessingException {
        boolean isMirrored = screenCode.equals("R2") || screenCode.equals("R3");
        
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put(Constants.LAYOUT_KEY_VERSION, Constants.LAYOUT_VERSION);
        layout.put(Constants.LAYOUT_KEY_WIDTH, Constants.CANVAS_WIDTH);
        layout.put(Constants.LAYOUT_KEY_HEIGHT, Constants.CANVAS_HEIGHT);
        layout.put(Constants.LAYOUT_KEY_BACKGROUND, Constants.BACKGROUND_NORMAL);
        
        Map<String, Object> screenPosition = new HashMap<>();
        screenPosition.put(Constants.POSITION_KEY_X, Constants.SCREEN_POSITION_X);
        screenPosition.put(Constants.POSITION_KEY_Y, Constants.SCREEN_POSITION_Y);
        screenPosition.put(Constants.POSITION_KEY_WIDTH, Constants.SCREEN_WIDTH);
        screenPosition.put(Constants.POSITION_KEY_HEIGHT, Constants.SCREEN_HEIGHT);
        layout.put(Constants.LAYOUT_KEY_SCREEN_POSITION, screenPosition);

        // dynamic aisle calculation like before
        List<Map<String, Object>> aisles = new ArrayList<>();
        int minColumnIndex = Integer.MAX_VALUE;
        int maxColumnIndex = Integer.MIN_VALUE;
        for (Map<String, Object> rowCfg : Constants.NORMAL_SCREEN_SEAT_CONFIG.values()) {
            @SuppressWarnings("unchecked")
            List<Integer> cols = (List<Integer>) rowCfg.get("positions");
            if (cols == null) {
                Integer count = (Integer) rowCfg.get("count");
                if (count != null) {
                    for (int c = 1; c <= count; c++) {
                        minColumnIndex = Math.min(minColumnIndex, c);
                        maxColumnIndex = Math.max(maxColumnIndex, c);
                    }
                }
            } else {
                for (Integer c : cols) {
                    if (c != null) {
                        minColumnIndex = Math.min(minColumnIndex, c);
                        maxColumnIndex = Math.max(maxColumnIndex, c);
                    }
                }
            }
        }
        if (minColumnIndex == Integer.MAX_VALUE) minColumnIndex = 1;
        if (maxColumnIndex == Integer.MIN_VALUE) maxColumnIndex = 1;

        int rightmostSeatX = Constants.BASE_X_LEFT + (maxColumnIndex * Constants.SEAT_SPACING);
        int leftmostSeatXMirrored = Constants.BASE_X_RIGHT - (maxColumnIndex * Constants.SEAT_SPACING);
        int aisleMargin = 20;

        int aisleX;
        if (isMirrored) {
            double seatHalf = Constants.SEAT_WIDTH / 2.0;
            aisleX = (int) Math.round(leftmostSeatXMirrored - seatHalf - aisleMargin - Constants.AISLE_WIDTH);
        } else {
            double seatHalf = Constants.SEAT_WIDTH / 2.0;
            aisleX = (int) Math.round(rightmostSeatX + seatHalf + aisleMargin);
        }

     // compute aisle Y with a slight upward shift for regular screens
        int aisleY = Math.max(4, Constants.AISLE_Y_POSITION - Constants.AISLE_VERTICAL_SHIFT_REGULAR);

        Map<String, Object> aisle = new HashMap<>();
        aisle.put(Constants.AISLE_KEY_ID, isMirrored ? "left_aisle" : "right_aisle");
        aisle.put(Constants.POSITION_KEY_X, aisleX);
        aisle.put(Constants.POSITION_KEY_Y, aisleY);
        aisle.put(Constants.POSITION_KEY_WIDTH, Constants.AISLE_WIDTH);
        aisle.put(Constants.POSITION_KEY_HEIGHT, Constants.AISLE_HEIGHT_NORMAL);
        aisle.put(Constants.AISLE_KEY_LABEL, "Aisle");
        aisle.put(Constants.AISLE_KEY_COLOR, Constants.AISLE_COLOR_NORMAL);
        aisles.add(aisle);

        layout.put(Constants.LAYOUT_KEY_AISLES, aisles);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String rowLabel : Constants.NORMAL_SCREEN_ROW_POSITIONS.keySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put(Constants.ROW_KEY_ID, "row" + rowLabel);
            row.put(Constants.ROW_KEY_LABEL, rowLabel);
            row.put(Constants.POSITION_KEY_Y, Constants.NORMAL_SCREEN_ROW_POSITIONS.get(rowLabel));
            row.put(Constants.POSITION_KEY_HEIGHT, Constants.SEAT_HEIGHT);
            rows.add(row);
        }
        layout.put(Constants.LAYOUT_KEY_ROWS, rows);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(Constants.META_KEY_TOTAL_SEATS, Constants.NORMAL_SCREEN_CAPACITY);
        metadata.put(Constants.META_KEY_CAPACITY, Constants.NORMAL_SCREEN_CAPACITY);
        metadata.put(Constants.META_KEY_REGULAR_SEATS, Constants.NORMAL_SCREEN_REGULAR_SEATS);
        metadata.put(Constants.META_KEY_GOLD_SEATS, Constants.NORMAL_SCREEN_GOLD_SEATS);
        metadata.put(Constants.META_KEY_PREMIUM_SEATS, Constants.NORMAL_SCREEN_PREMIUM_SEATS);
        layout.put(Constants.LAYOUT_KEY_METADATA, metadata);

        return objectMapper.writeValueAsString(layout);
    }

    /**
     * Build VIP layout JSON using precomputed VipCanvasInfo and explicit isMirrored flag.
     */
    private String buildVipScreenLayout(boolean isMirrored, VipCanvasInfo info) throws JsonProcessingException {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put(Constants.LAYOUT_KEY_VERSION, Constants.LAYOUT_VERSION);
        layout.put(Constants.LAYOUT_KEY_WIDTH, info.width);
        layout.put(Constants.LAYOUT_KEY_HEIGHT, info.height);
        layout.put(Constants.LAYOUT_KEY_BACKGROUND, Constants.BACKGROUND_VIP);

        // Screen position: center the screen bar horizontally inside the VIP canvas
        Map<String, Object> screenPosition = new HashMap<>();
        screenPosition.put(Constants.POSITION_KEY_X, (info.width - Constants.SCREEN_WIDTH) / 2);
        screenPosition.put(Constants.POSITION_KEY_Y, Constants.SCREEN_POSITION_Y);
        screenPosition.put(Constants.POSITION_KEY_WIDTH, Constants.SCREEN_WIDTH);
        screenPosition.put(Constants.POSITION_KEY_HEIGHT, Constants.SCREEN_HEIGHT);
        layout.put(Constants.LAYOUT_KEY_SCREEN_POSITION, screenPosition);

        // shifted bounds used to place aisle
        int shiftedMinX = info.minX + info.leftOffset;
        int shiftedMaxX = info.maxX + info.leftOffset;
        int shiftedMinY = info.minY + info.topOffset;
        int shiftedMaxY = info.maxY + info.topOffset;

        int gap = 12;
        int aisleX;
        if (isMirrored) {
            // left aisle: just left of the leftmost seat
            aisleX = Math.max(8, shiftedMinX - Constants.AISLE_WIDTH - gap);
        } else {
            // right aisle: just right of the rightmost seat (but keep within canvas)
            aisleX = Math.min(info.width - Constants.AISLE_WIDTH - 8, shiftedMaxX + gap);
        }

        // vertical placement
        int verticalPadBottom = 16;
//        int seatBlockHeight = shiftedMaxY - shiftedMinY;
//        int aisleHeight = Math.min(Constants.AISLE_HEIGHT_VIP, Math.max((int)Math.round(seatBlockHeight * 0.85), 120));
        int seatBlockHeight = shiftedMaxY - shiftedMinY;
        int dynamicHeight = Math.max((int)Math.round(seatBlockHeight * 0.85), 120);
        int aisleHeight = Math.max(Constants.AISLE_HEIGHT_VIP, dynamicHeight);


     // place aisle top slightly above the topmost seat row — controlled by constant
        int aisleY = Math.max(4, shiftedMinY - Constants.AISLE_VERTICAL_OFFSET_VIP);

        // keep previous safety: don't overflow the canvas bottom
        if (aisleY + aisleHeight > info.height - verticalPadBottom) {
            aisleY = Math.max(4, info.height - verticalPadBottom - aisleHeight);
        }


        List<Map<String, Object>> aisles = new ArrayList<>();
        Map<String, Object> aisle = new HashMap<>();
        aisle.put(Constants.AISLE_KEY_ID, isMirrored ? "left_aisle" : "right_aisle");
        aisle.put(Constants.POSITION_KEY_X, aisleX);
        aisle.put(Constants.POSITION_KEY_Y, aisleY);
        aisle.put(Constants.POSITION_KEY_WIDTH, Constants.AISLE_WIDTH);
        aisle.put(Constants.POSITION_KEY_HEIGHT, aisleHeight);
        aisle.put(Constants.AISLE_KEY_LABEL, "Aisle");
        aisle.put(Constants.AISLE_KEY_COLOR, Constants.AISLE_COLOR_VIP);
        aisles.add(aisle);
        layout.put(Constants.LAYOUT_KEY_AISLES, aisles);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(Constants.META_KEY_TOTAL_SEATS, Constants.VIP_SCREEN_CAPACITY);
        metadata.put(Constants.META_KEY_CAPACITY, Constants.VIP_SCREEN_CAPACITY);
        metadata.put(Constants.META_KEY_SOFA_GROUPS, Constants.VIP_SCREEN_SOFA_GROUPS);
        metadata.put(Constants.META_KEY_SOFA_SEATS, Constants.VIP_SCREEN_SOFA_SEATS);
        layout.put(Constants.LAYOUT_KEY_METADATA, metadata);

        // debugging offsets (so frontend and DB match)
        layout.put("leftOffset", info.leftOffset);
        layout.put("topOffset", info.topOffset);

        return objectMapper.writeValueAsString(layout);
    }


    // ========== Seats creation ==========
    private void createNormalScreenSeats(Long screenId, boolean isMirrored) throws JsonProcessingException {
        List<ScreenSeat> seats = new ArrayList<>();
        for (String rowLabel : Constants.NORMAL_SCREEN_SEAT_CONFIG.keySet()) {
            Map<String, Object> rowConfig = Constants.NORMAL_SCREEN_SEAT_CONFIG.get(rowLabel);
            int seatCount = (int) rowConfig.get("count");
            SeatType seatType = (SeatType) rowConfig.get("type");
            @SuppressWarnings("unchecked")
            List<Integer> positions = (List<Integer>) rowConfig.get("positions");
            createRowSeats(seats, screenId, rowLabel, seatCount, isMirrored, seatType, positions);
        }
        screenSeatRepository.saveAll(seats);
        log.debug("✅ Created {} seats for screen ID: {}", seats.size(), screenId);
    }

    /**
     * VIP seats creator which reuses the precomputed VipCanvasInfo so layout + seats match.
     *
     * Key fix: keep B row centered (sofaCount==2) while allowing C (also sofaCount==2) to use outer positions.
     */
    private void createVipScreenSeats(Long screenId, boolean isMirrored, VipCanvasInfo info) throws JsonProcessingException {
        List<ScreenSeat> seats = new ArrayList<>();
        int leftOffset = info.leftOffset;
        int topOffset  = info.topOffset;

        for (String rowLabel : Constants.VIP_SCREEN_SOFA_CONFIG.keySet()) {
            Map<String, Object> rowConfig = Constants.VIP_SCREEN_SOFA_CONFIG.get(rowLabel);
            int sofaCount = (int) rowConfig.get("sofaCount");

            for (int sofaNumber = 1; sofaNumber <= sofaCount; sofaNumber++) {
                String groupPublicId = UUID.randomUUID().toString();

                // compute multiplier so even-count rows (like sofaCount==2) map to outer positions
                double center = (sofaCount + 1) / 2.0;
                double multiplier = (sofaNumber - center);

                // IMPORTANT FIX:
                // if sofaCount is even, push them to outer positions by doubling multiplier,
                // but keep row B as centered so its two sofas sit between A's gaps
                if (sofaCount % 2 == 0 && !"B".equals(rowLabel)) {
                    multiplier *= 2.0;
                }

                int sofaOffset = (int) Math.round(multiplier * Constants.SOFA_SPACING);

                int centerBase = (Constants.BASE_X_VIP_LEFT + Constants.BASE_X_VIP_RIGHT) / 2;
                int sofaX = isMirrored ? (centerBase - sofaOffset) : (centerBase + sofaOffset);

                int sofaXShifted = sofaX + leftOffset;
                int rowY = Constants.VIP_SCREEN_ROW_POSITIONS.get(rowLabel) + topOffset;

                for (int seatInSofa = 1; seatInSofa <= 2; seatInSofa++) {
                    String label = rowLabel + ((sofaNumber - 1) * 2 + seatInSofa);
                    int seatX;
                    if (isMirrored) {
                        // Mirrored: seat 1 on right side of sofa center, seat 2 on left
                        seatX = sofaXShifted + (seatInSofa == 1 ? Constants.SOFA_SEAT_OFFSET : -Constants.SOFA_SEAT_OFFSET);
                    } else {
                        seatX = sofaXShifted + (seatInSofa == 1 ? -Constants.SOFA_SEAT_OFFSET : Constants.SOFA_SEAT_OFFSET);
                    }

                    SeatPosition position = new SeatPosition(seatX, rowY, Constants.SOFA_SEAT_WIDTH, Constants.SOFA_SEAT_HEIGHT);
                    ScreenSeat seat = buildScreenSeat(screenId, label, rowLabel.charAt(0) - 'A',
                                                     seatInSofa, SeatType.VIP_SOFA, groupPublicId, position);
                    seats.add(seat);
                }
            }
        }

        screenSeatRepository.saveAll(seats);
        log.debug("Created {} VIP seats for screen {}", seats.size(), screenId);
    }

    // ========== createRowSeats (fixed labeling logic) ==========
 // ========== createRowSeats (with A1/A2 half-step offset) ==========
    private void createRowSeats(List<ScreenSeat> seats, Long screenId, String rowLabel, int seatCount,
            boolean isMirrored, SeatType seatType, List<Integer> seatPositions) throws JsonProcessingException {

        int baseX = isMirrored ? Constants.BASE_X_RIGHT : Constants.BASE_X_LEFT;
        int rowY = Constants.NORMAL_SCREEN_ROW_POSITIONS.get(rowLabel);

        List<Integer> columnIndices = (seatPositions != null) ? seatPositions : range(1, seatCount + 1);

        // Use explicit label numbers if provided AND size matches; otherwise sequential when positions provided
        List<Integer> explicitLabels = Constants.NORMAL_SCREEN_SEAT_LABELS.get(rowLabel);
        boolean useExplicitLabels = explicitLabels != null && explicitLabels.size() == columnIndices.size();

        for (int i = 0; i < columnIndices.size(); i++) {
            int columnIndex = columnIndices.get(i);

            final int labelNumber;
            if (useExplicitLabels) {
                labelNumber = explicitLabels.get(i);
            } else if (seatPositions != null) {
                // sequential labeling for partial rows (A -> A1,A2,A3)
                labelNumber = i + 1;
            } else {
                labelNumber = columnIndex;
            }

            // base X from column index
            int x = isMirrored
                    ? (baseX - (columnIndex * Constants.SEAT_SPACING))
                    : (baseX + (columnIndex * Constants.SEAT_SPACING));

            // --- SPECIAL CASE: Row A alignment ---
            // A1 should sit between B1 & B2; A2 between B2 & B3.
            // We push A1 and A2 by half a seat spacing toward the next column.
            if ("A".equals(rowLabel) && (labelNumber == 1 || labelNumber == 2)) {
                int half = Constants.SEAT_SPACING / 2;
                // for mirrored layouts, "next column" is to the left; otherwise to the right
                x += isMirrored ? -half : half;
            }
            // --- END SPECIAL CASE ---

            String label = rowLabel + labelNumber;
            SeatPosition position = new SeatPosition(x, rowY, Constants.SEAT_WIDTH, Constants.SEAT_HEIGHT);

            ScreenSeat seat = buildScreenSeat(
                    screenId, label, rowLabel.charAt(0) - 'A',
                    labelNumber, seatType, null, position
            );
            seats.add(seat);
        }
    }


    // ========== Build seat entity ==========
    private ScreenSeat buildScreenSeat(Long screenId, String label, int rowIndex, int colIndex,
                                     SeatType seatType, String groupPublicId, SeatPosition position) throws JsonProcessingException {
        
        Map<String, Object> metaJson = new HashMap<>();
        metaJson.put(Constants.META_KEY_X, position.getX());
        metaJson.put(Constants.META_KEY_Y, position.getY());
        metaJson.put(Constants.META_KEY_WIDTH, position.getWidth());
        metaJson.put(Constants.META_KEY_HEIGHT, position.getHeight());
        metaJson.put(Constants.META_KEY_TYPE, seatType.getValue());
        
        if (seatType == SeatType.VIP_SOFA) {
            metaJson.put(Constants.META_KEY_ICON, Constants.META_ICON_SOFA);
        }

        return ScreenSeat.builder()
                .screenId(screenId)
                .label(label)
                .rowIndex(rowIndex)
                .colIndex(colIndex)
                .seatType(seatType.getValue())
                .groupPublicId(groupPublicId)
                .metaJson(objectMapper.writeValueAsString(metaJson))
                .status(SeatStatus.ACTIVE.getCode())
                .build();
    }

    // ========== Utility ==========
    private List<Integer> range(int start, int end) {
        List<Integer> list = new ArrayList<>();
        for (int i = start; i < end; i++) list.add(i);
        return list;
    }

    // ========== VIP canvas computation (updated to match seat placement rule) ==========
    private static class VipCanvasInfo {
        int width;
        int height;
        int leftOffset;
        int topOffset;
        int minX;
        int maxX;
        int minY;
        int maxY;
    }

    private VipCanvasInfo computeVipCanvasInfo(boolean isMirrored) {
        List<Integer> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();

        for (String rowLabel : Constants.VIP_SCREEN_SOFA_CONFIG.keySet()) {
            Map<String, Object> r = Constants.VIP_SCREEN_SOFA_CONFIG.get(rowLabel);
            int sofaCount = (int) r.get("sofaCount");
            int rowY = Constants.VIP_SCREEN_ROW_POSITIONS.get(rowLabel);
            ys.add(rowY);

            for (int sofaNumber = 1; sofaNumber <= sofaCount; sofaNumber++) {
                double center = (sofaCount + 1) / 2.0;
                double multiplier = (sofaNumber - center);

                // Apply same rule as when creating seats:
                // double multiplier for even-count rows except row B (keep B centered)
                if (sofaCount % 2 == 0 && !"B".equals(rowLabel)) {
                    multiplier *= 2.0;
                }

                int sofaOffset = (int) Math.round(multiplier * Constants.SOFA_SPACING);

                int centerBase = (Constants.BASE_X_VIP_LEFT + Constants.BASE_X_VIP_RIGHT) / 2;
                int sofaX = isMirrored ? (centerBase - sofaOffset) : (centerBase + sofaOffset);

                int leftSeatX  = sofaX - Constants.SOFA_SEAT_OFFSET - (Constants.SOFA_SEAT_WIDTH / 2);
                int rightSeatX = sofaX + Constants.SOFA_SEAT_OFFSET + (Constants.SOFA_SEAT_WIDTH / 2);
                xs.add(leftSeatX);
                xs.add(rightSeatX);
            }
        }

        int minX = xs.stream().min(Integer::compareTo).orElse(Constants.BASE_X_VIP_LEFT);
        int maxX = xs.stream().max(Integer::compareTo).orElse(Constants.BASE_X_VIP_RIGHT);
        int minY = ys.stream().min(Integer::compareTo).orElse(250);
        int maxY = ys.stream().max(Integer::compareTo).orElse(500);

        int horizontalMargin = 84;
        int verticalMarginTop = 80;
        int verticalMarginBottom = 80;
        int extraWidthForAisle = Constants.AISLE_WIDTH + 24;

        VipCanvasInfo info = new VipCanvasInfo();
        info.minX = minX;
        info.maxX = maxX;
        info.minY = minY;
        info.maxY = maxY;

        info.width  = Math.max((maxX - minX) + horizontalMargin * 2 + extraWidthForAisle, 640);
        info.height = Math.max((maxY - minY) + verticalMarginTop + verticalMarginBottom, 480);

        info.leftOffset = horizontalMargin - minX;
        info.topOffset  = verticalMarginTop - minY;

        log.info("VIP canvas computed: minX={}, maxX={}, minY={}, maxY={}, width={}, height={}, leftOffset={}, topOffset={}",
                 minX, maxX, minY, maxY, info.width, info.height, info.leftOffset, info.topOffset);

        return info;
    }

}
