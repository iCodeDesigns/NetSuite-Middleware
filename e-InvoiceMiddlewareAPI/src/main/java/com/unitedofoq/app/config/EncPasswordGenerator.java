package com.unitedofoq.app.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class EncPasswordGenerator {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		System.out.print(encoder.encode("APIPass"));
	}
}
