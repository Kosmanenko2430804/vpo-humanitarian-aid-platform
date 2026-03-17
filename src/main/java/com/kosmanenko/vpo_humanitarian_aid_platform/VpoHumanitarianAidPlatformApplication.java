package com.kosmanenko.vpo_humanitarian_aid_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VpoHumanitarianAidPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(VpoHumanitarianAidPlatformApplication.class, args);
    }

}
