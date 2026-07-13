package com.heichan.camera;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class CameraAccessBackendApplicationTests {

	@Test
	void generatePassword() {

		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		String encoded = encoder.encode("123456");

		System.out.println(encoded);
		System.out.println(encoder.matches("123456", encoded));
	}

}
