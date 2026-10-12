package com.peter.myhealth;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import java.time.*;
import java.util.*;
import org.json.*;

final class Reports {
  static String cell(String v) {
    String safe = v == null ? "" : v;
    if (safe.matches("(?s)^[\\s\\uFEFF]*[=+@-].*")
        || safe.startsWith("\t")
        || safe.startsWith("\r")
        || safe.startsWith("\n")) safe = "'" + safe;
    return "\"" + safe.replace("\"", "\"\"") + "\"";
  }

  static String csv(Context c) throws Exception {
    JSONObject d = Store.snapshot(c);
    StringBuilder out =
        new StringBuilder(
            "date,type,label,value,unit,diastolic,distance_km,protein_g,carbs_g,fat_g,notes,source\r\n");
    JSONObject steps = d.getJSONObject("steps");
    List<String> dates = new ArrayList<>();
    for (Iterator<String> i = steps.keys(); i.hasNext(); ) dates.add(i.next());
    Collections.sort(dates);
    for (String date : dates)
      out.append(cell(date))
          .append(",\"steps\",\"Daily steps\",")
          .append(steps.getInt(date))
          .append(",\"steps\",,,,,,,\"daily total\"\r\n");
    JSONArray entries = d.getJSONArray("entries");
    String[] keys = {
      "date",
      "type",
      "label",
      "value",
      "unit",
      "diastolic",
      "distance",
      "protein",
      "carbs",
      "fat",
      "note",
      "source"
    };
    for (int n = 0; n < entries.length(); n++) {
      JSONObject e = entries.getJSONObject(n);
      for (int k = 0; k < keys.length; k++) {
        if (k > 0) out.append(',');
        out.append(cell(e.optString(keys[k])));
      }
      out.append("\r\n");
    }
    return out.toString();
  }

  static byte[] pdf(Context c) throws Exception {
    JSONObject d = Store.snapshot(c);
    List<String> lines = new ArrayList<>();
    LocalDate start = LocalDate.now().minusDays(29);
    long total = 0;
    int tracked = 0, goals = 0;
    double water = 0, sleep = 0;
    int wd = 0, sd = 0;
    for (int i = 0; i < 30; i++) {
      String day = start.plusDays(i).toString();
      int steps = Store.step(c, day);
      total += steps;
      if (steps > 0) tracked++;
      if (steps >= d.getJSONObject("goals").getInt("steps")) goals++;
      double dw = 0, ds = 0;
      for (int n = 0; n < d.getJSONArray("entries").length(); n++) {
        JSONObject e = d.getJSONArray("entries").getJSONObject(n);
        if (!day.equals(e.optString("date"))) continue;
        if (e.optString("type").equals("water")) dw += e.optDouble("value");
        if (e.optString("type").equals("sleep")) ds += e.optDouble("value");
      }
      if (dw > 0) {
        water += dw;
        wd++;
      }
      if (ds > 0) {
        sleep += ds;
        sd++;
      }
    }
    lines.add("My Health | 30-day report");
    lines.add(start + " to " + LocalDate.now());
    lines.add("Prepared for " + d.optString("name"));
    lines.add("");
    lines.add(
        "Steps recorded: "
            + total
            + " | Average on tracked days: "
            + (tracked > 0 ? total / tracked : 0));
    lines.add("Step goals achieved: " + goals + " / 30 days");
    lines.add("Water average on logged days: " + (wd > 0 ? Math.round(water / wd) : 0) + " ml");
    lines.add(
        "Sleep average on logged days: "
            + (sd > 0 ? Math.round(sleep / sd * 10) / 10.0 : 0)
            + " hours");
    lines.add("");
    lines.add("Daily steps");
    for (int i = 0; i < 30; i++) {
      String day = start.plusDays(i).toString();
      lines.add(day + "    " + Store.step(c, day) + " steps");
    }
    lines.add("");
    lines.add("Journal entries in this period");
    JSONArray a = d.getJSONArray("entries");
    for (int i = a.length() - 1; i >= 0; i--) {
      JSONObject e = a.getJSONObject(i);
      if (e.optString("date").compareTo(start.toString()) < 0
          || e.optString("date").compareTo(Store.today()) > 0) continue;
      lines.add(
          e.optString("date")
              + " | "
              + e.optString("type")
              + " | "
              + e.optString("label")
              + " | "
              + e.optString("value")
              + (e.has("diastolic") ? "/" + e.optString("diastolic") : "")
              + " "
              + e.optString("unit"));
      if (!e.optString("note").isEmpty()) lines.add("  " + e.optString("note"));
    }
    lines.add("");
    lines.add("Personal records only. This report does not provide a diagnosis.");
    List<String> wrapped = new ArrayList<>();
    for (String line : lines) {
      String clean = line.replaceAll("[\\r\\n]", " ");
      while (clean.length() > 86) {
        wrapped.add(clean.substring(0, 86));
        clean = clean.substring(86);
      }
      wrapped.add(clean);
    }
    PdfDocument doc = new PdfDocument();
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    int page = 0, index = 0;
    while (index < wrapped.size()) {
      PdfDocument.Page p =
          doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++page).create());
      Canvas canvas = p.getCanvas();
      paint.setColor(Color.rgb(22, 76, 61));
      paint.setTextSize(18);
      canvas.drawText("MY HEALTH", 40, 44, paint);
      paint.setColor(Color.DKGRAY);
      paint.setTextSize(10);
      for (int y = 78; y < 794 && index < wrapped.size(); y += 16)
        canvas.drawText(wrapped.get(index++), 40, y, paint);
      paint.setColor(Color.GRAY);
      canvas.drawText("Private health report · Page " + page, 40, 820, paint);
      doc.finishPage(p);
    }
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    doc.writeTo(out);
    doc.close();
    return out.toByteArray();
  }
}
