
package br.com.gatewayseguro.pedidos.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;

@Component
public class GatewayTokenValidator {

    private static final long VALIDADE_SEGUNDOS = 30;

    private final byte[] chaveSecreta;

    private final ConcurrentHashMap<String, Long> noncesUtilizados =
            new ConcurrentHashMap<>();

    public GatewayTokenValidator(
            @Value("${gateway.internal.secret}") String segredo) {

        this.chaveSecreta = Base64.getDecoder().decode(segredo);

        if (chaveSecreta.length < 32) {
            throw new IllegalArgumentException(
                    "A chave interna deve ter pelo menos 256 bits");
        }
    }

    public boolean validar(String token, String metodo, String rota) {

        if (token == null || token.isBlank()
                || metodo == null || rota == null) {
            return false;
        }

        try {
            String[] partes = token.split("\\.", -1);

            if (partes.length != 3) {
                return false;
            }

            String timestamp = partes[0];
            String nonce = partes[1];

            byte[] bytesNonce =
                    Base64.getUrlDecoder().decode(nonce);

            if (bytesNonce.length != 16) {
                return false;
            }

            byte[] assinaturaRecebida =
                    Base64.getUrlDecoder().decode(partes[2]);

            long instante = Long.parseLong(timestamp);
            long agora = Instant.now().getEpochSecond();

            if (instante > agora
                    || agora - instante > VALIDADE_SEGUNDOS) {
                return false;
            }

            String mensagem = timestamp + "\n"
                    + nonce + "\n"
                    + metodo + "\n"
                    + rota;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    chaveSecreta, "HmacSHA256"));

            byte[] assinaturaEsperada = mac.doFinal(
                    mensagem.getBytes(StandardCharsets.UTF_8));

            if (!MessageDigest.isEqual(
                    assinaturaEsperada, assinaturaRecebida)) {
                return false;
            }

            String identificador = timestamp + "." + nonce;

            // Apenas a primeira utilização do nonce é aceita.
            Long anterior = noncesUtilizados.putIfAbsent(
                    identificador, instante);

            return anterior == null;

        } catch (Exception e) {
            return false;
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void limparNoncesExpirados() {

        long agora = Instant.now().getEpochSecond();

        noncesUtilizados.entrySet().removeIf(entrada ->
                agora - entrada.getValue() > VALIDADE_SEGUNDOS
        );
    }
}
