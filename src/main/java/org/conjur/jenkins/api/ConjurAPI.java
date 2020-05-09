package org.conjur.jenkins.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
//import org.conjur.jenkins.configuration.ConjurConfiguration;
//import org.conjur.jenkins.configuration.ConjurJITJobProperty;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import com.fasterxml.jackson.databind.ObjectMapper;

import hudson.model.Run;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConjurAPI {

	public static class ConjurAuthnInfo {
		String applianceUrl;
		String authnPath;
		String account;
		String login;
		String apiKey;

        public ConjurAuthnInfo() {
            }

        public ConjurAuthnInfo(String applianceUrl, String authnPath, String account, String login, String apiKey) {
            this.applianceUrl = applianceUrl;
            this.authnPath = authnPath;
            this.account = account;
            this.login = login;
            this.apiKey = apiKey;
        }
                
	}

	private static final Logger LOGGER = Logger.getLogger(ConjurAPI.class.getName());

	private static void defaultToEnvironment(ConjurAuthnInfo conjurAuthn) {
		Map<String, String> env = System.getenv();
		if (conjurAuthn.applianceUrl == null && env.containsKey("CONJUR_APPLIANCE_URL"))
			conjurAuthn.applianceUrl = env.get("CONJUR_APPLIANCE_URL");
		if (conjurAuthn.account == null && env.containsKey("CONJUR_ACCOUNT"))
			conjurAuthn.account = env.get("CONJUR_ACCOUNT");
		if (conjurAuthn.login == null && env.containsKey("CONJUR_AUTHN_LOGIN"))
			conjurAuthn.login = env.get("CONJUR_AUTHN_LOGIN");
		if (conjurAuthn.apiKey == null && env.containsKey("CONJUR_AUTHN_API_KEY"))
			conjurAuthn.apiKey = env.get("CONJUR_AUTHN_API_KEY");
	}

	public static String getAuthorizationToken(OkHttpClient client, ConjurAuthnInfo conjurAuthn, 
			Run<?, ?> context) throws IOException {

		String resultingToken = null;

		if (conjurAuthn.login != null && conjurAuthn.apiKey != null) {
			LOGGER.log(Level.INFO, "Authenticating with Conjur");
			Request request = new Request.Builder()
					.url(String.format("%s/%s/%s/%s/authenticate", conjurAuthn.applianceUrl, conjurAuthn.authnPath,
							conjurAuthn.account, URLEncoder.encode(conjurAuthn.login, "utf-8")))
					.post(RequestBody.create(MediaType.parse("text/plain"), conjurAuthn.apiKey)).build();

			Response response = client.newCall(request).execute();
			resultingToken = Base64.getEncoder().withoutPadding()
					.encodeToString(response.body().string().getBytes("UTF-8"));
			LOGGER.log(Level.INFO,
					() -> "Conjur Authenticate response " + response.code() + " - " + response.message());
			if (response.code() != 200) {
				throw new IOException("Error authenticating to Conjur [" + response.code() + " - " + response.message()
						+ "\n" + resultingToken);
			}
		} else {
			LOGGER.log(Level.INFO, "Failed to find credentials for conjur authentication");
		}

		return resultingToken;
	}


	private static String signatureForRequest(String challenge, RSAPrivateKey privateKey) {
		// sign using the private key
		LOGGER.log(Level.INFO, "Challenge: {0}", challenge);
		try {
			Signature sig = Signature.getInstance("SHA256withRSA");
			sig.initSign(privateKey);
			sig.update(challenge.getBytes("UTF8"));
			String signatureString = Base64.getEncoder().encodeToString(sig.sign());
			LOGGER.log(Level.INFO, "*** SignatureString: {0}", signatureString);
			return signatureString;
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String apiKeyForAuthentication(String prefix, String buildNumber, String signature, String keyAlgorithm) {
		// Build the response Body
		Map<String, String> body = new HashMap<String, String>();
		body.put("buildNumber", buildNumber);
		body.put("signature", signature);
		body.put("keyAlgorithm", keyAlgorithm);
		if (prefix != null && prefix.length() > 0) {
			body.put("jobProperty_hostPrefix", prefix);
		}

		ObjectMapper objectMapper = new ObjectMapper();

		try {
			return objectMapper.writeValueAsString(body);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static OkHttpClient getHttpClient() {
            //working without Conjur configuration all connect info direct from credential.
            return new OkHttpClient.Builder().build();
        }

	public static String getSecret(OkHttpClient client, ConjurAuthnInfo conjurAuthn, String authToken,
                        String variablePath) throws IOException {
		String result = null;

		LOGGER.log(Level.INFO, "Fetching secret from Conjur");
		Request request = new Request.Builder().url(
				String.format("%s/secrets/%s/variable/%s", conjurAuthn.applianceUrl, conjurAuthn.account, variablePath))
				.get().addHeader("Authorization", "Token token=\"" + authToken + "\"").build();

		Response response = client.newCall(request).execute();
		result = response.body().string();
		LOGGER.log(Level.INFO, () -> "Fetch secret [" + variablePath + "] from Conjur response " + response.code()
				+ " - " + response.message());
		if (response.code() != 200) {
			throw new IOException("Error fetching secret from Conjur [" + response.code() + " - " + response.message()
					+ "\n" + result);
		}

		return result;
	}

	private static void initializeWithCredential(ConjurAuthnInfo conjurAuthn, String credentialID,
			List<UsernamePasswordCredentials> availableCredentials) {
		if (credentialID != null && !credentialID.isEmpty()) {
			LOGGER.log(Level.INFO, "Retrieving Conjur credential stored in Jenkins");
			UsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(availableCredentials,
					CredentialsMatchers.withId(credentialID));
			if (credential != null) {
				conjurAuthn.login = credential.getUsername();
				conjurAuthn.apiKey = credential.getPassword().getPlainText();
			}
		}
	}

	private ConjurAPI() {
		super();
	}

}
