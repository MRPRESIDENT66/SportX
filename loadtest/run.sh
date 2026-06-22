#!/bin/bash
# 一键并发报名压测：重置 → 造token → sanity → wrk → 验证正确性。
# 用法：在项目根目录执行  bash loadtest/run.sh
set -e
cd "$(dirname "$0")/.."   # 切到项目根，保证 register.lua 的相对路径可达

MYSQL="/usr/local/mysql/bin/mysql --default-character-set=utf8mb4 -u root -p12345678 SportX"
RESET="DELETE FROM challenge_participation WHERE challenge_id=9999; UPDATE challenge SET joined_slots=0 WHERE id=9999;"

echo "===== ① 重置挑战 9999（名额100，已报0）====="
$MYSQL -e "$RESET" 2>/dev/null

echo "===== ② 造 200 个用户 token（现造现用，避免30min TTL过期）====="
bash loadtest/gen_tokens.sh 200

echo "===== ③ sanity check：单次报名应成功 ====="
T1=$(head -1 loadtest/tokens.txt)
echo "  响应: $(curl -s -X POST http://localhost:8080/challenge/register/9999 -H "authorization: $T1")"
$MYSQL -e "$RESET" 2>/dev/null   # 清掉 sanity 占用，回到 0

echo "===== ④ 正式压测：200并发抢100名额，30秒 ====="
wrk -t8 -c200 -d30s -s loadtest/register.lua http://localhost:8080

echo "===== ⑤ 验证正确性（两个数字都应=100，且相等=零超卖）====="
/usr/local/mysql/bin/mysql -u root -p12345678 SportX -e "
SELECT
  (SELECT joined_slots FROM challenge WHERE id=9999) AS joined_slots,
  (SELECT COUNT(*) FROM challenge_participation WHERE challenge_id=9999) AS participation_rows;
" 2>/dev/null
