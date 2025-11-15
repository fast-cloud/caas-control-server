package caas.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "caas_config")
@Data
public class Config {

    @Id
    @Column(name = "config_id", length = 36)
    private String configId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private Application application;

    @Column(name = "image_link", nullable = false, length = 255)
    private String imageLink;

    @Column(name = "external_port", nullable = false)
    private Integer externalPort;

    @Column(name = "internal_port", nullable = false)
    private Integer internalPort;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

