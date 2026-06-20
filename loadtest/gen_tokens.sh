#!/bin/bash
# 批量造测试用户并获取登录 token。
# 验证码登录"免注册"：用户不存在自动创建；sendCode 在 demo 模式直接返回验证码明文。
# 用法：bash loadtest/gen_tokens.sh [数量，默认200]
set -e
BASE=${BASE:-http://localhost:8080}
N=${1:-200}
OUT="$(dirname "$0")/tokens.txt"
: > "$OUT"

for i in $(seq 1 "$N"); do
  phone=$(printf "138%08d" "$i")
  code=$(curl -s -X POST "$BASE/user/code?phone=$phone" | python3 -c "import sys,json;print(json.load(sys.stdin)['data'])")
  token=$(curl -s -X POST "$BASE/user/login" -H "Content-Type: application/json" \
            -d "{\"phone\":\"$phone\",\"code\":\"$code\"}" | python3 -c "import sys,json;print(json.load(sys.stdin)['data'])")
  echo "$token" >> "$OUT"
done

echo "Generated $(wc -l < "$OUT" | tr -d ' ') tokens -> $OUT"
