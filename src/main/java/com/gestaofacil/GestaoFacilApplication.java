package com.gestaofacil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GestaoFacilApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestaoFacilApplication.class, args);
    }
}
