package io.learn.lexigeek;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LexigeekApplication {

	public static void main(String[] args) {
		SpringApplication.run(LexigeekApplication.class, args);
	}

}
