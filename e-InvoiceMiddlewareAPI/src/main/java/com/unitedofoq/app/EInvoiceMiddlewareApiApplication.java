package com.unitedofoq.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;

@ComponentScan(basePackages = "com.unitedofoq")
@SpringBootApplication
@EnableRetry
public class EInvoiceMiddlewareApiApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(EInvoiceMiddlewareApiApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(EInvoiceMiddlewareApiApplication.class);
	}
}
