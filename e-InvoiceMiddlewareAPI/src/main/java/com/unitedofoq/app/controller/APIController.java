package com.unitedofoq.app.controller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.io.CharStreams;

import lombok.extern.slf4j.Slf4j;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Slf4j
@RestController
public class APIController {

	private final Logger logger = LogManager.getLogger(this.getClass());
	String tokenUrl = "https://id.preprod.eta.gov.eg/connect/token";
	String subumitDocumentURL = "https://api.preprod.invoicing.eta.gov.eg/api/v1.0/documentsubmissions";
	CloseableHttpResponse response = null;

	@RequestMapping(value = "/submitdocument", method = RequestMethod.POST)
	@Retryable(value = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 5000))
	public @ResponseBody JSONObject callPost(@RequestHeader String client_id, @RequestHeader String client_secret,
			@RequestBody String documents) throws ParseException, NoSuchAlgorithmException, KeyStoreException,
			KeyManagementException, URISyntaxException, IOException, NotFoundException {
		JSONObject submitionResult = null;
		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(null, new TrustStrategy() {
			@Override
			public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				return true;
			}
		});

		SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(builder.build(),
				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		logger.info("Initiate Submit Document Request");
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

		String accessToken = "Bearer " + getToken(client, httpPost, client_id, client_secret);
		System.out.println(accessToken);
		submitionResult = submitDocument(client, httpPost, accessToken, documents);

		response.close();
		logger.info("Submit Document Request Ended");
		return submitionResult;
	}

	@Recover
	public JSONObject recover(ResponseStatusException responseStatus) throws ParseException {
		logger.info("Submit Document Request Ended With Error: " + responseStatus.getStatus());
		if(responseStatus.getStatus() == HttpStatus.NOT_FOUND) {
			throw new ResponseStatusException(
					responseStatus.getStatus(), "e-Invoice API Error: Not Found");
		}
		else {
			throw new ResponseStatusException(
					responseStatus.getStatus(), responseStatus.getReason());
		}
	}

	private String getToken(HttpClient client, HttpPost httpPost, String clientID, String clientSecret) {
		try {
			JSONObject json = null;
			httpPost.setURI(new URI(tokenUrl));
			httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("grant_type", "client_credentials"));
			params.add(new BasicNameValuePair("scope", "InvoicingAPI"));
			params.add(new BasicNameValuePair("client_id", clientID));
			params.add(new BasicNameValuePair("client_secret", clientSecret));
			httpPost.setEntity(new UrlEncodedFormEntity(params));

			logger.info("Forward Token Request " + httpPost.getURI());
			response = (CloseableHttpResponse) client.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			if(statusCode >= 200 && statusCode < 300) {
				logger.info("Success: Received Generate Token Response: " + response.getStatusLine());
				json = parseResponseToJsonObject(response);
			}
			else {
				logger.error("Error: Received Generate Token Response: " + response.getStatusLine());
				throw new ResponseStatusException(
	                    HttpStatus.valueOf(statusCode));
			}	
			return json.get("access_token").toString();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return "Error While Get Token : " + e.getMessage();

		}
	}

	private JSONObject submitDocument(HttpClient client, HttpPost httpPost, String token, String documents)
			throws URISyntaxException, IOException, ParseException, NotFoundException {
		JSONObject json = null;

		httpPost.setURI(new URI(subumitDocumentURL));
		httpPost.setHeader("Authorization", token);
		httpPost.setHeader("Content-type", "application/json");
		httpPost.setEntity(new StringEntity(documents));
		
		logger.info("Forward Document Submission Request: " + httpPost.getURI());
		response = (CloseableHttpResponse) client.execute(httpPost);
		int statusCode = response.getStatusLine().getStatusCode();
		if(statusCode >= 200 && statusCode < 300) {
			logger.info("Success: Received Submission Response: " + response.getStatusLine());
			json = parseResponseToJsonObject(response);
		}
		else {
			logger.error("Error: Received Submission Response: " + response.getStatusLine());
			throw new ResponseStatusException(
                    HttpStatus.valueOf(statusCode));
		}
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
 
}
