package com.peter.myhealth;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;

public class PrivacyActivity extends Activity {
  public void onCreate(Bundle b) {
    super.onCreate(b);
    ScrollView scroll = new ScrollView(this);
    TextView t = new TextView(this);
    t.setPadding(32, 48, 32, 32);
    t.setTextSize(16);
    t.setText(
        "My Health — Your privacy\n\n"
            + "Health Connect access is read-only and optional. When you choose Sync, the app"
            + " imports the past 30 days of allowed steps, sleep, exercise and measurements into"
            + " your private local journal. It never uploads your health data.\n\n"
            + "GPS routes stay on this phone. Location access is used only during a workout you"
            + " start.\n\n"
            + "Barcode lookup sends only the product barcode to Open Food Facts over HTTPS. No"
            + " journal entries, weight, location or Health Connect records are sent.\n\n"
            + "There are no ads, analytics or user accounts. Backups and CSV/PDF exports are saved"
            + " only to a location you choose. Exports contain personal data.\n\n"
            + "Revoke permissions in Android settings at any time. Revoking Health Connect access"
            + " stops future imports; imported records can be deleted in Journal. Uninstalling"
            + " removes local records.\n\n"
            + "Biometric locking hides the journal when leaving the app. The app does not store"
            + " fingerprints. When locking is enabled, the step widget hides your count.\n\n"
            + "Daily steps run automatically in the background after physical activity permission"
            + " is granted. Each local calendar day starts fresh at 00:00, with previous totals"
            + " kept in history. Revoke activity access in Android settings to stop collection.\n\n"
            + "The one-time Pro upgrade uses Google Play Billing. Google handles payment methods;"
            + " My Health receives signed purchase information to verify access, acknowledge"
            + " purchases and restore them. Purchase receipts are stored only in private app"
            + " storage.\n\n"
            + "Delete all health data in You removes the journal, profile, steps, routes, food"
            + " cache and reminders. It preserves your Pro purchase and Android permissions."
            + " Exported files and original Health Connect records are not deleted.\n\n"
            + "My Health is a personal wellness journal, not a medical device. It does not"
            + " diagnose, prevent, cure or treat medical conditions. Consult a qualified healthcare"
            + " professional for medical advice."
            + (BuildConfig.SUPPORT_EMAIL.isEmpty()
                ? ""
                : "\n\nPrivacy/support contact: " + BuildConfig.SUPPORT_EMAIL));
    scroll.addView(t);
    android.text.util.Linkify.addLinks(
        t, android.text.util.Linkify.WEB_URLS | android.text.util.Linkify.EMAIL_ADDRESSES);
    t.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    setContentView(scroll);
  }
}
