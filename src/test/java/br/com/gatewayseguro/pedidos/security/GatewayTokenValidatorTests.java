
package br.com.gatewayseguro.pedidos.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class GatewayTokenValidatorTests {

    private static final byte[] CHAVE =
            "chave-de-teste-com-32-bytes-12345!"
                    .getBytes(StandardCharsets.UTF_8);

    private static final String METODO = "GET";
    private static final String ROTA = "/api/pedidos";

    private final GatewayTokenValidator validator =
            new GatewayTokenValidator(
                    Base64.getEncoder().encodeToString(CHAVE)
            );

    private String criarToken(
            long timestamp,
            String metodo,
            String rota) throws Exception {

        byte[] bytesNonce = new byte[16];
        new SecureRandom().nextBytes(bytesNonce);

        String nonce = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytesNonce);

        String mensagem = timestamp + "\n"
                + nonce + "\n"
                + metodo + "\n"
                + rota;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(CHAVE, "HmacSHA256"));

        byte[] assinatura = mac.doFinal(
                mensagem.getBytes(StandardCharsets.UTF_8)
        );

        return timestamp + "." + nonce + "."
                + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(assinatura);
    }

    @Test
    void tokenValidoDeveSerAceito() throws Exception {

        String token = criarToken(
                Instant.now().getEpochSecond(),
                METODO,
                ROTA
        );

        assertTrue(validator.validar(token, METODO, ROTA));
    }

    @Test
    void tokenAdulteradoDeveSerRejeitado() throws Exception {

        String token = criarToken(
                Instant.now().getEpochSecond(),
                METODO,
                ROTA
        );

        String adulterado = token.substring(
                0, token.length() - 1
        ) + "!";

        assertFalse(validator.validar(
                adulterado, METODO, ROTA));
    }

    @Test
    void tokenExpiradoDeveSerRejeitado() throws Exception {

        String token = criarToken(
                Instant.now().getEpochSecond() - 60,
                METODO,
                ROTA
        );

        assertFalse(validator.validar(
                token, METODO, ROTA));
    }

    @Test
    void tokenFuturoDeveSerRejeitado() throws Exception {

        String token = criarToken(
                Instant.now().getEpochSecond() + 60,
                METODO,
                ROTA
        );

        assertFalse(validator.validar(
                token, METODO, ROTA));
    }

    @Test
    void tokenAusenteDeveSerRejeitado() {

        assertFalse(validator.validar(null, METODO, ROTA));
        assertFalse(validator.validar("", METODO, ROTA));
        assertFalse(validator.validar(" ", METODO, ROTA));
    }

    @Test
    void tokenReutilizadoDeveSerRejeitado() throws Exception {

        String token = criarToken(
                Instant.now().getEpochSecond(),
                METODO,
                ROTA
        );

        assertTrue(validator.validar(token, METODO, ROTA));

        // A segunda utilização deve ser bloqueada.
        assertFalse(validator.validar(token, METODO, ROTA));
    }

    @Test
    void metodoHttpDiferenteDeveSerRejeitado() throws Exception {

        String token = criarToken(
                Instant.now().getEpochSecond(),
                METODO,
                ROTA
        );

        assertFalse(validator.validar(
                token, "POST", ROTA));
    }

    @Test
    void rotaDiferenteDeveSerRejeitada() throws Exception {

        String token = criarToken(
                Instant.now().getEpochSecond(),
                METODO,
                ROTA
        );

        assertFalse(validator.validar(
                token, METODO, "/api/usuarios"));
    }

    @Test
    void assinaturaInvalidaNaoDeveConsumirNonce() throws Exception {

        String token = criarToken(
                Instant.now().getEpochSecond(),
                METODO,
                ROTA
        );

        String adulterado = token.substring(
                0, token.length() - 1
        ) + "!";

        assertFalse(validator.validar(
                adulterado, METODO, ROTA));

        // Uma tentativa inválida não deve impedir
        // a primeira utilização do token verdadeiro.
        assertTrue(validator.validar(
                token, METODO, ROTA));
    }

    @Test
    void tokenComFormatoAntigoDeveSerRejeitado() {

        String tokenAntigo = "1234567890.assinatura";

        assertFalse(validator.validar(
                tokenAntigo, METODO, ROTA));
    }
}
