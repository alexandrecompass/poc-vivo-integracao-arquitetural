package br.com.vivo.poc.biometria.adapter.soap;

import br.com.vivo.poc.biometria.application.exception.LegadoIndisponivelException;
import br.com.vivo.poc.biometria.application.exception.LegadoTimeoutException;
import br.com.vivo.poc.biometria.domain.Biometria;
import br.com.vivo.poc.biometria.domain.Cpf;
import br.com.vivo.poc.biometria.infrastructure.metrics.BiometriaMetrics;
import br.com.vivo.poc.biometria.port.BiometriaLegadoPort;
import org.springframework.stereotype.Component;
import org.springframework.ws.soap.client.SoapFaultClientException;

@Component
public class SoapBiometriaLegadoAdapter implements BiometriaLegadoPort {

    private final SoapBiometriaClient client;
    private final SoapBiometriaMapper mapper;
    private final SoapFaultTranslator faultTranslator;
    private final BiometriaMetrics metrics;

    public SoapBiometriaLegadoAdapter(SoapBiometriaClient client, SoapBiometriaMapper mapper,
            SoapFaultTranslator faultTranslator, BiometriaMetrics metrics) {
        this.client = client;
        this.mapper = mapper;
        this.faultTranslator = faultTranslator;
        this.metrics = metrics;
    }

    @Override
    public Biometria consultarPorCpf(Cpf cpf) {
        try {
            Object result = metrics.registrarLegadoLatencia(() -> client.consultar(cpf.getValor()));
            return mapper.toDomain(result, cpf);
        } catch (SoapFaultClientException ex) {
            throw faultTranslator.traduzir(ex);
        } catch (LegadoTimeoutException | LegadoIndisponivelException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LegadoIndisponivelException(ex.getMessage(), ex);
        }
    }
}
