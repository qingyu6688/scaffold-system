[CmdletBinding()]
param(
    [switch]$Build,
    [switch]$ValidateOnly
)

$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
    throw '缺少本地配置，请将 scaffold-system/.env.example 复制为 .env 并填写连接信息。'
}

# 只解析键值，不执行配置内容，也不输出密码或签名密钥。
$lineNumber = 0
foreach ($line in Get-Content -LiteralPath $envFile -Encoding utf8) {
    $lineNumber++
    $trimmed = $line.Trim()
    if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) { continue }
    if ($trimmed -notmatch '^([A-Z_][A-Z0-9_]*)\s*=(.*)$') {
        throw "本地配置第 $lineNumber 行格式错误，应使用大写变量名=值。"
    }
    $key = $Matches[1]
    $value = $Matches[2].Trim()
    if ($value.Length -ge 2 -and (
        ($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))
    )) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($key, $value, 'Process')
}

foreach ($key in @('DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USERNAME', 'DB_PASSWORD', 'REDIS_HOST', 'REDIS_PORT', 'JWT_SECRET')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key, 'Process'))) {
        throw "本地配置缺少必要变量：$key。"
    }
}
if ([Text.Encoding]::UTF8.GetByteCount($env:JWT_SECRET) -lt 32) {
    throw 'JWT_SECRET 必须是至少32字节的随机签名密钥。'
}
if ($ValidateOnly) {
    Write-Host '本地配置格式检查通过；未连接数据库，也未启动后端。'
    return
}

Push-Location $PSScriptRoot
try {
    if ($Build) {
        # 真实开发库只用于运行服务；完整集成测试必须另配独立测试库。
        & mvn -pl scaffold-admin -am package '-DskipTests'
        if ($LASTEXITCODE -ne 0) { throw '后端构建失败，请检查 Maven 输出。' }
    }
    [xml]$pom = Get-Content -LiteralPath 'pom.xml' -Raw -Encoding utf8
    $jar = Join-Path $PSScriptRoot "scaffold-admin/target/scaffold-admin-$($pom.project.version).jar"
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
        throw '找不到后端构建产物，请添加 -Build 参数重新运行。'
    }
    Write-Host '已加载本地配置，正在启动后端。'
    & java -jar $jar
    if ($LASTEXITCODE -ne 0) { throw '后端启动或运行失败，请检查上方日志。' }
} finally {
    Pop-Location
}
