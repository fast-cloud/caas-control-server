package caas.repositoty;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import caas.entity.Application;

public interface ApplicationRepository extends JpaRepository<Application, String> {
    Optional<Application> findByAppId(String appId);
    List<Application> findByOwnerUserId(String ownerUserId);
}
