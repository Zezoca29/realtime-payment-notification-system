package com.zez.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentProducerApplication.class, args);
    }
}
