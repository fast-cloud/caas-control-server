## 변경 사항
Kubernetes 기반 Container as a Service (CaaS) 플랫폼의 핵심 기능 구현

## 변경 유형
- ✨ 새로운 기능 (Feature)

## 주요 구현 내용

### 1. Kubernetes 통합 설정
- Kubernetes Java Client API 설정 (`KubernetesConfig`)
- 환경변수 `KUBECONFIG` 우선 사용, 없을 경우 `~/.kube/config` 자동 탐지
- 클러스터 내부 실행 시 자동 감지

### 2. 컨테이너 생성 API 구현
- **엔드포인트**: `POST /container`
- Kubernetes Deployment, Service, Ingress 자동 생성
- 데이터베이스에 Application 및 Config 정보 저장
- 동적 호스트 이름 생성: `{clusterName}.{baseDomain}`

### 3. 컨테이너 목록 조회 API 구현
- **엔드포인트**: `GET /container?ownerUserId={userId}`
- 사용자별 컨테이너 목록 조회
- Kubernetes Deployment 상태 실시간 확인
- 컨테이너 통계 정보 제공 (전체, 실행 중, 클러스터 수)

### 4. 데이터베이스 엔티티 설계
- **Application 엔티티**: 애플리케이션 메타데이터 및 Kubernetes 리소스 정보 저장
- **Config 엔티티**: 컨테이너 설정 정보 저장 (이미지, 포트 등)
- JPA를 통한 영구 저장

### 5. Kubernetes 리소스 생성 로직
- **Deployment**: 컨테이너 애플리케이션 배포
- **Service**: ClusterIP 타입으로 내부 통신 제공
- **Ingress**: Nginx Ingress Controller를 통한 외부 접근 설정

### 6. Deployment 상태 조회 로직
- 실시간 Deployment 상태 확인 (RUNNING, PENDING, STOPPED, UNKNOWN)

## 주요 변경 파일

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
- `.github/ISSUE_TEMPLATE/` (이슈 템플릿)
- `.github/pull_request_template.md` (PR 템플릿)

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

## 테스트 방법
1. Kubernetes 클러스터에 연결 (kubeconfig 설정)
2. `POST /container` API로 컨테이너 생성
3. `GET /container?ownerUserId=system` API로 컨테이너 목록 조회
4. Kubernetes에서 생성된 리소스 확인:
   ```bash
   kubectl get deployment,service,ingress -n default
   ```

## 알려진 제한사항
1. 현재는 `default` 네임스페이스만 사용
2. 소유자 사용자 ID는 기본값 `system`으로 고정
3. Deployment replica는 1로 고정
4. Ingress Controller는 nginx로 고정
5. 컨테이너 삭제/업데이트 기능 미구현
6. 컨테이너 로그 조회 기능 미구현

## 체크리스트
- [x] 코드가 프로젝트의 코딩 스타일을 따릅니다
- [x] 자체적으로 코드 리뷰를 수행했습니다
- [x] 코드에 적절한 주석을 추가했습니다
- [x] 관련 문서를 업데이트했습니다 (WORK_LOG.md, 이슈 템플릿)
- [x] 로컬에서 빌드가 성공적으로 완료됩니다
- [x] Kubernetes 리소스 생성 로직이 구현되었습니다


