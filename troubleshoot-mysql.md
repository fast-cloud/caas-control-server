# MySQL 연결 문제 해결

## 문제 진단 명령어

```bash
# 1. MySQL Pod 상태 확인
kubectl get pods -n caas | grep mysql

# 2. MySQL Service 확인
kubectl get svc -n caas | grep mysql

# 3. MySQL Pod 로그 확인
kubectl logs -n caas deployment/mysql-caas

# 4. 애플리케이션 Pod에서 MySQL 연결 테스트
kubectl exec -it deployment/fast-cloud-caas -n caas -- nslookup mysql-caas

# 5. 네임스페이스 확인
kubectl get namespace caas
```

## 가능한 원인 및 해결 방법

### 1. MySQL이 아직 준비되지 않음
```bash
# MySQL Pod가 Running 상태인지 확인
kubectl get pods -n caas

# MySQL이 준비될 때까지 대기
kubectl wait --for=condition=ready pod -l app=mysql-caas -n caas --timeout=300s
```

### 2. MySQL Service가 생성되지 않음
```bash
# Service 확인
kubectl get svc mysql-caas -n caas

# Service가 없으면 생성
kubectl apply -f k8s/mysql-service.yaml
```

### 3. 네임스페이스 문제
애플리케이션과 MySQL이 같은 네임스페이스(`caas`)에 있어야 합니다.

### 4. 애플리케이션이 MySQL보다 먼저 시작됨
Deployment에 `initContainer`나 `dependsOn`을 추가하거나, 애플리케이션에 재시도 로직을 추가해야 합니다.


