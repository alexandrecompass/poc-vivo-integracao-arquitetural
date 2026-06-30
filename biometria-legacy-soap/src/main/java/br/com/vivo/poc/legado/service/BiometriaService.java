package br.com.vivo.poc.legado.service;

import br.com.vivo.poc.legado.domain.Biometria;
import br.com.vivo.poc.legado.logging.CpfMasker;
import br.com.vivo.poc.legado.repository.BiometriaRepository;
import br.com.vivo.poc.legado.ws.metrics.SoapMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class BiometriaService {

    private static final Logger log = LoggerFactory.getLogger(BiometriaService.class);

    private final BiometriaRepository repository;
    private final CpfMasker cpfMasker;
    private final SoapMetrics metrics;

    public BiometriaService(BiometriaRepository repository, CpfMasker cpfMasker, SoapMetrics metrics) {
        this.repository = repository;
        this.cpfMasker = cpfMasker;
        this.metrics = metrics;
    }

    public Biometria consultarPorCpf(String cpf) {
        log.info("db_query_started", kv("event", "db_query_started"), kv("cpf", cpfMasker.mask(cpf)));

        Biometria biometria = metrics.registrarDbLatencia(() -> repository.findByCpf(cpf).orElse(null));

        log.info("db_query_finished", kv("event", "db_query_finished"), kv("cpf", cpfMasker.mask(cpf)));

        if (biometria == null) {
            metrics.incrementarNaoEncontrada();
            log.warn("biometria_not_found", kv("event", "biometria_not_found"), kv("cpf", cpfMasker.mask(cpf)));
            throw new BiometriaNaoEncontradaException("Biometria não encontrada para o CPF informado");
        }

        log.info("biometria_found", kv("event", "biometria_found"), kv("cpf", cpfMasker.mask(cpf)));
        return biometria;
    }
}
