package com.peter.myhealth;

/** Verifies the store's signed receipt; a JSON flag alone can never unlock Pro. */
final class PurchaseVerifier {
  static boolean verify(String publicKey, String receipt, String signature) {
    try {
      if (publicKey == null || publicKey.trim().isEmpty() || receipt == null || signature == null)
        return false;
      java.security.PublicKey key =
          java.security.KeyFactory.getInstance("RSA")
              .generatePublic(
                  new java.security.spec.X509EncodedKeySpec(
                      java.util.Base64.getDecoder().decode(publicKey)));
      java.security.Signature verifier = java.security.Signature.getInstance("SHA1withRSA");
      verifier.initVerify(key);
      verifier.update(receipt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return verifier.verify(java.util.Base64.getDecoder().decode(signature));
    } catch (Exception e) {
      return false;
    }
  }
}
