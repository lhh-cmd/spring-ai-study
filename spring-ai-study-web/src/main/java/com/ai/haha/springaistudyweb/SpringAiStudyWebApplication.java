package com.ai.haha.springaistudyweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;

@SpringBootApplication(
		scanBasePackages = {"com.ai.haha.springaistudyweb", "com.ai.haha.springaistudyservice"},
		exclude = {ContextFunctionCatalogAutoConfiguration.class}
)
public class SpringAiStudyWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiStudyWebApplication.class, args);
	}

}
