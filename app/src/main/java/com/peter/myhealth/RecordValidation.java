package com.peter.myhealth;

import java.time.*;
import java.util.*;
import org.json.*;

final class RecordValidation {
  static String check(JSONObject e) {
    try {
      String type = e.getString("type");
      LocalDate.parse(e.getString("date"));
      if (e.optString("value").isEmpty()
          || e.optString("value").length() > 1000
          || e.optString("label").length() > 200
          || e.optString("note").length() > 4000) return "Check your entry text";
      if (Arrays.asList(
              "water",
              "weight",
              "sleep",
              "workout",
              "food",
              "bloodpressure",
              "heartrate",
              "glucose",
              "temperature",
              "mood")
          .contains(type)) {
        double value = Double.parseDouble(e.getString("value"));
        if (!Double.isFinite(value) || value < 0 || value > 100000)
          return "Enter a valid measurement";
        if (type.equals("sleep") && value > 24) return "Sleep must be 24 hours or less";
        if (type.equals("mood") && (value < 1 || value > 5)) return "Choose a mood from 1 to 5";
      }
      for (String key : Arrays.asList("diastolic", "distance", "protein", "carbs", "fat"))
        if (e.has(key) && !e.optString(key).isEmpty()) {
          double v = Double.parseDouble(e.getString(key));
          if (!Double.isFinite(v) || v < 0 || v > 100000) return "Check " + key;
        }
      if (type.equals("medication")) {
        if (e.optString("label").trim().isEmpty()) return "Enter a medication name";
        LocalTime.parse(e.getString("time"));
      }
      return "";
    } catch (Exception ex) {
      return "Check the date and measurement values";
    }
  }
}
