package com.thesis.irrigation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test for AiPredictionService#calculateIrrigationTime()
 *
 * Covers test design techniques described in the thesis report (Section 4.1.1):
 *   - Equivalence Partitioning (TC01, TC02, TC03)
 *   - Boundary Value Analysis  (TC04, TC05)
 *   - Negative / Error Cases   (TC06, TC07, TC08)
 *
 * This test is fully isolated — no DB or MQTT connection required.
 */
class AiPredictionServiceTest {

    private AiPredictionService aiPredictionService;

    @BeforeEach
    void setUp() {
        // Pure unit — no mocks needed (service has no dependencies)
        aiPredictionService = new AiPredictionService();
    }

    // =========================================================================
    // Equivalence Partitioning — Moisture Zones
    // =========================================================================

    @Test
    @DisplayName("TC01 - Vùng khô (moisture < thresholdMin): phải trả về thời gian tưới > 0")
    void whenMoistureBelowThreshold_shouldReturnPositiveDuration() {
        // Vùng khô: độ ẩm 30%, ngưỡng [50, 80], Kc = 1.0 (cây trung bình)
        long duration = aiPredictionService.calculateIrrigationTime(30.0, 50.0, 80.0, 1.0);

        // Expected: (80 - 30) * 12 * 1.0 = 600 giây
        assertEquals(600L, duration);
    }

    @Test
    @DisplayName("TC02 - Vùng an toàn (moisture >= thresholdMin): phải trả về 0 (không tưới)")
    void whenMoistureInSafeZone_shouldReturnZero() {
        // Vùng an toàn: độ ẩm 65%, ngưỡng [50, 80]
        long duration = aiPredictionService.calculateIrrigationTime(65.0, 50.0, 80.0, 1.0);
        assertEquals(0L, duration);
    }

    @Test
    @DisplayName("TC03 - Vùng úng (moisture > thresholdMax): phải trả về 0 (không tưới)")
    void whenMoistureAboveMax_shouldReturnZero() {
        // Độ ẩm 90%, ngưỡng [50, 80] — đất đang úng
        long duration = aiPredictionService.calculateIrrigationTime(90.0, 50.0, 80.0, 1.0);
        assertEquals(0L, duration);
    }

    // =========================================================================
    // Boundary Value Analysis — Giá trị biên
    // =========================================================================

    @Test
    @DisplayName("TC04 - Biên trên: moisture chính xác bằng thresholdMin → không tưới (=0)")
    void whenMoistureExactlyAtThresholdMin_shouldReturnZero() {
        // Đúng ngưỡng kích hoạt: moisture = 50.0 = thresholdMin → không cần tưới
        long duration = aiPredictionService.calculateIrrigationTime(50.0, 50.0, 80.0, 1.0);
        assertEquals(0L, duration);
    }

    @Test
    @DisplayName("TC05 - Biên dưới: thresholdMin = 0, thresholdMax = 100 → phải tính được thời gian hợp lệ")
    void whenThresholdsAtExtremeBounds_shouldCalculateCorrectly() {
        // moisture = 0%, ngưỡng [0, 100] → không tưới vì 0 >= 0
        long durationAtMin = aiPredictionService.calculateIrrigationTime(0.0, 0.0, 100.0, 1.0);
        assertEquals(0L, durationAtMin);

        // moisture = -0.1 là invalid — nhưng test boundary với moisture = 0, thresholdMin = 1
        long durationBelowMin = aiPredictionService.calculateIrrigationTime(0.0, 1.0, 100.0, 1.0);
        // Expected: (100 - 0) * 12 * 1.0 = 1200 giây
        assertEquals(1200L, durationBelowMin);
    }

    // =========================================================================
    // Kc Factor — Hệ số cây trồng
    // =========================================================================

    @Test
    @DisplayName("TC06 - Kc cao (cây tiêu thụ nhiều nước): thời gian tưới phải lớn hơn Kc thấp")
    void whenKcIsHigher_shouldReturnLongerDuration() {
        // Cà chua (Kc=1.15) vs rau diếp (Kc=0.7), cùng điều kiện độ ẩm
        long durationTomato  = aiPredictionService.calculateIrrigationTime(30.0, 50.0, 80.0, 1.15);
        long durationLettuce = aiPredictionService.calculateIrrigationTime(30.0, 50.0, 80.0, 0.7);

        assertTrue(durationTomato > durationLettuce,
                "Cà chua (Kc cao hơn) phải được tưới lâu hơn rau diếp");
    }

    // =========================================================================
    // Negative Testing — Dữ liệu không hợp lệ
    // =========================================================================

    @Test
    @DisplayName("TC07 - Kc âm hoặc = 0: phải ném IllegalArgumentException")
    void whenKcIsNotPositive_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> aiPredictionService.calculateIrrigationTime(30.0, 50.0, 80.0, 0.0));

        assertThrows(IllegalArgumentException.class,
                () -> aiPredictionService.calculateIrrigationTime(30.0, 50.0, 80.0, -1.0));
    }

    @Test
    @DisplayName("TC08 - thresholdMin >= thresholdMax: phải ném IllegalArgumentException")
    void whenThresholdMinIsGreaterOrEqualToMax_shouldThrowException() {
        // min = max
        assertThrows(IllegalArgumentException.class,
                () -> aiPredictionService.calculateIrrigationTime(30.0, 80.0, 80.0, 1.0));

        // min > max
        assertThrows(IllegalArgumentException.class,
                () -> aiPredictionService.calculateIrrigationTime(30.0, 90.0, 80.0, 1.0));
    }

    @Test
    @DisplayName("TC09 - Ngưỡng ngoài phạm vi [0,100]: phải ném IllegalArgumentException")
    void whenThresholdsOutOfRange_shouldThrowException() {
        // thresholdMin âm
        assertThrows(IllegalArgumentException.class,
                () -> aiPredictionService.calculateIrrigationTime(30.0, -10.0, 80.0, 1.0));

        // thresholdMax > 100
        assertThrows(IllegalArgumentException.class,
                () -> aiPredictionService.calculateIrrigationTime(30.0, 50.0, 110.0, 1.0));
    }
}
