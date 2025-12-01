package caas.service;

import caas.dto.request.ContainerCreateRequestDto;
import caas.dto.response.ContainerCreateResponseDto;
import caas.dto.response.ContainerListResponseDto;
import caas.entity.Application;
import caas.entity.Config;
import caas.repositoty.ApplicationRepository;
import caas.repositoty.ConfigRepository;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerService {

	private final ApiClient apiClient;
	private final ApplicationRepository applicationRepository;
	private final ConfigRepository configRepository;
	private static final String DEFAULT_NAMESPACE = "default";
	private static final String DEFAULT_OWNER_USER_ID = "1";

	@Value("${kubernetes.ingress.base-domain}")
	private String baseDomain;

	@Transactional
	public ContainerCreateResponseDto createContainer(ContainerCreateRequestDto request) {
		log.info("=== Container Creation Started ===");
		log.info("Request: clusterName={}, imageLink={}, externalPort={}, internalPort={}", 
				request.getClusterName(), request.getImageLink(), request.getExternalPort(), request.getInternalPort());
		
		String containerId = UUID.randomUUID().toString();
		String deploymentName = request.getClusterName() + "-" + containerId.substring(0, 8);
		String serviceName = deploymentName + "-svc";
		String ingressName = deploymentName + "-ingress";
		
		log.info("Generated names: containerId={}, deploymentName={}, serviceName={}, ingressName={}", 
				containerId, deploymentName, serviceName, ingressName);

		try {
			// Kubernetes 리소스 생성
			log.info("Step 1/3: Creating Deployment...");
			createDeployment(deploymentName, request.getImageLink(), request.getInternalPort());
			
			log.info("Step 2/3: Creating Service...");
			createService(serviceName, deploymentName, request.getInternalPort());
			
			log.info("Step 3/3: Creating Ingress...");
			createIngress(ingressName, serviceName, request.getClusterName(), request.getInternalPort());
			
			// 실제로 생성되었는지 확인
			log.info("Step 4/4: Verifying resources exist...");
			verifyResourceExists(deploymentName, serviceName, ingressName);

			// DB에 Application 저장
			log.info("Step 5/6: Saving Application to database...");
			Application application = new Application();
			application.setAppId(containerId);
			application.setAppName(request.getClusterName());
			application.setK8sNamespace(DEFAULT_NAMESPACE);
			application.setK8sDeploymentName(deploymentName);
			application.setK8sServiceName(serviceName);
			application.setOwnerUserId(DEFAULT_OWNER_USER_ID);
			application.setCachedStatus("PENDING");
			
			application = applicationRepository.save(application);
			log.info("Application saved: appId={}, appName={}", application.getAppId(), application.getAppName());

			// DB에 Config 저장
			log.info("Step 6/6: Saving Config to database...");
			Config config = new Config();
			config.setConfigId(UUID.randomUUID().toString());
			config.setApplication(application);
			config.setImageLink(request.getImageLink());
			config.setExternalPort(request.getExternalPort());
			config.setInternalPort(request.getInternalPort());
			
			configRepository.save(config);
			log.info("Config saved: configId={}, imageLink={}, ports={}/{}", 
					config.getConfigId(), config.getImageLink(), config.getExternalPort(), config.getInternalPort());

			log.info("=== Container Creation Completed Successfully ===");
			log.info("ContainerId: {}, Deployment: {}, Service: {}, Ingress: {}", 
					containerId, deploymentName, serviceName, ingressName);

			return ContainerCreateResponseDto.builder()
					.containerId(containerId)
					.clusterName(request.getClusterName())
					.imageLink(request.getImageLink())
					.ports(ContainerCreateResponseDto.Ports.builder()
							.external(request.getExternalPort())
							.internal(request.getInternalPort())
							.build())
					.requestTime(LocalDateTime.now())
					.status("PENDING")
					.build();

		} catch (ApiException e) {
			log.error("=== Container Creation Failed ===");
			log.error("ApiException Details:");
			log.error("  Code: {}", e.getCode());
			log.error("  Message: {}", e.getMessage());
			log.error("  Response Body: {}", e.getResponseBody());
			log.error("  Response Headers: {}", e.getResponseHeaders());
			if (e.getCause() != null) {
				log.error("  Cause: {}", e.getCause().getMessage(), e.getCause());
			}
			log.error("Full stack trace:", e);
			throw new RuntimeException("컨테이너 생성 중 오류가 발생했습니다: " + e.getMessage() + 
					" (HTTP " + e.getCode() + ")", e);
		} catch (Exception e) {
			log.error("=== Container Creation Failed with Unexpected Error ===");
			log.error("Error Type: {}", e.getClass().getName());
			log.error("Error Message: {}", e.getMessage());
			log.error("Full stack trace:", e);
			throw new RuntimeException("컨테이너 생성 중 예상치 못한 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	private void createDeployment(String deploymentName, String imageLink, Integer internalPort) throws ApiException {
		log.info("Creating Deployment: name={}, namespace={}, image={}, port={}", 
				deploymentName, DEFAULT_NAMESPACE, imageLink, internalPort);
		
		AppsV1Api appsV1Api = new AppsV1Api(apiClient);

		V1Deployment deployment = new V1Deployment()
				.apiVersion("apps/v1")
				.kind("Deployment")
				.metadata(new V1ObjectMeta()
						.name(deploymentName)
						.labels(Map.of("app", deploymentName)))
				.spec(new V1DeploymentSpec()
						.replicas(1)
						.selector(new V1LabelSelector()
								.matchLabels(Map.of("app", deploymentName)))
						.template(new V1PodTemplateSpec()
								.metadata(new V1ObjectMeta()
										.labels(Map.of("app", deploymentName)))
								.spec(new V1PodSpec()
										.containers(List.of(
												new V1Container()
														.name(deploymentName)
														.image(imageLink)
														.ports(List.of(
																new V1ContainerPort()
																		.containerPort(internalPort)
																		.name("http")))
														.imagePullPolicy("IfNotPresent")
										)))));

		try {
			log.info("Calling Kubernetes API: createNamespacedDeployment(namespace={}, name={})", 
					DEFAULT_NAMESPACE, deploymentName);
			V1Deployment created = appsV1Api.createNamespacedDeployment(DEFAULT_NAMESPACE, deployment)
					.execute();
			
			if (created != null && created.getMetadata() != null) {
				log.info("✓ Deployment created successfully: name={}, namespace={}, uid={}, creationTimestamp={}", 
						deploymentName, DEFAULT_NAMESPACE, created.getMetadata().getUid(), 
						created.getMetadata().getCreationTimestamp());
			} else {
				log.error("✗ Deployment creation returned null or empty metadata");
				throw new ApiException(500, "Deployment creation returned null");
			}
		} catch (ApiException e) {
			log.error("✗ Deployment creation failed:");
			log.error("  Name: {}, Namespace: {}", deploymentName, DEFAULT_NAMESPACE);
			log.error("  Error Code: {}", e.getCode());
			log.error("  Error Message: {}", e.getMessage());
			log.error("  Response Body: {}", e.getResponseBody());
			throw e;
		}
	}

	private void createService(String serviceName, String deploymentName, Integer internalPort) throws ApiException {
		log.info("Creating Service: name={}, namespace={}, selector={}, port={}", 
				serviceName, DEFAULT_NAMESPACE, deploymentName, internalPort);
		
		CoreV1Api coreV1Api = new CoreV1Api(apiClient);

		V1Service service = new V1Service()
				.apiVersion("v1")
				.kind("Service")
				.metadata(new V1ObjectMeta()
						.name(serviceName)
						.labels(Map.of("app", deploymentName)))
				.spec(new V1ServiceSpec()
						.type("ClusterIP")  // Ingress와 함께 사용하므로 ClusterIP로 변경
						.selector(Map.of("app", deploymentName))
						.ports(List.of(
								new V1ServicePort()
										.port(internalPort)
										.targetPort(new IntOrString(internalPort))
										.name("http")
										.protocol("TCP")
						)));

		try {
			log.info("Calling Kubernetes API: createNamespacedService(namespace={}, name={})", 
					DEFAULT_NAMESPACE, serviceName);
			V1Service created = coreV1Api.createNamespacedService(DEFAULT_NAMESPACE, service)
					.execute();
			
			if (created != null && created.getMetadata() != null) {
				log.info("✓ Service created successfully: name={}, namespace={}, uid={}, creationTimestamp={}", 
						serviceName, DEFAULT_NAMESPACE, created.getMetadata().getUid(), 
						created.getMetadata().getCreationTimestamp());
			} else {
				log.error("✗ Service creation returned null or empty metadata");
				throw new ApiException(500, "Service creation returned null");
			}
		} catch (ApiException e) {
			log.error("✗ Service creation failed:");
			log.error("  Name: {}, Namespace: {}", serviceName, DEFAULT_NAMESPACE);
			log.error("  Error Code: {}", e.getCode());
			log.error("  Error Message: {}", e.getMessage());
			log.error("  Response Body: {}", e.getResponseBody());
			throw e;
		}
	}

	private void createIngress(String ingressName, String serviceName, String clusterName, Integer servicePort) throws ApiException {
		String sanitizedClusterName = clusterName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
		String host = sanitizedClusterName + "." + baseDomain;
		
		log.info("Creating Ingress: name={}, namespace={}, host={}, service={}, port={}", 
				ingressName, DEFAULT_NAMESPACE, host, serviceName, servicePort);
		log.info("Ingress details: clusterName={} -> sanitized={} -> host={}", 
				clusterName, sanitizedClusterName, host);
		
		NetworkingV1Api networkingV1Api = new NetworkingV1Api(apiClient);

		V1Ingress ingress = new V1Ingress()
				.apiVersion("networking.k8s.io/v1")
				.kind("Ingress")
				.metadata(new V1ObjectMeta()
						.name(ingressName)
						.labels(Map.of("app", serviceName))
						.annotations(Map.of(
								"nginx.ingress.kubernetes.io/rewrite-target", "/",
								"nginx.ingress.kubernetes.io/ssl-redirect", "false"
						)))
				.spec(new V1IngressSpec()
						.ingressClassName("nginx")  // nginx ingress controller 사용 (다른 컨트롤러 사용 시 변경)
						.rules(List.of(
								new V1IngressRule()
										.host(host)
										.http(new V1HTTPIngressRuleValue()
												.paths(List.of(
														new V1HTTPIngressPath()
																.path("/")
																.pathType("Prefix")
																.backend(new V1IngressBackend()
																		.service(new V1IngressServiceBackend()
																				.name(serviceName)
																				.port(new V1ServiceBackendPort()
																						.number(servicePort)  // Service의 포트 사용
																				)
																		)
																)
												))
										)
						)));

		try {
			log.info("Calling Kubernetes API: createNamespacedIngress(namespace={}, name={})", 
					DEFAULT_NAMESPACE, ingressName);
			V1Ingress created = networkingV1Api.createNamespacedIngress(DEFAULT_NAMESPACE, ingress)
					.execute();
			
			if (created != null && created.getMetadata() != null) {
				log.info("✓ Ingress created successfully: name={}, namespace={}, host={}, uid={}, creationTimestamp={}", 
						ingressName, DEFAULT_NAMESPACE, host, created.getMetadata().getUid(), 
						created.getMetadata().getCreationTimestamp());
			} else {
				log.error("✗ Ingress creation returned null or empty metadata");
				throw new ApiException(500, "Ingress creation returned null");
			}
		} catch (ApiException e) {
			log.error("✗ Ingress creation failed:");
			log.error("  Name: {}, Namespace: {}, Host: {}", ingressName, DEFAULT_NAMESPACE, host);
			log.error("  Error Code: {}", e.getCode());
			log.error("  Error Message: {}", e.getMessage());
			log.error("  Response Body: {}", e.getResponseBody());
			throw e;
		}
	}

	public ContainerListResponseDto getContainers() {
		// DB에서 사용자의 Application 목록 조회 (하드코딩: user id = 1)
		List<Application> applications = applicationRepository.findByOwnerUserId(DEFAULT_OWNER_USER_ID);
		
		List<ContainerListResponseDto.ContainerInfo> containerInfos = new ArrayList<>();
		int runningCount = 0;
		Set<String> clusterNames = new HashSet<>();

		for (Application application : applications) {
			// Config 조회 (첫 번째 Config 사용)
			Config config = application.getConfigs().isEmpty() 
					? null 
					: application.getConfigs().get(0);

			if (config == null) {
				continue;
			}

			// Kubernetes에서 Deployment 상태 확인
			String status = getDeploymentStatus(application.getK8sNamespace(), application.getK8sDeploymentName());
			if ("RUNNING".equals(status)) {
				runningCount++;
			}

			clusterNames.add(application.getAppName());

			// ContainerInfo 생성
			ContainerListResponseDto.ContainerInfo containerInfo = ContainerListResponseDto.ContainerInfo.builder()
					.containerId(application.getAppId())
					.clusterName(application.getAppName())
					.status(status)
					.image(config.getImageLink())
					.ports(ContainerListResponseDto.ContainerInfo.Ports.builder()
							.external(config.getExternalPort())
							.internal(config.getInternalPort())
							.build())
					.createdAt(application.getCreatedAt())
					.build();

			containerInfos.add(containerInfo);
		}

		// Summary 생성
		ContainerListResponseDto.Summary summary = ContainerListResponseDto.Summary.builder()
				.totalContainers(containerInfos.size())
				.runningContainers(runningCount)
				.clusterCount(clusterNames.size())
				.build();

		return ContainerListResponseDto.builder()
				.summary(summary)
				.containers(containerInfos)
				.build();
	}

	private void verifyResourceExists(String deploymentName, String serviceName, String ingressName) {
		log.info("Starting resource verification: deployment={}, service={}, ingress={}, namespace={}", 
				deploymentName, serviceName, ingressName, DEFAULT_NAMESPACE);
		
		try {
			log.info("Waiting 500ms for resources to be created...");
			Thread.sleep(500); // 리소스가 생성될 시간 대기
			
			AppsV1Api appsV1Api = new AppsV1Api(apiClient);
			CoreV1Api coreV1Api = new CoreV1Api(apiClient);
			NetworkingV1Api networkingV1Api = new NetworkingV1Api(apiClient);
			
			// Deployment 확인
			log.info("Verifying Deployment: name={}, namespace={}", deploymentName, DEFAULT_NAMESPACE);
			try {
				V1Deployment deployment = appsV1Api.readNamespacedDeployment(deploymentName, DEFAULT_NAMESPACE).execute();
				if (deployment != null && deployment.getMetadata() != null) {
					log.info("✓ Verified: Deployment exists - name={}, namespace={}, uid={}, creationTimestamp={}", 
							deploymentName, DEFAULT_NAMESPACE, deployment.getMetadata().getUid(), 
							deployment.getMetadata().getCreationTimestamp());
					if (deployment.getStatus() != null) {
						log.info("  Deployment status: replicas={}, readyReplicas={}, availableReplicas={}", 
								deployment.getStatus().getReplicas(), 
								deployment.getStatus().getReadyReplicas(),
								deployment.getStatus().getAvailableReplicas());
					}
				} else {
					log.error("✗ Deployment verification failed: API returned null or empty metadata");
					throw new RuntimeException("Deployment 생성 확인 실패: API returned null");
				}
			} catch (ApiException e) {
				log.error("✗ Deployment NOT FOUND:");
				log.error("  Name: {}, Namespace: {}", deploymentName, DEFAULT_NAMESPACE);
				log.error("  Error Code: {}", e.getCode());
				log.error("  Error Message: {}", e.getMessage());
				log.error("  Response Body: {}", e.getResponseBody());
				throw new RuntimeException("Deployment 생성 확인 실패: " + e.getMessage() + " (Code: " + e.getCode() + ")", e);
			}
			
			// Service 확인
			log.info("Verifying Service: name={}, namespace={}", serviceName, DEFAULT_NAMESPACE);
			try {
				V1Service service = coreV1Api.readNamespacedService(serviceName, DEFAULT_NAMESPACE).execute();
				if (service != null && service.getMetadata() != null) {
					log.info("✓ Verified: Service exists - name={}, namespace={}, uid={}, creationTimestamp={}", 
							serviceName, DEFAULT_NAMESPACE, service.getMetadata().getUid(), 
							service.getMetadata().getCreationTimestamp());
					if (service.getSpec() != null) {
						log.info("  Service spec: type={}, clusterIP={}", 
								service.getSpec().getType(), service.getSpec().getClusterIP());
					}
				} else {
					log.error("✗ Service verification failed: API returned null or empty metadata");
					throw new RuntimeException("Service 생성 확인 실패: API returned null");
				}
			} catch (ApiException e) {
				log.error("✗ Service NOT FOUND:");
				log.error("  Name: {}, Namespace: {}", serviceName, DEFAULT_NAMESPACE);
				log.error("  Error Code: {}", e.getCode());
				log.error("  Error Message: {}", e.getMessage());
				log.error("  Response Body: {}", e.getResponseBody());
				throw new RuntimeException("Service 생성 확인 실패: " + e.getMessage() + " (Code: " + e.getCode() + ")", e);
			}
			
			// Ingress 확인
			log.info("Verifying Ingress: name={}, namespace={}", ingressName, DEFAULT_NAMESPACE);
			try {
				V1Ingress ingress = networkingV1Api.readNamespacedIngress(ingressName, DEFAULT_NAMESPACE).execute();
				if (ingress != null && ingress.getMetadata() != null) {
					log.info("✓ Verified: Ingress exists - name={}, namespace={}, uid={}, creationTimestamp={}", 
							ingressName, DEFAULT_NAMESPACE, ingress.getMetadata().getUid(), 
							ingress.getMetadata().getCreationTimestamp());
					if (ingress.getSpec() != null && ingress.getSpec().getRules() != null) {
						ingress.getSpec().getRules().forEach(rule -> 
							log.info("  Ingress rule: host={}", rule.getHost()));
					}
				} else {
					log.error("✗ Ingress verification failed: API returned null or empty metadata");
					throw new RuntimeException("Ingress 생성 확인 실패: API returned null");
				}
			} catch (ApiException e) {
				log.error("✗ Ingress NOT FOUND:");
				log.error("  Name: {}, Namespace: {}", ingressName, DEFAULT_NAMESPACE);
				log.error("  Error Code: {}", e.getCode());
				log.error("  Error Message: {}", e.getMessage());
				log.error("  Response Body: {}", e.getResponseBody());
				throw new RuntimeException("Ingress 생성 확인 실패: " + e.getMessage() + " (Code: " + e.getCode() + ")", e);
			}
			
			log.info("✓ All resources verified successfully");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("✗ Verification interrupted", e);
			throw new RuntimeException("리소스 검증 중단됨", e);
		} catch (RuntimeException e) {
			log.error("✗ Verification failed with RuntimeException", e);
			throw e;
		} catch (Exception e) {
			log.error("✗ Verification failed with unexpected error", e);
			throw new RuntimeException("리소스 검증 중 예상치 못한 오류 발생", e);
		}
	}

	private String getDeploymentStatus(String namespace, String deploymentName) {
		try {
			AppsV1Api appsV1Api = new AppsV1Api(apiClient);
			V1Deployment deployment = appsV1Api.readNamespacedDeployment(deploymentName, namespace)
					.execute();

			if (deployment == null || deployment.getStatus() == null) {
				return "UNKNOWN";
			}

			V1DeploymentStatus status = deployment.getStatus();
			Integer replicas = status.getReplicas();
			Integer readyReplicas = status.getReadyReplicas();
			Integer availableReplicas = status.getAvailableReplicas();

			// Deployment가 실행 중인지 확인
			if (replicas != null && replicas > 0) {
				if (readyReplicas != null && readyReplicas > 0 && availableReplicas != null && availableReplicas > 0) {
					return "RUNNING";
				} else {
					return "PENDING";
				}
			} else {
				return "STOPPED";
			}
		} catch (ApiException e) {
			log.warn("Failed to get deployment status for {} in namespace {}: {}", deploymentName, namespace, e.getMessage());
			// Deployment가 없거나 조회 실패 시 STOPPED로 간주./
			return "STOPPED";
		}
	}
}

