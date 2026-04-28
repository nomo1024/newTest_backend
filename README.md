# 后端项目 README


## 项目简介
项目基于 Java/Maven（含源码在 `src/`）。提供 Dockerfile 以便容器化部署。`sql/` 目录保存了数据库相关脚本。

## 特性
- Java + Maven 项目结构
- 提供 Dockerfile，支持容器化运行

## 快速开始（本地）
1. 克隆或获取代码到本地
2. 在项目根目录执行：
   - Windows: `mvnw.cmd clean package`
   - macOS / Linux: `./mvnw clean package`
3. 运行生成的 jar（示例，路径以实际 target 下产物为准）:
   `java -jar target\your-app.jar`


## Docker
构建镜像：
```
docker build -t backend-app:latest .
```
运行容器
```
docker run -p 8080:8080 --env-file .env backend-app:latest
```
根据项目实际端口与环境变量调整命令。

## 数据库
- 数据库脚本位于 `sql/` 目录.
- driver-class-name: com.mysql.cj.jdbc.Driver

## 项目目录（概要）
- src/         - 源代码
- sql/         - 数据库脚本
- pom.xml      - Maven 配置
- Dockerfile   - 容器化配置

## 开发与贡献

