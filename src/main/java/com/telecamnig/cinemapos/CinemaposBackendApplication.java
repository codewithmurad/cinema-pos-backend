package com.telecamnig.cinemapos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks for seat hold expiry
public class CinemaposBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CinemaposBackendApplication.class, args);
	}

}
