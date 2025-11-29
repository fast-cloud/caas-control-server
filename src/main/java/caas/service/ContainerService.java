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
		String containerId = UUID.randomUUID().toString();
		String deploymentName = request.getClusterName() + "-" + containerId.substring(0, 8);
		String serviceName = deploymentName + "-svc";
		String ingressName = deploymentName + "-ingress";

		try {
			// Kubernetes 리소스 생성
			createDeployment(deploymentName, request.getImageLink(), request.getInternalPort());
			createService(serviceName, deploymentName, request.getInternalPort());
			createIngress(ingressName, serviceName, request.getClusterName(), request.getInternalPort());

			// DB에 Application 저장
			Application application = new Application();
			application.setAppId(containerId);
			application.setAppName(request.getClusterName());
			application.setK8sNamespace(DEFAULT_NAMESPACE);
			application.setK8sDeploymentName(deploymentName);
			application.setK8sServiceName(serviceName);
			application.setOwnerUserId(DEFAULT_OWNER_USER_ID);
			application.setCachedStatus("PENDING");
			
			application = applicationRepository.save(application);

			// DB에 Config 저장
			Config config = new Config();
			config.setConfigId(UUID.randomUUID().toString());
			config.setApplication(application);
			config.setImageLink(request.getImageLink());
			config.setExternalPort(request.getExternalPort());
			config.setInternalPort(request.getInternalPort());
			
			configRepository.save(config);

			log.info("Container created successfully. ContainerId: {}, Deployment: {}, Service: {}, Ingress: {}", 
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
			log.error("Failed to create container. Error: {}", e.getMessage(), e);
			throw new RuntimeException("컨테이너 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	private void createDeployment(String deploymentName, String imageLink, Integer internalPort) throws ApiException {
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

		appsV1Api.createNamespacedDeployment(DEFAULT_NAMESPACE, deployment);
		log.info("Deployment created: {}", deploymentName);
	}

	private void createService(String serviceName, String deploymentName, Integer internalPort) throws ApiException {
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

		coreV1Api.createNamespacedService(DEFAULT_NAMESPACE, service);
		log.info("Service created: {}", serviceName);
	}

	private void createIngress(String ingressName, String serviceName, String clusterName, Integer servicePort) throws ApiException {
		NetworkingV1Api networkingV1Api = new NetworkingV1Api(apiClient);

		String sanitizedClusterName = clusterName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
		String host = sanitizedClusterName + "." + baseDomain;

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

		networkingV1Api.createNamespacedIngress(DEFAULT_NAMESPACE, ingress);
		log.info("Ingress created: {} with host: {} -> Service: {}:{}", ingressName, host, serviceName, servicePort);
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
			// Deployment가 없거나 조회 실패 시 STOPPED로 간주
			return "STOPPED";
		}
	}
}

