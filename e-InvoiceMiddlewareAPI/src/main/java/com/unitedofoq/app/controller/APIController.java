package com.unitedofoq.app.controller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.management.ServiceNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException.Unauthorized;

import com.google.common.io.CharStreams;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@RestController
public class APIController {

	String tokenUrl = "https://id.preprod.eta.gov.eg/connect/token";
	String subumitDocumentURL = "https://api.preprod.invoicing.eta.gov.eg/api/v1.0/documentsubmissions";
	CloseableHttpResponse response = null;

	@RequestMapping(value = "/submitdocument", method = RequestMethod.POST)
	@Retryable(value = { ServiceNotFoundException.class, ConnectException.class, Unauthorized.class,
			ParseException.class, UnsupportedEncodingException.class, UnsupportedOperationException.class,
			IOException.class }, maxAttempts = 2, backoff = @Backoff(delay = 2000))
	public @ResponseBody JSONObject callPost(@RequestHeader String client_id, @RequestHeader String client_secret,
			@RequestBody String documents) throws ParseException, NoSuchAlgorithmException, KeyStoreException,
			KeyManagementException, UnsupportedOperationException, URISyntaxException, IOException {
		JSONObject submitionresult = null;
		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(null, new TrustStrategy() {
			@Override
			public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				return true;
			}
		});

		SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(builder.build(),
				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		HttpClient client = HttpClients.custom().setSSLSocketFactory(sslSF).build();

		HttpPost httpPost = new HttpPost();
		httpPost.setHeader("Accept", "*/*");

		RequestConfig.Builder requestConfig;
		requestConfig = RequestConfig.custom();
		requestConfig.setConnectTimeout(5000);
		requestConfig.setConnectionRequestTimeout(5000);

		if (requestConfig != null) {
			httpPost.setConfig(requestConfig.build());
		}

		String accessToken = "Bearer " + getToken(client, httpPost, client_id, client_secret) + "aabb";
		System.out.println(accessToken);
		submitionresult = submitDocument(client, httpPost, accessToken, documents);

		response.close();
		return submitionresult;
	}

	@Recover
	public JSONObject recover(Exception t) throws ParseException {
		String failuer = "{\"result\":\"" + t.getStackTrace()[0] + "\"}";
		JSONParser parser = new JSONParser();
		JSONObject result = (JSONObject) parser.parse(failuer);
		return result;
	}

	private String getToken(HttpClient client, HttpPost httpPost, String clientID, String clientSecret) {
		try {
			httpPost.setURI(new URI(tokenUrl));
			httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("grant_type", "client_credentials"));
			params.add(new BasicNameValuePair("scope", "InvoicingAPI"));
			params.add(new BasicNameValuePair("client_id", clientID));
			params.add(new BasicNameValuePair("client_secret", clientSecret));
			httpPost.setEntity(new UrlEncodedFormEntity(params));

			response = (CloseableHttpResponse) client.execute(httpPost);
			JSONObject json = parseResponseToJsonObject(response);
			return json.get("access_token").toString();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return "Error While Get Token : " + e.getMessage();

		}
	}

	private JSONObject submitDocument(HttpClient client, HttpPost httpPost, String token, String documents)
			throws URISyntaxException, UnsupportedOperationException, IOException, ParseException {
		JSONObject json = null;

		httpPost.setURI(new URI(subumitDocumentURL));
		httpPost.setHeader("Authorization", token);
		httpPost.setHeader("Content-type", "application/json");
		httpPost.setEntity(new StringEntity(documents));

		response = (CloseableHttpResponse) client.execute(httpPost);
		json = parseResponseToJsonObject(response);
		return json;
	}

	private JSONObject parseResponseToJsonObject(CloseableHttpResponse response)
			throws UnsupportedEncodingException, UnsupportedOperationException, IOException, ParseException {
		JSONObject json = null;
		if (response != null) {
			String result = null;
			InputStreamReader reader = null;
			JSONParser parser = new JSONParser();
			reader = new InputStreamReader(response.getEntity().getContent(), "UTF-8");
			result = CharStreams.toString(reader);
			if (result.contains("NOT_FOUND")) {
				return null;
			}
			json = (JSONObject) parser.parse(result);

		}
		return json;
	}

	@RequestMapping(value="/logout", method=RequestMethod.GET)  
    public String logoutPage(HttpServletRequest request, HttpServletResponse response) {  
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();  
        if (auth != null){      
           new SecurityContextLogoutHandler().logout(request, response, auth);  
        }  
         return "redirect:/";  
     }  
}
