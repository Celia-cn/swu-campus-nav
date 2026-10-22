# test-api.ps1
$baseUrl = "http://localhost:8091"

Write-Host "=== 开始测试后端 API ===" -ForegroundColor Green

# 测试 1：获取讲解点列表（公开接口）
Write-Host "`n[测试 1] 获取讲解点列表..." -ForegroundColor Cyan
try {
    $locations = Invoke-RestMethod -Uri "$baseUrl/api/locations" -Method GET
    Write-Host "? 成功！共 $($locations.data.Count) 个讲解点" -ForegroundColor Green
    $locations.data | ForEach-Object { Write-Host "  - $($_.name)" }
} catch {
    Write-Host "? 失败: $_" -ForegroundColor Red
}

# 测试 2：登录
Write-Host "`n[测试 2] 用户登录..." -ForegroundColor Cyan
try {
    $loginBody = @{
        username = "admin"
        password = "admin123456"
        client = "MOBILE"
    } | ConvertTo-Json

    $loginResult = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $token = $loginResult.data.token
    Write-Host "? 登录成功！Token: $($token.Substring(0, 20))..." -ForegroundColor Green
} catch {
    Write-Host "? 登录失败: $_" -ForegroundColor Red
    exit
}

# 设置认证头
$headers = @{
    "Authorization" = "Bearer $token"
}

# 测试 3：获取用户信息
Write-Host "`n[测试 3] 获取用户信息..." -ForegroundColor Cyan
try {
    $userInfo = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method GET -Headers $headers
    Write-Host "? 成功！用户名: $($userInfo.data.username), 角色: $($userInfo.data.role)" -ForegroundColor Green
} catch {
    Write-Host "? 失败: $_" -ForegroundColor Red
}

# 测试 4：获取用户统计
Write-Host "`n[测试 4] 获取用户统计..." -ForegroundColor Cyan
try {
    $stats = Invoke-RestMethod -Uri "$baseUrl/api/auth/me/stats" -Method GET -Headers $headers
    Write-Host "? 成功！" -ForegroundColor Green
    Write-Host "  签到次数: $($stats.data.checkInCount)"
    Write-Host "  播放次数: $($stats.data.playCount)"
} catch {
    Write-Host "? 失败: $_" -ForegroundColor Red
}

# 测试 5：管理员统计
Write-Host "`n[测试 5] 获取管理员统计..." -ForegroundColor Cyan
try {
    $adminStats = Invoke-RestMethod -Uri "$baseUrl/api/admin/stats" -Method GET -Headers $headers
    Write-Host "? 成功！" -ForegroundColor Green
    Write-Host "  总用户数: $($adminStats.data.totalUsers)"
    Write-Host "  总讲解点: $($adminStats.data.totalLocations)"
} catch {
    Write-Host "? 失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green