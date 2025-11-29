package caas.controller;

import caas.dto.request.ContainerCreateRequestDto;
import caas.dto.response.ApiResponseDto;
import caas.dto.response.ContainerCreateResponseDto;
import caas.dto.response.ContainerListResponseDto;
import caas.dto.response.SuccessCode;
import caas.service.ContainerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/container")
@RequiredArgsConstructor
public class ContainerController {

	private final ContainerService containerService;

	@PostMapping
	public ApiResponseDto<ContainerCreateResponseDto> createContainer(
			@Valid @RequestBody ContainerCreateRequestDto request) {
		ContainerCreateResponseDto response = containerService.createContainer(request);
		return ApiResponseDto.success(SuccessCode.CONTAINER_CREATE_SUCCESS, response);
	}

	@GetMapping
	public ApiResponseDto<ContainerListResponseDto> getContainers() {
		ContainerListResponseDto response = containerService.getContainers();
		return ApiResponseDto.success(SuccessCode.CONTAINER_LIST_SUCCESS, response);
	}
    
}
