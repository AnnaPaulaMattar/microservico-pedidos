
package br.com.gatewayseguro.pedidos.controller;

import br.com.gatewayseguro.pedidos.security.GatewayTokenValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PedidoController {

    private final GatewayTokenValidator tokenValidator;

    public PedidoController(GatewayTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @GetMapping("/pedidos")
    public ResponseEntity<?> listarPedidos(
            @RequestHeader(
                    value = "X-Gateway-Authenticated",
                    required = false
            ) String autenticado,
            @RequestHeader(
                    value = "X-Gateway-Token",
                    required = false
            ) String tokenInterno) {

        if (!"true".equals(autenticado)
                || !tokenValidator.validar(
                tokenInterno,
                "GET",
                "/api/pedidos")) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "erro", "ACESSO_NEGADO",
                            "mensagem", "Credencial interna ausente ou invalida"
                    ));
        }

        List<Map<String, Object>> pedidos = List.of(
                Map.of(
                        "id", 1,
                        "produto", "Notebook",
                        "quantidade", 1,
                        "status", "APROVADO"
                ),
                Map.of(
                        "id", 2,
                        "produto", "Mouse",
                        "quantidade", 2,
                        "status", "PENDENTE"
                ),
                Map.of(
                        "id", 3,
                        "produto", "Teclado",
                        "quantidade", 1,
                        "status", "ENVIADO"
                )
        );

        return ResponseEntity.ok(pedidos);
    }
}
