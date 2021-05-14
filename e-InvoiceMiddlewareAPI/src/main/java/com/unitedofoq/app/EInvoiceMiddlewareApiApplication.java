package com.unitedofoq.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;

@ComponentScan(basePackages = "com.unitedofoq")
@SpringBootApplication
@EnableRetry
public class EInvoiceMiddlewareApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EInvoiceMiddlewareApiApplication.class, args);
	}

}
