# Kubernetes API 클라이언트 연동 설정 가이드

## 개요

`KubernetesConfig.java`는 두 가지 방식으로 Kubernetes API에 연결합니다:

1. **로컬 실행**: `~/.kube/config` 또는 `KUBECONFIG` 환경변수 사용
2. **Kubernetes Pod 내부 실행**: ServiceAccount를 통한 자동 인증

## 필요한 설정

### 1. Kubernetes Pod 내부에서 실행하는 경우 (권장)

애플리케이션이 Kubernetes Pod로 실행될 때는 **ServiceAccount와 RBAC 권한**이 필요합니다.

#### 설정 방법:

```bash
# RBAC 설정 적용
kubectl apply -f k8s-rbac-setup.yaml
```

#### Deployment에 ServiceAccount 지정:

Deployment YAML에서 `serviceAccountName`을 지정해야 합니다:

```yaml
spec:
  serviceAccountName: caas-service-account  # 이 부분 추가
  containers:
  - name: fast-cloud-caas
    ...
```

#### 필요한 권한:

- **default 네임스페이스**에서:
  - Deployment: 생성, 수정, 삭제, 조회
  - Service: 생성, 수정, 삭제, 조회
  - Ingress: 생성, 수정, 삭제, 조회
  - Pod: 조회 (상태 확인용)

### 2. 로컬에서 실행하는 경우

로컬에서 실행할 때는 kubeconfig 파일만 있으면 됩니다.

#### 설정 방법:

```bash
# kubeconfig 파일이 ~/.kube/config에 있는지 확인
ls ~/.kube/config

# 또는 KUBECONFIG 환경변수 설정
export KUBECONFIG=/path/to/your/kubeconfig
```

#### 필요한 권한:

로컬 kubeconfig의 사용자/서비스 계정도 위와 동일한 권한이 필요합니다.

## 확인 방법

### 1. ServiceAccount 확인

```bash
kubectl get serviceaccount caas-service-account -n caas
```

### 2. Role 및 RoleBinding 확인

```bash
kubectl get role caas-role -n default
kubectl get rolebinding caas-role-binding -n default
```

### 3. 권한 테스트

```bash
# 애플리케이션 Pod에서 권한 테스트
kubectl exec -it deployment/fast-cloud-caas -n caas -- \
  kubectl auth can-i create deployments --namespace=default

kubectl exec -it deployment/fast-cloud-caas -n caas -- \
  kubectl auth can-i create services --namespace=default

kubectl exec -it deployment/fast-cloud-caas -n caas -- \
  kubectl auth can-i create ingresses --namespace=default
```

## 문제 해결

### 에러: "Forbidden" 또는 "Unauthorized"

1. ServiceAccount가 Deployment에 지정되었는지 확인
2. Role과 RoleBinding이 올바른 네임스페이스에 생성되었는지 확인
3. Role의 verbs에 필요한 권한이 포함되어 있는지 확인

### 에러: "kubeconfig not found"

- Pod 내부에서 실행 중이면 정상입니다 (ServiceAccount 사용)
- 로컬에서 실행 중이면 `~/.kube/config` 파일이 있는지 확인

## 참고사항

- `ContainerService`는 **default 네임스페이스**에서 리소스를 생성합니다
- 애플리케이션은 **caas 네임스페이스**에서 실행됩니다
- 따라서 Role과 RoleBinding은 **default 네임스페이스**에 생성해야 합니다

