package com.peter.myhealth;

final class StepMath {
  static long nextMidnight(long wallNow, java.time.ZoneId zone) {
    return java.time.Instant.ofEpochMilli(wallNow)
        .atZone(zone)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli();
  }

  static java.util.Map<String, Long> splitDailySteps(
      long delta,
      java.util.List<Long> detected,
      long counterTimestamp,
      long wallNow,
      long elapsedNow,
      java.time.ZoneId zone) {
    java.util.Map<String, Long> totals = new java.util.LinkedHashMap<>();
    long used = 0;
    for (long timestamp : detected) {
      if (timestamp > counterTimestamp || used >= delta) continue;
      String day = eventDay(wallNow, elapsedNow, timestamp, zone);
      totals.put(day, totals.getOrDefault(day, 0L) + 1);
      used++;
    }
    if (delta > used) {
      String day = eventDay(wallNow, elapsedNow, counterTimestamp, zone);
      totals.put(day, totals.getOrDefault(day, 0L) + delta - used);
    }
    return totals;
  }

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
