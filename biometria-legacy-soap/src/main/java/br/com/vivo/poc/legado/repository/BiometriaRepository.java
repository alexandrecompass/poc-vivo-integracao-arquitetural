package br.com.vivo.poc.legado.repository;

import br.com.vivo.poc.legado.domain.Biometria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BiometriaRepository extends JpaRepository<Biometria, Long> {

    Optional<Biometria> findByCpf(String cpf);
}
