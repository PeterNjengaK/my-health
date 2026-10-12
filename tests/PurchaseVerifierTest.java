package com.peter.myhealth;

public class PurchaseVerifierTest {
  static void check(boolean condition) {
    if (!condition) throw new AssertionError("Receipt signature regression");
  }

  public static void main(String[] args) throws Exception {
    java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    java.security.KeyPair pair = generator.generateKeyPair();
    String key = java.util.Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    String receipt = "{\"productId\":\"my_health_pro\",\"purchaseState\":0}";
    java.security.Signature signer = java.security.Signature.getInstance("SHA1withRSA");
    signer.initSign(pair.getPrivate());
    signer.update(receipt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String signature = java.util.Base64.getEncoder().encodeToString(signer.sign());
    check(PurchaseVerifier.verify(key, receipt, signature));
    check(!PurchaseVerifier.verify(key, receipt + " ", signature));
    check(!PurchaseVerifier.verify("", receipt, signature));
    check(!PurchaseVerifier.verify(key, receipt, "invalid"));
    check(
        !PurchaseVerifier.verify(
            java.util.Base64.getEncoder()
                .encodeToString(generator.generateKeyPair().getPublic().getEncoded()),
            receipt,
            signature));
    System.out.println("Passed 5 receipt signature, tampering and missing-key checks");
  }
}
