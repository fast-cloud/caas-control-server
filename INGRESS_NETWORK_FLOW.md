# Kubernetes Ingress 네트워크 흐름 설명

## 시나리오
- **Kubernetes 컨트롤러 노드**: `200.01.10.1` (IP), `fast-cloud.kro.kr` (도메인)
- **사용자 요청**: `http://my-web-cluster.fast-cloud.kro.kr` (실제 도메인)
- **목표**: 특정 Pod의 특정 포트로 트래픽 전달

## 전체 네트워크 흐름

```
[외부 사용자]
    ↓ HTTP 요청: http://my-web-cluster.local
    ↓ DNS 조회 → Ingress Controller IP로 해석
[Ingress Controller] (200.01.10.1:80 또는 NodePort)
    ↓ Ingress 규칙 확인 → Service로 라우팅
[Service] (ClusterIP: 10.96.x.x:80)
    ↓ Service Selector로 Pod 선택
[Pod] (10.244.x.x:80 - containerPort)
    ↓ 애플리케이션 처리
[응답 반환]
```

## 단계별 상세 설명

### 1단계: 외부 사용자 요청
```
사용자 브라우저: http://my-web-cluster.local
```

**DNS 설정 필요:**
- `/etc/hosts` 또는 DNS 서버에 설정
  ```
  200.01.10.1  my-web-cluster.local
  ```
- 또는 실제 도메인 사용 시:
  ```
  my-web-cluster.fast-cloud.kro.kr  →  200.01.10.1
  ```

### 2단계: Ingress Controller가 요청 수신

**Ingress Controller Service 확인:**
```bash
kubectl get svc -n ingress-nginx
```

**예시 출력:**
```
NAME            TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)
ingress-nginx   NodePort    10.96.1.100   200.01.10.1   80:30080/TCP
```

**트래픽 흐름:**
- 외부 IP: `200.01.10.1:80` (또는 NodePort: `200.01.10.1:30080`)
- Ingress Controller Pod가 이 포트에서 리스닝
- Ingress Controller는 모든 Ingress 리소스를 모니터링

### 3단계: Ingress 규칙 매칭

**생성된 Ingress 리소스:**
```yaml
spec:
  rules:
  - host: my-web-cluster.local
    http:
      paths:
      - path: /
        backend:
          service:
            name: my-web-cluster-abc12345-svc
            port:
              number: 80
```

**Ingress Controller 동작:**
1. `Host` 헤더 확인: `my-web-cluster.local`
2. 매칭되는 Ingress 규칙 찾기
3. 백엔드 Service 확인: `my-web-cluster-abc12345-svc:80`

### 4단계: Service로 트래픽 전달

**Service 확인:**
```bash
kubectl get svc my-web-cluster-abc12345-svc
```

**예시 출력:**
```
NAME                        TYPE        CLUSTER-IP    PORT(S)
my-web-cluster-abc12345-svc ClusterIP   10.96.2.50   80/TCP
```

**트래픽 흐름:**
- Ingress Controller → Service ClusterIP: `10.96.2.50:80`
- Service는 가상 IP (실제 Pod로 프록시)

### 5단계: Pod 선택 및 트래픽 전달

**Service Selector:**
```yaml
spec:
  selector:
    app: my-web-cluster-abc12345
```

**매칭되는 Pod:**
```bash
kubectl get pods -l app=my-web-cluster-abc12345
```

**예시 출력:**
```
NAME                                    READY   STATUS    IP
my-web-cluster-abc12345-xxxxx-xxxxx    1/1     Running   10.244.1.5
```

**트래픽 흐름:**
- Service → Pod IP: `10.244.1.5:80` (containerPort)
- kube-proxy가 iptables/IPVS로 라우팅

### 6단계: Pod 내부 애플리케이션 처리

**Pod 내부:**
- 컨테이너가 `containerPort: 80`에서 리스닝
- 애플리케이션 처리 후 응답 반환

## 실제 IP/포트 매핑 예시

```
외부 요청: 200.01.10.1:80 (Ingress Controller)
    ↓
Ingress Controller Pod: 10.244.0.10:80
    ↓
Service ClusterIP: 10.96.2.50:80
    ↓
Pod IP: 10.244.1.5:80 (containerPort)
```

## 주의사항

1. **Ingress Controller Service 타입:**
   - `NodePort`: 모든 노드의 특정 포트로 접근 가능
   - `LoadBalancer`: 클라우드 제공자의 로드밸런서 사용
   - `ClusterIP`: 클러스터 내부만 접근 가능 (외부 접근 불가)

2. **포트 범위:**
   - NodePort: 30000-32767
   - Service: 임의의 포트 (보통 80, 443 등)
   - Pod containerPort: 애플리케이션이 리스닝하는 포트

3. **DNS 설정:**
   - 로컬 테스트: `/etc/hosts` 파일 수정
   - 프로덕션: 실제 DNS 서버에 A 레코드 추가

