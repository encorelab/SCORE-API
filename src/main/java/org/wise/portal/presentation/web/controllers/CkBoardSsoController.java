package org.wise.portal.presentation.web.controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.wise.portal.dao.ObjectNotFoundException;
import org.wise.portal.domain.authentication.MutableUserDetails;
import org.wise.portal.domain.user.User;
import org.wise.portal.domain.workgroup.Workgroup;
import org.wise.portal.presentation.util.http.Base64;
import org.wise.portal.service.user.UserService;
import org.wise.portal.service.workgroup.WorkgroupService;

@Controller
public class CkBoardSsoController {
  public static final String STUDENT = "student";
  public static final String TEACHER = "teacher";

  @Autowired
  Environment appProperties;

  @Autowired
  UserService userService;

  @Autowired
  WorkgroupService workgroupService;

  String ckBoardUrl;
  String hashAlgorithm = "HmacSHA256";
  String secretKey;

  @PostConstruct
  public void init() {
    ckBoardUrl = appProperties.getProperty("ck_board_url");
    secretKey = appProperties.getProperty("ck_board_sso_secret_key");
  }

  @GetMapping("/sso/ckboard")
  protected RedirectView ckBoardSsoLogin(@RequestParam("sso") String payload,
      @RequestParam("sig") String sig, @RequestParam("redirectUrl") String redirectUrl,
      Authentication auth) throws IOException, UnsupportedEncodingException {
    String nonce = getNonce(payload);
    if (isCkBoardAvailable() && isValidHash(payload, sig) && nonce != null) {
      User user = userService.retrieveUserByUsername(auth.getName());
      return new RedirectView(generateCkBoardSsoLoginUrl(nonce, user, redirectUrl));
    } else {
      return null;
    }
  }

  private String getNonce(String payloadEncoded) throws IOException, UnsupportedEncodingException {
    String payload = new String(Base64.decode(payloadEncoded), "UTF-8");
    if (payload.startsWith("nonce=")) {
      return payload.substring(6);
    }
    return null;
  }

  private boolean isCkBoardAvailable() {
    return secretKey != null && !secretKey.isEmpty() && ckBoardUrl != null && !ckBoardUrl.isEmpty();
  }

  private boolean isValidHash(String payload, String sig) {
    return hmacDigest(payload, secretKey, hashAlgorithm).equals(sig);
  }

  private String generateCkBoardSsoLoginUrl(String nonce, User user, String redirectUrl)
      throws UnsupportedEncodingException {
    String payload = "";
    if (user.isStudent()) {
      payload = generateStudentPayload(nonce, user, redirectUrl);
    } else if (user.isTeacher()) {
      payload = generateTeacherPayload(nonce, user, redirectUrl);
    }
    String payloadBase64 = Base64.encodeBytes(payload.getBytes());
    String payloadBase64UrlEncoded = URLEncoder.encode(payloadBase64, "UTF-8");
    String payloadBase64Hashed = hmacDigest(payloadBase64, secretKey, hashAlgorithm);
    return generateCkBoardSsoLoginUrlWithPayload(payloadBase64UrlEncoded, payloadBase64Hashed);
  }

  private String generateTeacherPayload(String nonce, User user, String redirectUrl)
      throws UnsupportedEncodingException {
    MutableUserDetails userDetails = user.getUserDetails();
    String username = URLEncoder.encode(userDetails.getUsername(), "UTF-8");
    String email = URLEncoder.encode(userDetails.getEmailAddress(), "UTF-8");
    String redirectUrlWithoutParams = getRedirectUrlWithoutParams(redirectUrl);
    return generatePayload(nonce, username, email, redirectUrlWithoutParams, null, TEACHER);
  }

  private String generateStudentPayload(String nonce, User user, String redirectUrl)
      throws UnsupportedEncodingException {
    Long workgroupId = null;
    String username = null;
    try {
      Long workgroupIdFromRedirectUrl = getWorkgroupIdFromRedirectUrl(redirectUrl);
      Workgroup workgroup = workgroupService.retrieveById(workgroupIdFromRedirectUrl);
      if (workgroup.getMembers().contains(user)) {
        workgroupId = workgroupIdFromRedirectUrl;
        username = workgroup.generateWorkgroupName();
      }
    } catch(ObjectNotFoundException e) {
      e.printStackTrace();
    }
    String redirectUrlWithoutParams = getRedirectUrlWithoutParams(redirectUrl);
    return generatePayload(nonce, username, null, redirectUrlWithoutParams, workgroupId, STUDENT);
  }

  private String getRedirectUrlWithoutParams(String redirectUrl) {
    String redirectUrlWithoutParams = null;
    try {
      URI uri = new URI(redirectUrl);
      redirectUrlWithoutParams = uri.getPath();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return redirectUrlWithoutParams;
  }

  private Long getWorkgroupIdFromRedirectUrl(String redirectUrl) {
    Long workgroupId = null;
    try {
      List<NameValuePair> params =
          URLEncodedUtils.parse(new URI(redirectUrl), Charset.forName("UTF-8"));
      for (NameValuePair param : params) {
        if (param.getName().equals("workgroup-id")) {
          workgroupId = Long.parseLong(param.getValue());
        }
      }
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return workgroupId;
  }

  private String generatePayload(String nonce, String username, String email, String redirectUrl,
      Long workgroupId, String role) {
    StringBuilder payloadBuffer = new StringBuilder();
    payloadBuffer.append("nonce=" + nonce + "&");
    payloadBuffer.append("username=" + username + "&");
    payloadBuffer.append("redirect-url=" + redirectUrl + "&");
    if (email != null) {
      payloadBuffer.append("email=" + email + "&");
    }
    if (workgroupId != null) {
      payloadBuffer.append("workgroup-id=" + workgroupId + "&");
    }
    payloadBuffer.append("role=" + role);
    return payloadBuffer.toString();
  }

  private String generateCkBoardSsoLoginUrlWithPayload(String payload, String hashedPayload) {
    return ckBoardUrl + "/session/sso_login/" + payload + "/" + hashedPayload;
  }

  public static String hmacDigest(String msg, String secretKey, String algorithm) {
    String digest = null;
    try {
      SecretKeySpec key = new SecretKeySpec((secretKey).getBytes("UTF-8"), algorithm);
      Mac mac = Mac.getInstance(algorithm);
      mac.init(key);
      byte[] bytes = mac.doFinal(msg.getBytes("ASCII"));
      StringBuffer hash = new StringBuffer();
      for (int i = 0; i < bytes.length; i++) {
        String hex = Integer.toHexString(0xFF & bytes[i]);
        if (hex.length() == 1) {
          hash.append('0');
        }
        hash.append(hex);
      }
      digest = hash.toString();
    } catch (UnsupportedEncodingException e) {
    } catch (InvalidKeyException e) {
    } catch (NoSuchAlgorithmException e) {
    }
    return digest;
  }
}
