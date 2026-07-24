package com.flight.booking;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class BookingApplication {

	public static void main(String[] args) {
		// Run the whole app in IST so timestamps are stored/displayed in Indian Standard Time.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(BookingApplication.class, args);
	}

	@PostConstruct
	void ensureIstTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
	}

}
