package com.peter.myhealth;

public class ReviewAccessVerifierTest {
  static void check(boolean ok) { if (!ok) throw new AssertionError("Review grant verification failed"); }
  static String sign(java.security.KeyPair pair, String purpose) throws Exception {
    java.security.Signature signer = java.security.Signature.getInstance("SHA256withRSA");
    signer.initSign(pair.getPrivate());
    signer.update(purpose.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
  }
  public static void main(String[] args) throws Exception {
    java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    java.security.KeyPair pair = generator.generateKeyPair();
    String key = java.util.Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    String grant = sign(pair, ReviewAccessVerifier.PURPOSE);
    check(ReviewAccessVerifier.verify(key, grant));
    check(ReviewAccessVerifier.verify(key, " \n" + grant + "\n"));
    check(!ReviewAccessVerifier.verify(key, sign(pair, "another-app")));
    check(!ReviewAccessVerifier.verify(key, sign(generator.generateKeyPair(), ReviewAccessVerifier.PURPOSE)));
    check(!ReviewAccessVerifier.verify(key, "invalid"));
    check(!ReviewAccessVerifier.verify("", grant));
    check(!ReviewAccessVerifier.verify(key, grant.substring(1)));
    check(!ReviewAccessVerifier.verify(key, "x".repeat(2049)));
    System.out.println("Passed 8 review-access signature, purpose, tampering and missing-key checks");
  }
}
