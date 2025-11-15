package iaas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SuccessCode {

	//200 OK
	BUCKET_CREATE_SUCCESS(20001, "버킷 생성 성공");

	private final int code;
	private final String message;
}