package com.unitedofoq.app.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	@Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authEx)
      throws IOException {
		logger.error("Basic Authentication Error: " + authEx.getMessage());
        response.addHeader("WWW-Authenticate", "Basic realm=" +getRealmName());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        PrintWriter writer = response.getWriter();
        writer.println("{\n"
        		+ "    \"timestamp\": "+ new Timestamp(new Date().getTime()).toString() +",\n"
        		+ "    \"status\": 401,\n"
        		+ "    \"error\": \"Unauthorized\",\n"
        		+ "    \"message\": "+ authEx.getMessage() +",\n"
        		+ "    \"path\": \"/\"\n"
        		+ "}");
    }

	@Override
    public void afterPropertiesSet() {
        setRealmName("e-InvoiceAPI");
        super.afterPropertiesSet();
    }

}
