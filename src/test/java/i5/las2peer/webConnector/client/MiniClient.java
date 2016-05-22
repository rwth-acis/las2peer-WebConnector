package i5.las2peer.webConnector.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import i5.las2peer.restMapper.data.Pair;

/**
 * Very simple client to communicate with the las2peer web connector
 *
 *
 */
public class MiniClient {

	private String authorization;
	private String serverAddress;

	/**
	 * set address and port
	 * 
	 * @param address address of the server
	 * @param port if 0 no port is appended to the address
	 */
	public void setAddressPort(String address, int port) {
		serverAddress = address;
		if (port > 0) {
			serverAddress += ":" + Integer.toString(port);
		}
	}

	/**
	 * set login data
	 * 
	 * @param username
	 * @param password
	 * @throws UnsupportedEncodingException
	 */
	public void setLogin(String username, String password) throws UnsupportedEncodingException {
		authorization = username + ":" + password;// Base64.encodeBytes((username+":"+password).getBytes("UTF-8"));
		authorization = Base64.encodeBase64String(authorization.getBytes());
	}

	/**
	 * send request to server
	 * 
	 * @param method POST, GET, DELETE, PUT
	 * @param uri REST-URI (server address excluded)
	 * @param content if POST is used information can be embedded here
	 * @param contentType value of Content-Type header
	 * @param accept value of Accept Header
	 * @param headers headers for HTTP request
	 * @return returns server response
	 * 
	 */
	public ClientResponse sendRequest(String method, String uri, String content, String contentType, String accept,
			Pair<String>[] headers) {
		System.out.println("Request: " + method + " URI: " + uri + " Content: " + content + " Content-type: "
				+ contentType + " accept: " + accept + " headers: " + headers.length);
		HttpURLConnection connection = null;
		ClientResponse response;
		try {
			// Create connection
			URL url = new URL(String.format("%s/%s", serverAddress, uri));
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(method.toUpperCase());
			if (authorization != null && authorization.length() > 0) {
				connection.setRequestProperty("Authorization", "Basic " + authorization);
			}
			connection.setRequestProperty("Content-Type", contentType);
			connection.setRequestProperty("Accept", accept);
			connection.setRequestProperty("Content-Length", Integer.toString(content.getBytes().length));

			for (Pair<String> header : headers) {
				connection.setRequestProperty(header.getOne(), header.getTwo());
			}

			connection.setUseCaches(false);
			connection.setDoInput(true);
			if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
				connection.setDoOutput(true);

				// Send request
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(content);
				wr.close();
			}
			int code = connection.getResponseCode();

			response = new ClientResponse(code);

			// Get Response
			InputStream is = null;
			try {
				is = connection.getErrorStream();
			} catch (Exception e) {
				// do nothing
			}

			try {
				is = connection.getInputStream();
			} catch (Exception e) {
				// do nothing
			}
			if (is == null) {
				return response;
			}

			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			StringBuilder responseText = new StringBuilder();
			String line;
			while ((line = rd.readLine()) != null) {
				responseText.append(line);
				responseText.append('\r');
			}
			response.setResponse(responseText.toString());

			Map<String, List<String>> responseMap = connection.getHeaderFields();
			StringBuilder sb = new StringBuilder();
			for (Iterator<String> iterator = responseMap.keySet().iterator(); iterator.hasNext();) {
				String key = iterator.next();

				sb.setLength(0);

				List<String> values = responseMap.get(key);
				for (int i = 0; i < values.size(); i++) {
					Object o = values.get(i);
					sb.append(" " + o);
				}

				if (key == null) {
					key = "head";
				}

				response.addHeader(key.trim(), sb.toString().trim());
			}

			rd.close();
			return response;

		} catch (Exception e) {

			e.printStackTrace();
			return null;

		} finally {

			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * send request to server
	 * 
	 * @param method POST, GET, DELETE, PUT
	 * @param uri REST-URI (server address excluded)
	 * @param content if POST is used information can be embedded here
	 * @param headers headers for HTTP request
	 * @return returns server response
	 * 
	 */
	public ClientResponse sendRequest(String method, String uri, String content, Pair<String>[] headers) {
		return sendRequest(method, uri, content, "text/plain", "*/*", headers);
	}

	/**
	 * send request to server
	 * 
	 * @param method POST, GET, DELETE, PUT
	 * @param uri REST-URI (server address excluded)
	 * @param content if POST is used information can be embedded here
	 * @return returns server response
	 * 
	 */
	@SuppressWarnings("unchecked")
	public ClientResponse sendRequest(String method, String uri, String content) {
		return sendRequest(method, uri, content, new Pair[] {});
	}

}