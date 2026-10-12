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
    System.out.println("Passed 7 step-counter regression checks");
  }
}
