package com.github.smartretry.samples;

import com.github.smartretry.spring4.EnableRetrying;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AdviceMode;

@EnableRetrying(mode = AdviceMode.ASPECTJ)
@SpringBootApplication
public class SamplesApplication {
    public static void main(String[] args) {
        SpringApplication.run(SamplesApplication.class, args);
    }
}