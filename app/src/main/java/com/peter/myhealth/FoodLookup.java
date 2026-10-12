package com.peter.myhealth;

import android.content.Context;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

final class FoodLookup {
  static JSONObject lookup(Context c, String barcode) throws Exception {
    if (!barcode.matches("[0-9]{8,14}"))
      throw new IllegalArgumentException("Enter an 8–14 digit product barcode");
    String key = "food:" + barcode;
    String cached = Store.prefs(c).getString(key, null);
    if (cached != null) return new JSONObject(cached).put("cached", true);
    HttpURLConnection connection =
        (HttpURLConnection)
            new URL(
                    "https://world.openfoodfacts.org/api/v2/product/"
                        + barcode
                        + ".json?fields=product_name,brands,nutriments")
                .openConnection();
    connection.setConnectTimeout(12000);
    connection.setReadTimeout(12000);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestProperty(
        "User-Agent", "MyHealth/2.0 (https://github.com/PeterNjengaK/my-health)");
    try {
      if (connection.getResponseCode() != 200)
        throw new Exception("Product lookup unavailable. You can enter nutrition manually.");
      java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
      try (java.io.InputStream in = connection.getInputStream()) {
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) != -1) {
          b.write(chunk, 0, n);
          if (b.size() > 1000000) throw new Exception("Product response too large");
        }
      }
      JSONObject response = new JSONObject(b.toString(StandardCharsets.UTF_8.name()));
      if (response.optInt("status") != 1)
        throw new Exception("Product not found. Enter the nutrition from its label.");
      JSONObject p = response.getJSONObject("product"), n = p.optJSONObject("nutriments");
      JSONObject out =
          new JSONObject()
              .put("barcode", barcode)
              .put("name", p.optString("product_name", "Packaged food"))
              .put("brand", p.optString("brands"))
              .put("basis", "per 100 g / 100 ml");
      double calories =
          n == null
              ? Double.NaN
              : n.optDouble("energy-kcal_100g", n.optDouble("energy-kj_100g", Double.NaN) / 4.184);
      if (!Double.isFinite(calories) || calories < 0)
        throw new Exception("No calorie information for this product. Enter it from the label.");
      out.put("calories", calories);
      String[][] macros = {
        {"protein", "proteins_100g"}, {"carbs", "carbohydrates_100g"}, {"fat", "fat_100g"}
      };
      for (String[] macro : macros) {
        double value = n.optDouble(macro[1], Double.NaN);
        out.put(macro[0], Double.isFinite(value) && value >= 0 ? value : JSONObject.NULL);
      }
      Store.prefs(c).edit().putString(key, out.toString()).apply();
      return out;
    } finally {
      connection.disconnect();
    }
  }
}
