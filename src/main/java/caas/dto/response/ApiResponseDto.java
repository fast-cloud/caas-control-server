package iaas.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.NonNull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
public record ApiResponseDto<T>(
		int code,
		@NonNull String message,
		@JsonInclude(value = NON_NULL) T data
) {
	public static <T> ApiResponseDto<T> success(final SuccessCode successCode, @Nullable final T data) {
		return ApiResponseDto.<T>builder()
				.code(successCode.getCode())
				.message(successCode.getMessage())
				.data(data)
				.build();
	}

	public static <T> ApiResponseDto<T> success(final SuccessCode successCode) {
		return ApiResponseDto.<T>builder()
				.code(successCode.getCode())
				.message(successCode.getMessage())
				.data(null)
				.build();
	}

	public static <T> ApiResponseDto<T> fail(final ErrorCode errorCode) {
		return ApiResponseDto.<T>builder()
				.code(errorCode.getCode())
				.message(errorCode.getMessage())
				.data(null)
				.build();
	}
}