#!/bin/bash

# MySQL Public Key Retrieval 문제 해결 스크립트

echo "MySQL Pod 찾는 중..."
MYSQL_POD=$(kubectl get pods -n default -l app=mysql-caas -o jsonpath='{.items[0].metadata.name}')

if [ -z "$MYSQL_POD" ]; then
    echo "MySQL Pod를 찾을 수 없습니다."
    exit 1
fi

echo "MySQL Pod: $MYSQL_POD"
echo "사용자 인증 방식을 mysql_native_password로 변경 중..."

kubectl exec -it "$MYSQL_POD" -n default -- mysql -u root -p1234 <<EOF
-- 기존 사용자 삭제 (있는 경우)
DROP USER IF EXISTS 'admin'@'%';

-- 새 사용자 생성 (mysql_native_password 인증 방식 사용)
CREATE USER 'admin'@'%' IDENTIFIED WITH mysql_native_password BY '1234';

-- 권한 부여
GRANT ALL PRIVILEGES ON caas.* TO 'admin'@'%';

-- 변경사항 적용
FLUSH PRIVILEGES;

-- 확인
SELECT user, host, plugin FROM mysql.user WHERE user='admin';
EOF

echo ""
echo "완료! 이제 애플리케이션을 재시작하세요:"
echo "kubectl rollout restart deployment/fast-cloud-caas -n caas"


