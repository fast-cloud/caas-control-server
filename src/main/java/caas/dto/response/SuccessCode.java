package caas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SuccessCode {

	//200 OK
	BUCKET_CREATE_SUCCESS(20001, "버킷 생성 성공"),
	CONTAINER_CREATE_SUCCESS(20002, "컨테이너 생성 요청이 성공적으로 접수되었습니다."),
	CONTAINER_LIST_SUCCESS(20003, "컨테이너 목록을 성공적으로 조회했습니다.");

	private final int code;
	private final String message;
}