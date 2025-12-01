---
name: 컨테이너 관련 이슈
about: 컨테이너 생성, 조회, 관리와 관련된 이슈
title: '[CONTAINER] '
labels: container
assignees: ''
---

## 이슈 유형
- [ ] 컨테이너 생성 실패
- [ ] 컨테이너 목록 조회 오류
- [ ] Deployment 생성 실패
- [ ] Service 생성 실패
- [ ] Ingress 생성 실패
- [ ] 상태 조회 오류
- [ ] 기타

## 컨테이너 정보
- Container ID: 
- Cluster Name: 
- Image Link: 
- External Port: 
- Internal Port: 
- Owner User ID: 

## Kubernetes 리소스 정보
- Namespace: 
- Deployment Name: 
- Service Name: 
- Ingress Name: 

## 문제 설명
발생한 문제에 대한 상세한 설명을 작성해주세요.

## 재현 방법
1. 
2. 
3. 

## 예상 동작
예상했던 동작을 설명해주세요.

## 실제 동작
실제로 발생한 동작을 설명해주세요.

## Kubernetes 리소스 상태
관련 Kubernetes 리소스의 상태를 확인한 결과를 작성해주세요.

```bash
# kubectl 명령어 실행 결과를 여기에 붙여넣기
kubectl get deployment <deployment-name> -n <namespace>
kubectl get service <service-name> -n <namespace>
kubectl get ingress <ingress-name> -n <namespace>
```

## 로그
관련된 로그나 에러 메시지를 붙여넣어주세요.

```
로그 내용을 여기에 붙여넣기
```

## 환경 정보
- Kubernetes 클러스터 버전: 
- Ingress Controller: [예: nginx]
- Base Domain: 

## 추가 컨텍스트
이슈와 관련된 추가 정보를 여기에 추가해주세요.

