package com.peter.myhealth;

final class StepMath {
  static long delta(long previous, long current, boolean sameBoot) {
    if (previous < 0) return 0; // First enable establishes a baseline.
    if (!sameBoot) return Math.max(0, current); // Sensor resets on reboot.
    return Math.max(0, current - previous); // Duplicate / backwards events add nothing.
  }

  static String eventDay(
      long wallNow, long elapsedNowNanos, long eventNanos, java.time.ZoneId zone) {
    long wallEvent = wallNow - (elapsedNowNanos - eventNanos) / 1_000_000L;
    return java.time.Instant.ofEpochMilli(wallEvent).atZone(zone).toLocalDate().toString();
  }
}
