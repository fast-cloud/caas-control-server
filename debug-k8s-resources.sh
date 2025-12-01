#!/bin/bash

echo "=== Kubernetes 리소스 확인 ==="
echo ""

echo "1. default 네임스페이스의 모든 Deployment:"
kubectl get deployments -n default
echo ""

echo "2. default 네임스페이스의 모든 Service:"
kubectl get svc -n default
echo ""

echo "3. default 네임스페이스의 모든 Ingress:"
kubectl get ingress -n default
echo ""

echo "4. default 네임스페이스의 모든 Pod:"
kubectl get pods -n default
echo ""

echo "5. my-web-cluster 관련 리소스 검색:"
kubectl get all -n default | grep my-web-cluster
echo ""

echo "6. 애플리케이션 Pod 로그에서 에러 확인:"
kubectl logs -n caas deployment/fast-cloud-caas --tail=50 | grep -i error
echo ""

echo "7. RBAC 권한 확인:"
kubectl auth can-i create deployments --namespace=default --as=system:serviceaccount:caas:caas-service-account
kubectl auth can-i create services --namespace=default --as=system:serviceaccount:caas:caas-service-account
kubectl auth can-i create ingresses --namespace=default --as=system:serviceaccount:caas:caas-service-account
echo ""

echo "8. 모든 네임스페이스에서 my-web-cluster 검색:"
kubectl get all --all-namespaces | grep my-web-cluster

