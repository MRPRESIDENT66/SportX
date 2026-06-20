-- 并发报名压测脚本：每个请求轮流使用不同用户的 token，模拟多用户抢同一挑战的名额。
-- 用法：wrk -t8 -c200 -d30s -s loadtest/register.lua http://localhost:8080
-- 注意：在项目根目录运行，使 tokens.txt 相对路径可达。

local tokens = {}
for line in io.lines("loadtest/tokens.txt") do
  if #line > 0 then
    tokens[#tokens + 1] = line
  end
end

local counter = 0

request = function()
  counter = counter + 1
  -- 轮流取 token，让请求分散到不同用户（同一用户对同一挑战只能成功报名一次）
  wrk.headers["authorization"] = tokens[(counter % #tokens) + 1]
  return wrk.format("POST", "/challenge/register/9999")
end
