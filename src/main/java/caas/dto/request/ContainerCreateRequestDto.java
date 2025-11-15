package caas.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BucketCreateRequestDto {
	@NotEmpty(message = "버킷 이름 필수")
	private String name;
}
