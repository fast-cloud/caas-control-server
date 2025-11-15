package caas.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "caas_application")
@Data
public class Application {

    @Id
    @Column(name = "app_id", length = 36)
    private String appId;

    @Column(name = "app_name", nullable = false, length = 255)
    private String appName;

    @Column(name = "k8s_namespace", nullable = false, length = 255)
    private String k8sNamespace;

    @Column(name = "k8s_deployment_name", nullable = false, length = 255)
    private String k8sDeploymentName;

    @Column(name = "k8s_service_name", nullable = false, length = 255)
    private String k8sServiceName;

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private String ownerUserId;

    @Column(name = "cached_status", nullable = false, length = 50)
    private String cachedStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Config> configs = new ArrayList<>();
}

