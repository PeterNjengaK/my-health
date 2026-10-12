package com.peter.myhealth;

/** A portrait scanner so food logging never turns the entire app sideways. */
public class FoodScannerActivity extends com.journeyapps.barcodescanner.CaptureActivity {
  protected com.journeyapps.barcodescanner.DecoratedBarcodeView initializeContent() {
    setContentView(R.layout.food_scanner);
    findViewById(R.id.scanner_close).setOnClickListener(v -> finish());
    com.journeyapps.barcodescanner.DecoratedBarcodeView scanner = findViewById(R.id.food_scanner);
    findViewById(R.id.scanner_torch)
        .setOnClickListener(
            new android.view.View.OnClickListener() {
              boolean torch;

              public void onClick(android.view.View v) {
                torch = !torch;
                if (torch) scanner.setTorchOn();
                else scanner.setTorchOff();
                ((android.widget.Button) v).setText(torch ? "Light off" : "Light on");
              }
            });
    return scanner;
  }
}
