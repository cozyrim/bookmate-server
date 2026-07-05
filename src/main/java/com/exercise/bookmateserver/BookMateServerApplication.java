package com.exercise.bookmateserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class BookMateServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookMateServerApplication.class, args);
	}

}
