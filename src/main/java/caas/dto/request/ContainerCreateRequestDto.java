package caas.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerCreateRequestDto {
	@NotEmpty(message = "클러스터 이름은 필수입니다.")
	private String clusterName;

	@NotEmpty(message = "이미지 링크는 필수입니다.")
	private String imageLink;

	@NotNull(message = "외부 포트는 필수입니다.")
	private Integer externalPort;

	@NotNull(message = "내부 포트는 필수입니다.")
	private Integer internalPort;
}
