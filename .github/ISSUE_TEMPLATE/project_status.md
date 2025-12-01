# Fast Cloud CaaS 프로젝트 현재 상태

## 프로젝트 개요
Kubernetes 기반 Container as a Service (CaaS) 플랫폼으로, Spring Boot를 사용하여 컨테이너 생명주기를 관리합니다.

## 기술 스택
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Kubernetes Client**: io.kubernetes:client-java:20.0.1
- **Database**: MySQL (JPA/Hibernate)
- **Build Tool**: Gradle

## 구현된 기능

### 1. 컨테이너 생성 API
- **엔드포인트**: `POST /container`
- **기능**: 
  - Kubernetes Deployment, Service, Ingress 자동 생성
  - 데이터베이스에 Application 및 Config 정보 저장
  - 컨테이너 ID 자동 생성 (UUID)
  - Ingress를 통한 외부 접근 설정
- **요청 파라미터**:
  - `clusterName`: 클러스터 이름 (필수)
  - `imageLink`: 컨테이너 이미지 링크 (필수)
  - `externalPort`: 외부 포트 (필수)
  - `internalPort`: 내부 포트 (필수)

### 2. 컨테이너 목록 조회 API
- **엔드포인트**: `GET /container?ownerUserId={userId}`
- **기능**:
  - 사용자별 컨테이너 목록 조회
  - Kubernetes Deployment 상태 실시간 확인
  - 컨테이너 통계 정보 제공 (전체, 실행 중, 클러스터 수)
- **응답 정보**:
  - 컨테이너 ID, 클러스터 이름, 상태, 이미지, 포트 정보, 생성 시간

### 3. Kubernetes 리소스 관리
- **Deployment**: 컨테이너 애플리케이션 배포
- **Service**: ClusterIP 타입으로 내부 통신 제공
- **Ingress**: Nginx Ingress Controller를 통한 외부 접근
  - 동적 호스트 이름 생성: `{clusterName}.{baseDomain}`
  - SSL 리다이렉트 비활성화 (개발 환경)

### 4. 데이터베이스 스키마
- **Application 엔티티** (`caas_application`):
  - `app_id`: 애플리케이션 고유 ID
  - `app_name`: 애플리케이션 이름
  - `k8s_namespace`: Kubernetes 네임스페이스
  - `k8s_deployment_name`: Deployment 이름
  - `k8s_service_name`: Service 이름
  - `owner_user_id`: 소유자 사용자 ID
  - `cached_status`: 캐시된 상태
  - `created_at`: 생성 시간

- **Config 엔티티** (`caas_config`):
  - `config_id`: 설정 고유 ID
  - `app_id`: 애플리케이션 ID (외래키)
  - `image_link`: 이미지 링크
  - `external_port`: 외부 포트
  - `internal_port`: 내부 포트
  - `created_at`: 생성 시간

## 아키텍처

### 계층 구조
```
Controller (ContainerController)
    ↓
Service (ContainerService)
    ↓
Repository (ApplicationRepository, ConfigRepository)
    ↓
Entity (Application, Config)
```

### Kubernetes 통합
- `KubernetesConfig`: Kubernetes API 클라이언트 설정
  - 환경변수 `KUBECONFIG` 우선 사용
  - 없을 경우 `~/.kube/config` 사용
  - 클러스터 내부 실행 시 자동 감지

## API 응답 구조

### 성공 응답
```json
{
  "code": 20002,
  "message": "컨테이너 생성 요청이 성공적으로 접수되었습니다.",
  "data": { ... }
}
```

### 에러 응답
```json
{
  "code": 40000,
  "message": "유효한 요청이 아닙니다.",
  "data": null
}
```

## 상태 코드
- `RUNNING`: Deployment가 정상 실행 중
- `PENDING`: Deployment가 생성 중이거나 준비 중
- `STOPPED`: Deployment가 중지되었거나 존재하지 않음
- `UNKNOWN`: 상태를 확인할 수 없음

## 설정 파일
- `application.properties`: 애플리케이션 설정
  - `kubernetes.ingress.base-domain`: Ingress 기본 도메인

## 알려진 제한사항
1. 현재는 `default` 네임스페이스만 사용
2. 소유자 사용자 ID는 기본값 `system`으로 고정
3. Deployment replica는 1로 고정
4. Ingress Controller는 nginx로 고정
5. 컨테이너 삭제 기능 미구현
6. 컨테이너 업데이트 기능 미구현
7. 컨테이너 로그 조회 기능 미구현

## 향후 개선 사항
- [ ] 컨테이너 삭제 API 구현
- [ ] 컨테이너 업데이트 API 구현
- [ ] 컨테이너 로그 조회 API 구현
- [ ] 컨테이너 상태 모니터링 개선
- [ ] 멀티 네임스페이스 지원
- [ ] 사용자 인증/인가 구현
- [ ] 리소스 제한 (CPU, Memory) 설정 기능
- [ ] 환경 변수 설정 기능
- [ ] 볼륨 마운트 기능
- [ ] 헬스체크 설정 기능

## 관련 문서
- `INGRESS_NETWORK_FLOW.md`: Ingress 네트워크 플로우 설명

