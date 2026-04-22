# chatting-rag 前端

基于 React 18 + Vite 构建的 RAG 聊天界面，负责文档上传、对话交互和结果展示。

## 环境配置

前端通过 `.env.*` 文件管理不同环境的配置变量：

| 文件 | 说明 |
|------|------|
| `.env.development` | 本地开发环境 |
| `.env.staging` | 预发布环境 |
| `.env.production` | 生产环境 |

关键变量：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_API_BASE_URL` | 前端请求路径前缀（与 proxy 路径对应） | `/dev-api` |
| `VITE_API_TARGET` | Vite 代理目标地址 | `http://localhost:8001` |
| `VITE_DEV_SERVER_PORT` | 开发服务器端口 | `8000` |

Vite 会根据 `--mode` 参数加载对应环境文件，默认加载 `.env.development`。

## 开发

```bash
# 安装依赖（使用国内镜像）
make install

# 启动开发服务器（默认 .env.development，端口 8000）
make run
```

## 构建

```bash
# 预发布构建
make package ENV=staging

# 生产构建
make package ENV=production
```

构建产物输出至 `dist/` 目录。

## API 接口

前端通过 Vite proxy 代理请求至后端 `/api` 路径：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat` | POST | SSE 流式对话，接收 `{ references, meta, data, done }` 事件 |
| `/api/documents` | POST | 文档上传（MultipartFile，单文件最大 10MB） |

## 部署

```bash
# Docker
make docker.build ENV=dev
make docker.run ENV=dev

# Kubernetes（Helm）
make helm.upgrade ENV=dev
```
