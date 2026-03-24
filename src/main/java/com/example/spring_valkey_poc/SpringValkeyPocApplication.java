package com.example.spring_valkey_poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SpringValkeyPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringValkeyPocApplication.class, args);
	}

}
