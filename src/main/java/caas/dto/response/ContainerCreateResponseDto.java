package caas.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ContainerCreateResponseDto(
		@JsonProperty("containerId")
		String containerId,

		@JsonProperty("clusterName")
		String clusterName,

		@JsonProperty("imageLink")
		String imageLink,

		@JsonProperty("ports")
		Ports ports,

		@JsonProperty("requestTime")
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
		LocalDateTime requestTime,

		@JsonProperty("status")
		String status
) {
	@Builder
	public record Ports(
			@JsonProperty("external")
			Integer external,

			@JsonProperty("internal")
			Integer internal
	) {}
}