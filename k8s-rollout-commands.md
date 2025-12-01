# Kubernetes Rollout 명령어 모음

## 기본 롤아웃 명령어

### 1. 배포 상태 확인
```bash
# Deployment 상태 확인
kubectl rollout status deployment/fast-cloud-caas -n caas

# 실시간으로 상태 확인 (watch 모드)
kubectl rollout status deployment/fast-cloud-caas -n caas --watch
```

### 2. 배포 히스토리 확인
```bash
# 배포 히스토리 조회
kubectl rollout history deployment/fast-cloud-caas -n caas

# 상세 히스토리 조회
kubectl rollout history deployment/fast-cloud-caas -n caas --revision=<revision-number>
```

### 3. 롤아웃 재시작 (이미지 업데이트 후)
```bash
# Deployment 재시작 (새 이미지로 롤아웃)
kubectl rollout restart deployment/fast-cloud-caas -n caas

# 또는 이미지 업데이트 후 자동 롤아웃
kubectl set image deployment/fast-cloud-caas fast-cloud-caas=mr8356/fast-cloud-caas:1.0 -n caas
```

### 4. 롤백
```bash
# 이전 버전으로 롤백
kubectl rollout undo deployment/fast-cloud-caas -n caas

# 특정 리비전으로 롤백
kubectl rollout undo deployment/fast-cloud-caas --to-revision=<revision-number> -n caas
```

### 5. 롤아웃 일시정지/재개
```bash
# 롤아웃 일시정지
kubectl rollout pause deployment/fast-cloud-caas -n caas

# 롤아웃 재개
kubectl rollout resume deployment/fast-cloud-caas -n caas
```

## 배포 관련 유용한 명령어

### Deployment 정보 확인
```bash
# Deployment 상세 정보
kubectl describe deployment/fast-cloud-caas -n caas

# Deployment 목록
kubectl get deployments -n caas

# Pod 상태 확인
kubectl get pods -n caas -l app=fast-cloud-caas

# Pod 로그 확인
kubectl logs -f deployment/fast-cloud-caas -n caas
```

### 이미지 업데이트 및 롤아웃 (전체 프로세스)
```bash
# 1. 새 이미지 빌드 및 푸시
./gradlew bootJar
docker build --platform linux/amd64 -t mr8356/fast-cloud-caas:1.1 .
docker push mr8356/fast-cloud-caas:1.1

# 2. Deployment 이미지 업데이트
kubectl set image deployment/fast-cloud-caas fast-cloud-caas=mr8356/fast-cloud-caas:1.1 -n caas

# 3. 롤아웃 상태 확인
kubectl rollout status deployment/fast-cloud-caas -n caas

# 4. 문제 발생 시 롤백
kubectl rollout undo deployment/fast-cloud-caas -n caas
```

## 빠른 참조

```bash
# 상태 확인
kubectl rollout status deployment/fast-cloud-caas -n caas

# 재시작
kubectl rollout restart deployment/fast-cloud-caas -n caas

# 롤백
kubectl rollout undo deployment/fast-cloud-caas -n caas

# 히스토리
kubectl rollout history deployment/fast-cloud-caas -n caas
```


