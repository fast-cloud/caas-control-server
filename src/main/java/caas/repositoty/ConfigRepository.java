package caas.repositoty;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import caas.entity.Config;

public interface ConfigRepository extends JpaRepository<Config, String> {

    Optional<Config> findByConfigId(String configId);
}