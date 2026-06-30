package br.com.vivo.poc.biometria.api.mapper;

import br.com.vivo.poc.biometria.api.dto.BiometriaResponse;
import br.com.vivo.poc.biometria.domain.Biometria;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class BiometriaResponseMapper {

    public BiometriaResponse toResponse(Biometria biometria) {
        return new BiometriaResponse(
                biometria.getCpf().toString(),
                biometria.isBiometriaDisponivel(),
                biometria.getImagemBase64(),
                biometria.getOrigem().name(),
                biometria.getDataConsulta().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                MDC.get("correlationId")
        );
    }
}
