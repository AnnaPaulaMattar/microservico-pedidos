package br.com.gatewayseguro.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MicroservicoPedidosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroservicoPedidosApplication.class, args);
    }

}
