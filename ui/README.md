# chatting-rag 前端

基于 React 18 + TypeScript + Vite + Ant Design 5 构建的 RAG 聊天界面。

## 技术栈

- **React 18** — UI 框架
- **TypeScript** — 类型安全（严格模式）
- **Vite 5** — 构建工具
- **Ant Design 5** — UI 组件库
- **Zustand** — 状态管理
- **React Router 6** — 路由管理
- **Vitest** — 单元测试

## 目录结构

```
src/
├── main.tsx                    # 入口
├── App.tsx                     # 根组件（路由 + Ant Design ConfigProvider）
├── index.css                   # 全局样式
├── App.css                     # App 级样式
├── components/                 # 共享组件
│   └── common/
│       ├── ErrorBoundary.tsx   # 错误边界
│       └── LoadingSkeleton.tsx # 加载骨架屏
├── pages/
│   └── Chat/
│       ├── index.tsx           # 聊天页面
│       └── components/
│           ├── ChatInput.tsx   # 输入框
│           └── MessageItem.tsx # 单条消息
├── services/                   # API 层
│   ├── chatService.ts          # SSE 流式对话
│   └── documentService.ts       # 文档上传
├── stores/                     # Zustand 状态管理
│   └── chatStore.ts            # 对话状态
├── hooks/                      # 自定义 Hooks
│   └── useScrollToBottom.ts     # 滚动到底部
├── types/                      # TypeScript 类型定义
│   └── chat.ts                 # ChatMessage, Reference, ChatMeta
└── utils/                      # 工具函数
    └── apiClient.ts            # API 基础配置
```

## 环境配置

```bash
# .env.development
VITE_API_BASE_URL=/dev-api
VITE_API_TARGET=http://localhost:8001
VITE_DEV_SERVER_PORT=8000
```

## 开发

```bash
# 安装依赖
make install

# 启动开发服务器（端口 8000）
make run

# 类型检查 & 代码规范
npm run lint

# 格式化代码
npm run format

# 运行测试
npm run test
npm run test:watch  # 监听模式
```

## 构建

```bash
# 开发构建
make package ENV=development

# 预发布构建
make package ENV=staging

# 生产构建
make package ENV=production
```

构建产物输出至 `dist/` 目录。

## 环境变量模板

参考 `.env.example` 创建各环境的配置文件。

| 文件 | 说明 |
|------|------|
| `.env.development` | 本地开发（proxy 到 localhost:8001）|
| `.env.staging` | 预发布（VITE_API_TARGET 留空）|
| `.env.production` | 生产（proxy 到 k8s 后端服务）|

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat` | POST | SSE 流式对话 |
| `/api/documents` | POST | 文档上传（MultipartFile，单文件最大 10MB）|

## 部署

## 部署

```bash
# Docker
make docker.build ENV=production
make docker.run ENV=production

# Kubernetes（Helm）
make helm.upgrade ENV=production
```

## 代码规范

- **TypeScript 严格模式**：禁止 `any`，禁止未检查的索引访问
- **ESLint**：使用 `@typescript-eslint` 插件，`no-explicit-any: error`
- **Prettier**：单引号、分号、行宽 100
- **提交前检查**：`npm run lint && npm run test`
