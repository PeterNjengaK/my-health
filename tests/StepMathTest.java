package com.peter.myhealth;

import java.time.*;

public class StepMathTest {
  static void equal(Object wanted, Object actual) {
    if (!wanted.equals(actual)) throw new AssertionError("Expected " + wanted + ", got " + actual);
  }

  public static void main(String[] args) {
    equal(0L, StepMath.delta(-1, 12000, true));
    equal(17L, StepMath.delta(12000, 12017, true));
    equal(0L, StepMath.delta(12017, 12017, true));
    equal(0L, StepMath.delta(12017, 12000, true));
    equal(25L, StepMath.delta(12017, 25, false));
    ZoneId zone = ZoneId.of("America/Los_Angeles");
    long midnight = LocalDate.of(2026, 10, 12).atStartOfDay(zone).toInstant().toEpochMilli();
    equal("2026-10-11", StepMath.eventDay(midnight + 5000, 20_000_000_000L, 10_000_000_000L, zone));
    equal("2026-10-12", StepMath.eventDay(midnight + 5000, 20_000_000_000L, 20_000_000_000L, zone));
    equal(midnight, StepMath.nextMidnight(midnight - 1, zone));
    equal(
        LocalDate.of(2026, 10, 13).atStartOfDay(zone).toInstant().toEpochMilli(),
        StepMath.nextMidnight(midnight, zone));
    long spring = LocalDate.of(2026, 3, 8).atStartOfDay(zone).toInstant().toEpochMilli();
    long fall = LocalDate.of(2026, 11, 1).atStartOfDay(zone).toInstant().toEpochMilli();
    equal(23 * 3600000L, StepMath.nextMidnight(spring, zone) - spring);
    equal(25 * 3600000L, StepMath.nextMidnight(fall, zone) - fall);
    java.util.Map<String, Long> split =
        StepMath.splitDailySteps(
            3,
            java.util.List.of(10_000_000_000L, 19_000_000_000L, 20_000_000_000L),
            20_000_000_000L,
            midnight + 5000,
            20_000_000_000L,
            zone);
    equal(1L, split.get("2026-10-11"));
    equal(2L, split.get("2026-10-12"));
    equal(
        3L,
        StepMath.splitDailySteps(
                3, java.util.List.of(), 20_000_000_000L, midnight + 5000, 20_000_000_000L, zone)
            .get("2026-10-12"));
    equal(
        0,
        StepMath.splitDailySteps(
                0,
                java.util.List.of(10_000_000_000L),
                20_000_000_000L,
                midnight + 5000,
                20_000_000_000L,
                zone)
            .size());
    equal(
        1L,
        StepMath.splitDailySteps(
                1,
                java.util.List.of(10_000_000_000L, 19_000_000_000L),
                20_000_000_000L,
                midnight + 5000,
                20_000_000_000L,
                zone)
            .get("2026-10-11"));
    System.out.println("Passed 16 daily step, midnight and DST regression checks");
  }
}
