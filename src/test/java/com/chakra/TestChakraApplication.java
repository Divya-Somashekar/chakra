package com.chakra;

import org.springframework.boot.SpringApplication;

public class TestChakraApplication {

    public static void main(String[] args) {
        SpringApplication.from(ChakraApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
