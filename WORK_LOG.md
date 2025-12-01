# 작업 내역 (Work Log)

## 작업 일자
작업 수행 날짜

## 작업 개요
Kubernetes 기반 Container as a Service (CaaS) 플랫폼의 핵심 기능 구현

## 주요 구현 내용

### 1. Kubernetes 통합 설정
- **파일**: `src/main/java/caas/config/KubernetesConfig.java`
- **내용**:
  - Kubernetes Java Client API 설정
  - 환경변수 `KUBECONFIG` 우선 사용
  - 없을 경우 `~/.kube/config` 자동 탐지
  - 클러스터 내부 실행 시 자동 감지

### 2. 컨테이너 생성 API 구현
- **엔드포인트**: `POST /container`
- **Controller**: `ContainerController.createContainer()`
- **Service**: `ContainerService.createContainer()`
- **기능**:
  - Kubernetes Deployment 자동 생성
  - Kubernetes Service (ClusterIP) 자동 생성
  - Kubernetes Ingress 자동 생성 및 외부 접근 설정
  - 데이터베이스에 Application 및 Config 정보 저장
  - 컨테이너 ID 자동 생성 (UUID)
  - 동적 호스트 이름 생성: `{clusterName}.{baseDomain}`

#### 요청 DTO
- **파일**: `src/main/java/caas/dto/request/ContainerCreateRequestDto.java`
- **필드**:
  - `clusterName` (필수): 클러스터 이름
  - `imageLink` (필수): 컨테이너 이미지 링크
  - `externalPort` (필수): 외부 포트
  - `internalPort` (필수): 내부 포트

#### 응답 DTO
- **파일**: `src/main/java/caas/dto/response/ContainerCreateResponseDto.java`
- **필드**:
  - `containerId`: 생성된 컨테이너 ID
  - `clusterName`: 클러스터 이름
  - `imageLink`: 이미지 링크
  - `ports`: 포트 정보 (external, internal)
  - `requestTime`: 요청 시간
  - `status`: 상태 (PENDING)

### 3. 컨테이너 목록 조회 API 구현
- **엔드포인트**: `GET /container?ownerUserId={userId}`
- **Controller**: `ContainerController.getContainers()`
- **Service**: `ContainerService.getContainers()`
- **기능**:
  - 사용자별 컨테이너 목록 조회
  - Kubernetes Deployment 상태 실시간 확인
  - 컨테이너 통계 정보 제공

#### 응답 DTO
- **파일**: `src/main/java/caas/dto/response/ContainerListResponseDto.java`
- **구조**:
  - `summary`: 통계 정보
    - `totalContainers`: 전체 컨테이너 수
    - `runningContainers`: 실행 중인 컨테이너 수
    - `clusterCount`: 클러스터 수
  - `containers`: 컨테이너 정보 리스트
    - `containerId`: 컨테이너 ID
    - `clusterName`: 클러스터 이름
    - `status`: 상태 (RUNNING, PENDING, STOPPED, UNKNOWN)
    - `image`: 이미지 링크
    - `ports`: 포트 정보
    - `createdAt`: 생성 시간

### 4. 데이터베이스 엔티티 설계
#### Application 엔티티
- **파일**: `src/main/java/caas/entity/Application.java`
- **테이블**: `caas_application`
- **주요 필드**:
  - `appId`: 애플리케이션 고유 ID (PK)
  - `appName`: 애플리케이션 이름
  - `k8sNamespace`: Kubernetes 네임스페이스
  - `k8sDeploymentName`: Deployment 이름
  - `k8sServiceName`: Service 이름
  - `ownerUserId`: 소유자 사용자 ID
  - `cachedStatus`: 캐시된 상태
  - `createdAt`: 생성 시간
  - `configs`: Config 엔티티와의 OneToMany 관계

#### Config 엔티티
- **파일**: `src/main/java/caas/entity/Config.java`
- **테이블**: `caas_config`
- **주요 필드**:
  - `configId`: 설정 고유 ID (PK)
  - `application`: Application 엔티티와의 ManyToOne 관계
  - `imageLink`: 이미지 링크
  - `externalPort`: 외부 포트
  - `internalPort`: 내부 포트
  - `createdAt`: 생성 시간

### 5. Repository 구현
- **ApplicationRepository**: `src/main/java/caas/repositoty/ApplicationRepository.java`
  - `findByAppId()`: App ID로 조회
  - `findByOwnerUserId()`: 소유자 사용자 ID로 목록 조회

- **ConfigRepository**: `src/main/java/caas/repositoty/ConfigRepository.java`
  - `findByConfigId()`: Config ID로 조회

### 6. Kubernetes 리소스 생성 로직
#### Deployment 생성
- **메서드**: `ContainerService.createDeployment()`
- **설정**:
  - Replica: 1
  - Image Pull Policy: IfNotPresent
  - Container Port: 요청받은 internalPort

#### Service 생성
- **메서드**: `ContainerService.createService()`
- **설정**:
  - Type: ClusterIP
  - Port: internalPort
  - Target Port: internalPort

#### Ingress 생성
- **메서드**: `ContainerService.createIngress()`
- **설정**:
  - Ingress Class: nginx
  - Host: `{sanitizedClusterName}.{baseDomain}`
  - Path: `/`
  - Path Type: Prefix
  - SSL Redirect: false
  - Rewrite Target: `/`

### 7. Deployment 상태 조회 로직
- **메서드**: `ContainerService.getDeploymentStatus()`
- **상태 판단 기준**:
  - `RUNNING`: replicas > 0 && readyReplicas > 0 && availableReplicas > 0
  - `PENDING`: replicas > 0 && (readyReplicas == 0 || availableReplicas == 0)
  - `STOPPED`: replicas == 0 또는 Deployment가 존재하지 않음
  - `UNKNOWN`: 상태 정보를 확인할 수 없음

### 8. API 응답 구조 개선
- **파일**: `src/main/java/caas/dto/response/ApiResponseDto.java`
- **구조**:
  ```java
  {
    "code": 응답 코드,
    "message": 응답 메시지,
    "data": 응답 데이터
  }
  ```

### 9. 성공/에러 코드 정의
- **SuccessCode**: `src/main/java/caas/dto/response/SuccessCode.java`
  - `CONTAINER_CREATE_SUCCESS` (20002): 컨테이너 생성 성공
  - `CONTAINER_LIST_SUCCESS` (20003): 컨테이너 목록 조회 성공

- **ErrorCode**: `src/main/java/caas/dto/response/ErrorCode.java`
  - 다양한 HTTP 상태 코드별 에러 코드 정의

### 10. 애플리케이션 설정
- **파일**: `src/main/resources/application.properties`
- **설정 항목**:
  - `kubernetes.ingress.base-domain`: Ingress 기본 도메인 (기본값: fast-cloud.kro.kr)

## 변경된 파일 목록

### 새로 추가된 파일
- `src/main/java/caas/config/KubernetesConfig.java`
- `src/main/java/caas/controller/ContainerController.java`
- `src/main/java/caas/dto/response/ContainerListResponseDto.java`
- `src/main/java/caas/entity/Application.java`
- `src/main/java/caas/entity/Config.java`
- `src/main/java/caas/repositoty/ApplicationRepository.java`
- `src/main/java/caas/repositoty/ConfigRepository.java`
- `src/main/java/caas/service/ContainerService.java`
- `src/main/resources/application.properties`

### 수정된 파일
- `build.gradle`: Kubernetes Java Client 의존성 추가
- `src/main/java/caas/CaasApplication.java`
- `src/main/java/caas/dto/request/ContainerCreateRequestDto.java`
- `src/main/java/caas/dto/response/ApiResponseDto.java`
- `src/main/java/caas/dto/response/ContainerCreateResponseDto.java`
- `src/main/java/caas/dto/response/ErrorCode.java`
- `src/main/java/caas/dto/response/SuccessCode.java`

### 삭제된 파일
- `src/main/java/caas/entity/Container.java` (Application 엔티티로 대체)

## 기술 스택
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Kubernetes Client**: io.kubernetes:client-java:20.0.1
- **Database**: MySQL (JPA/Hibernate)
- **Build Tool**: Gradle

## 주요 특징
1. **자동화된 Kubernetes 리소스 생성**: Deployment, Service, Ingress를 자동으로 생성
2. **동적 Ingress 호스트 생성**: 클러스터 이름 기반으로 자동 호스트 이름 생성
3. **실시간 상태 조회**: Kubernetes API를 통한 실시간 Deployment 상태 확인
4. **데이터베이스 연동**: Application과 Config 정보를 데이터베이스에 영구 저장
5. **통계 정보 제공**: 컨테이너 목록 조회 시 통계 정보 포함

## 알려진 제한사항
1. 현재는 `default` 네임스페이스만 사용
2. 소유자 사용자 ID는 기본값 `system`으로 고정
3. Deployment replica는 1로 고정
4. Ingress Controller는 nginx로 고정
5. 컨테이너 삭제/업데이트 기능 미구현
6. 컨테이너 로그 조회 기능 미구현

## 다음 작업 예정
- [ ] 컨테이너 삭제 API 구현
- [ ] 컨테이너 업데이트 API 구현
- [ ] 컨테이너 로그 조회 API 구현
- [ ] 사용자 인증/인가 구현
- [ ] 리소스 제한 (CPU, Memory) 설정 기능
- [ ] 환경 변수 설정 기능
- [ ] 볼륨 마운트 기능

