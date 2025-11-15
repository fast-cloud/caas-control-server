package caas.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BucketCreateResponseDto(
		String name,
		String status,

		@JsonProperty("created_at")
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm'Z'")
		LocalDateTime createdAt
) {}