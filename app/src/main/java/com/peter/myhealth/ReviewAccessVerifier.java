package com.peter.myhealth;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Verifies a developer-issued grant; private signing material never ships in the app. */
final class ReviewAccessVerifier {
  static final String PURPOSE = "my-health-review-access:v1:com.peter.myhealth";

  static boolean verify(String publicKey, String code) {
    try {
      if (publicKey == null || code == null || code.length() > 2048 || publicKey.isEmpty())
        return false;
      Signature verifier = Signature.getInstance("SHA256withRSA");
      verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(
          new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))));
      verifier.update(PURPOSE.getBytes(StandardCharsets.UTF_8));
      return verifier.verify(Base64.getUrlDecoder().decode(code.replaceAll("\\s", "")));
    } catch (Exception ignored) {
      return false;
    }
  }
}
