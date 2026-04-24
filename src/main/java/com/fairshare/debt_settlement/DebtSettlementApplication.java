package com.fairshare.debt_settlement;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class  DebtSettlementApplication {

	public static void main(String[] args) {
		// Load .env file and set system properties
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();
		
		dotenv.entries().forEach(entry -> 
			System.setProperty(entry.getKey(), entry.getValue())
		);

		SpringApplication.run(DebtSettlementApplication.class, args);
	}
}
