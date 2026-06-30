package br.com.vivo.poc.biometria.infrastructure.health;

import br.com.vivo.poc.biometria.adapter.soap.SoapBiometriaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("legadoSoap")
public class LegadoSoapHealthIndicator implements HealthIndicator {

    private final SoapBiometriaClient soapBiometriaClient;
    private final String soapUrl;

    public LegadoSoapHealthIndicator(SoapBiometriaClient soapBiometriaClient,
            @Value("${soap.legacy.url}") String soapUrl) {
        this.soapBiometriaClient = soapBiometriaClient;
        this.soapUrl = soapUrl;
    }

    @Override
    public Health health() {
        try {
            soapBiometriaClient.consultar("52998224725");
            return Health.up()
                    .withDetail("legado", "disponivel")
                    .withDetail("url", soapUrl)
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("legado", "indisponivel")
                    .withDetail("url", soapUrl)
                    .withDetail("erro", ex.getMessage())
                    .build();
        }
    }
}
