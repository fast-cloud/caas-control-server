package caas.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

@Configuration
public class KubernetesConfig {

	@Bean
	public ApiClient kubernetesApiClient() throws IOException {
		ApiClient client;
		String kubeConfigPath = System.getenv("KUBECONFIG");
		
		if (kubeConfigPath != null && !kubeConfigPath.isEmpty()) {
			// 환경변수로 지정된 kubeconfig 파일 사용
			client = ClientBuilder.kubeconfig(
					KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))
			).build();
		} else {
			// 기본 위치에서 kubeconfig 파일 찾기
			String homeDir = System.getProperty("user.home");
			String defaultKubeConfig = Paths.get(homeDir, ".kube", "config").toString();
			
			try {
				client = ClientBuilder.kubeconfig(
						KubeConfig.loadKubeConfig(new FileReader(defaultKubeConfig))
				).build();
			} catch (IOException e) {
				// kubeconfig 파일이 없으면 클러스터 내부에서 실행 중인 것으로 간주
				client = ClientBuilder.cluster().build();
			}
		}
		
		io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
		return client;
	}
}

