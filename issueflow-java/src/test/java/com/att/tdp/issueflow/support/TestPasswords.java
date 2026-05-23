package com.att.tdp.issueflow.support;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class TestPasswords {

	public static final String RAW = "secret";

	private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

	private TestPasswords() {}

	public static String encoded() {
		return ENCODER.encode(RAW);
	}
}
