package caas.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ContainerListResponseDto(
		@JsonProperty("summary")
		Summary summary,

		@JsonProperty("containers")
		List<ContainerInfo> containers
) {
	@Builder
	public record Summary(
			@JsonProperty("totalContainers")
			Integer totalContainers,

			@JsonProperty("runningContainers")
			Integer runningContainers,

			@JsonProperty("clusterCount")
			Integer clusterCount
	) {}

	@Builder
	public record ContainerInfo(
			@JsonProperty("containerId")
			String containerId,

			@JsonProperty("clusterName")
			String clusterName,

			@JsonProperty("status")
			String status,

			@JsonProperty("image")
			String image,

			@JsonProperty("ports")
			Ports ports,

			@JsonProperty("createdAt")
			@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
			LocalDateTime createdAt
	) {
		@Builder
		public record Ports(
				@JsonProperty("external")
				Integer external,

				@JsonProperty("internal")
				Integer internal
		) {}
	}
}

