package br.com.vivo.poc.legado.seed;

import br.com.vivo.poc.legado.domain.Biometria;
import br.com.vivo.poc.legado.repository.BiometriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BiometriaSeedLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BiometriaSeedLoader.class);

    private static final List<String[]> CPFS_SEED = List.of(
            new String[] {"52998224725", "aW1hZ2VtLWZpY3RpY2lhLTUyOTk4"},
            new String[] {"11144477735", "aW1hZ2VtLWZpY3RpY2lhLTExMTQ0"},
            new String[] {"87126398870", "aW1hZ2VtLWZpY3RpY2lhLTg3MTI2"},
            new String[] {"56872389510", "aW1hZ2VtLWZpY3RpY2lhLTU2ODcy"},
            new String[] {"04896019630", "aW1hZ2VtLWZpY3RpY2lhLTA0ODk2"},
            new String[] {"72687789120", "aW1hZ2VtLWZpY3RpY2lhLTcyNjg3"},
            new String[] {"98473126540", "aW1hZ2VtLWZpY3RpY2lhLTk4NDcz"},
            new String[] {"01546532755", "aW1hZ2VtLWZpY3RpY2lhLTAxNTQ2"},
            new String[] {"31279940582", "aW1hZ2VtLWZpY3RpY2lhLTMxMjc5"},
            new String[] {"45261897366", "aW1hZ2VtLWZpY3RpY2lhLTQ1MjYx"}
    );

    private final BiometriaRepository repository;

    public BiometriaSeedLoader(BiometriaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.info("event=seed_ignorado motivo=banco_ja_populado");
            return;
        }

        CPFS_SEED.forEach(dados -> repository.save(
                new Biometria(dados[0], true, dados[1], LocalDateTime.now())));

        log.info("event=seed_concluido total={}", CPFS_SEED.size());
    }
}
