package com.peter.myhealth;

import android.app.Activity;
import android.content.*;
import com.android.billingclient.api.*;
import java.util.*;
import org.json.*;

final class ProBilling implements PurchasesUpdatedListener {
  static final String PRODUCT = "my_health_pro";
  private final Activity activity;
  private BillingClient client;
  private ProductDetails product;
  private volatile String status = "Available after the Google Play launch";
  private volatile String price = "";
  private boolean connecting;

  ProBilling(Activity activity) {
    this.activity = activity;
  }

  static android.content.SharedPreferences prefs(Context c) {
    return c.getSharedPreferences("purchase", Context.MODE_PRIVATE);
  }

  static boolean owns(Context c) {
    try {
      String raw = prefs(c).getString("receipt", ""),
          signature = prefs(c).getString("signature", "");
      if (!PurchaseVerifier.verify(BuildConfig.PLAY_BILLING_PUBLIC_KEY, raw, signature))
        return false;
      Purchase p = new Purchase(raw, signature);
      return p.getProducts().contains(PRODUCT)
          && p.getPurchaseState() == Purchase.PurchaseState.PURCHASED;
    } catch (Exception e) {
      return false;
    }
  }

  static boolean access(Context c) {
    return BuildConfig.DEBUG || owns(c) || reviewer(c);
  }

  static boolean reviewer(Context c) {
    return ReviewAccessVerifier.verify(BuildConfig.REVIEW_ACCESS_PUBLIC_KEY,
        prefs(c).getString("reviewAccess", ""));
  }

  static JSONObject state(Context c) throws JSONException {
    return new JSONObject()
        .put("owned", owns(c))
        .put("access", access(c))
        .put("preview", BuildConfig.DEBUG)
        .put("reviewer", reviewer(c))
        .put("configured", !BuildConfig.PLAY_BILLING_PUBLIC_KEY.trim().isEmpty());
  }

  void start() {
    if (BuildConfig.PLAY_BILLING_PUBLIC_KEY.trim().isEmpty()) return;
    if (client == null)
      client =
          BillingClient.newBuilder(activity)
              .setListener(this)
              .enablePendingPurchases(
                  PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
              .enableAutoServiceReconnection()
              .build();
    if (client.isReady()) {
      restore();
      loadProduct();
      return;
    }
    if (connecting) return;
    connecting = true;
    client.startConnection(
        new BillingClientStateListener() {
          public void onBillingSetupFinished(BillingResult result) {
            connecting = false;
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
              restore();
              loadProduct();
            } else status = "Google Play is unavailable. Try again when connected.";
          }

          public void onBillingServiceDisconnected() {
            connecting = false;
            status = "Reconnect to Google Play to purchase or restore.";
          }
        });
  }

  private void loadProduct() {
    QueryProductDetailsParams params =
        QueryProductDetailsParams.newBuilder()
            .setProductList(
                Collections.singletonList(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()))
            .build();
    client.queryProductDetailsAsync(
        params,
        (result, details) -> {
          if (result.getResponseCode() != BillingClient.BillingResponseCode.OK
              || details.getProductDetailsList().isEmpty()) {
            product = null;
            status = "Pro is not available for purchase yet.";
            return;
          }
          product = details.getProductDetailsList().get(0);
          List<ProductDetails.OneTimePurchaseOfferDetails> offers =
              product.getOneTimePurchaseOfferDetailsList();
          if (offers == null || offers.isEmpty()) {
            product = null;
            status = "No purchase offer is available yet.";
            return;
          }
          price = offers.get(0).getFormattedPrice();
          status = "One-time purchase · no subscription";
        });
  }

  void buy() {
    if (BuildConfig.PLAY_BILLING_PUBLIC_KEY.trim().isEmpty()) {
      message("Purchases become available after our Google Play launch. No payment was taken.");
      return;
    }
    if (owns(activity)) {
      message("My Health Pro is already unlocked");
      return;
    }
    if (client == null || !client.isReady() || product == null) {
      start();
      message("Connect to Google Play, then try again when the price appears.");
      return;
    }
    ProductDetails.OneTimePurchaseOfferDetails offer =
        product.getOneTimePurchaseOfferDetailsList().get(0);
    BillingFlowParams.ProductDetailsParams detail =
        BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offer.getOfferToken())
            .build();
    BillingResult result =
        client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(detail))
                .build());
    if (result.getResponseCode() != BillingClient.BillingResponseCode.OK)
      message("Google Play could not start the purchase. No upgrade was applied.");
  }

  void restore() {
    if (client == null || !client.isReady()) {
      start();
      return;
    }
    client.queryPurchasesAsync(
        QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        (result, purchases) -> {
          if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            status = "Could not restore purchases. Try again when connected.";
            return;
          }
          boolean owned = false;
          for (Purchase p : purchases) {
            if (p.getProducts().contains(PRODUCT)
                && p.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                && valid(p)) {
              owned = true;
              handle(p);
            }
          }
          if (!owned) prefs(activity).edit().remove("receipt").remove("signature").apply();
        });
  }

  private boolean valid(Purchase p) {
    try {
      return activity
              .getPackageName()
              .equals(new JSONObject(p.getOriginalJson()).optString("packageName"))
          && PurchaseVerifier.verify(
              BuildConfig.PLAY_BILLING_PUBLIC_KEY, p.getOriginalJson(), p.getSignature());
    } catch (Exception e) {
      return false;
    }
  }

  public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null)
      for (Purchase p : purchases) handle(p);
    else if (result.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED)
      message("Purchase was not completed. You can retry or restore later.");
  }

  private void handle(Purchase p) {
    if (!p.getProducts().contains(PRODUCT)) return;
    if (p.getPurchaseState() == Purchase.PurchaseState.PENDING) {
      status = "Payment pending · Pro unlocks after payment completes";
      return;
    }
    if (p.getPurchaseState() != Purchase.PurchaseState.PURCHASED || !valid(p)) {
      status = "Purchase verification failed. Restore purchases or contact support.";
      return;
    }
    if (p.isAcknowledged()) {
      grant(p);
      return;
    }
    client.acknowledgePurchase(
        AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build(),
        result -> {
          if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) grant(p);
          else status = "Purchase confirmation pending. Restore purchases to retry.";
        });
  }

  private void grant(Purchase p) {
    prefs(activity)
        .edit()
        .putString("receipt", p.getOriginalJson())
        .putString("signature", p.getSignature())
        .apply();
    status = "My Health Pro unlocked";
    message(status);
  }

  private void message(String text) {
    activity.runOnUiThread(
        () ->
            android.widget.Toast.makeText(activity, text, android.widget.Toast.LENGTH_LONG).show());
  }

  JSONObject snapshot() {
    try {
      return state(activity).put("status", status).put("price", price);
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  void close() {
    if (client != null) client.endConnection();
  }
}
