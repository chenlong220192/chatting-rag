<div align="center">

# 🚀 RAG 对话系统

**基于 Spring Boot + React 的检索增强生成（RAG）智能对话平台**

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-green?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61dafb?style=flat-square&logo=react)](https://react.dev/)
[![ChromaDB](https://img.shields.io/badge/ChromaDB-Vector%20DB-ff6b6b?style=flat-square)](https://www.trychroma.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square)]()

**🎯 RAG 智能问答 | 📚 文档向量化检索 | 🔄 多模型可插拔 | 🐳 容器化部署**

[📖 在线文档](https://github.com/mingsha/chatting-rag) | [📚 文档导航](#-文档导航) | [✨ 功能特性](#-功能特性) | [🔧 技术栈](#-技术栈) | [🤝 贡献指南](#-贡献指南)

</div>

---

## 📋 目录

- [🎯 项目简介](#-项目简介)
- [✨ 功能特性](#-功能特性)
- [🔧 技术栈](#-技术栈)
- [📂 项目结构](#-项目结构)
- [🚀 快速开始](#-快速开始)
- [📦 制品结构](#-制品结构)
- [⚙️ 配置说明](#-配置说明)
- [📚 文档导航](#-文档导航)
- [🤝 贡献指南](#-贡献指南)
- [📄 许可证](#-许可证)

---

## 🎯 项目简介

**RAG 对话系统** 是一个基于 Spring Boot + React 的检索增强生成（Retrieval-Augmented Generation）智能对话平台。用户上传文档后，系统自动进行分块、向量化处理并存储至 ChromaDB；对话时通过向量检索召回相关片段，由大语言模型（LLM）生成精准回答。

### 🌟 核心优势

- **🎯 RAG 智能问答** — 基于向量检索的精准上下文召回，提升 LLM 回答质量
- **📚 多格式文档支持** — 支持常见文档格式，自动分块、向量化、索引存储
- **🔄 多模型可插拔** — LLM 与 Embedding 均支持 OpenAI / Ollama 灵活切换
- **⚡ 流式响应** — SSE 流式输出，实时返回 LLM 生成内容
- **🐳 容器化部署** — 支持 Docker 和 Kubernetes，一键部署
- **📊 可观测性** — 内置 Prometheus 指标采集， actuator 健康检查

---

## ✨ 功能特性

### 📚 文档处理
- **文档上传** — 支持多格式文档上传，单文件最大 10MB
- **智能分块** — 可配置分块大小与重叠长度，平衡召回精度与上下完整性
- **向量化存储** — 基于 ChromaDB 的向量索引，支持百万级文档片段检索

### 💬 智能对话
- **向量检索** — 基于语义相似度的 top-K 召回，灵活配置召回策略与最低分阈值
- **流式生成** — SSE 流式响应，实时展示 LLM 生成过程
- **上下文引用** — 返回检索来源片段，便于答案溯源与核实

### ⚙️ 系统管理
- **多环境隔离** — local / dev / test / prod 多套配置，热切换
- **敏感信息隔离** — LLM 凭证通过 `config/<profile>/application-llm.yml` 管理，不落地代码仓库
- **容器化部署** — Docker 镜像构建、Kubernetes Helm Chart 一键部署

---

## 🔧 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Java** | 21 | 基础运行环境 |
| **Spring Boot** | 4.0.5 | 应用框架 |
| **Spring AI** | 1.0.x | AI 集成框架 |
| **ChromaDB** | latest | 向量数据库 |
| **Maven** | 3.9+ | 构建工具 |
| **Prometheus** | - | 指标采集 |
| **Micrometer** | - | Spring Boot 指标桥接 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **React** | 18 | 前端框架 |
| **Vite** | - | 构建工具 |
| **Axios** | - | HTTP 客户端 |
| **Node.js** | 18+ | 开发环境 |

### 部署技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Docker** | 20+ | 容器化部署 |
| **Kubernetes** | 1.20+ | 容器编排 |
| **Helm** | 3.x | 包管理工具 |
| **Nginx** | 1.20+ | 反向代理 |

---

## 📂 项目结构

```
chatting-rag/
├── boot/                              Spring Boot 应用入口模块
├── app/
│   ├── web/                            REST 控制器层
│   │   ├── ChatController              对话接口 /api/chat
│   │   └── DocumentController          文档接口 /api/documents
│   ├── biz/                            业务逻辑层
│   │   ├── RAGService                  RAG 核心流程编排
│   │   ├── DocumentService             文档处理服务
│   │   └── ChromaService               ChromaDB 交互服务
│   ├── integration/                     外部服务客户端
│   │   ├── LlmClient                   LLM 调用客户端
│   │   ├── ChromaClient                ChromaDB 客户端
│   │   └── EmbeddingClient             Embedding 向量化客户端
│   └── test/                           单元测试
├── config/                             配置文件，按 profile 分目录管理
│   ├── local/                          local 环境配置
│   ├── dev/                            dev 环境配置
│   └── test/                           test 环境配置
├── deploy/                             部署脚本和 Helm Chart
│   ├── bin/                            Docker / Helm 运维脚本
│   ├── deploy/                         启动脚本（startup.sh、shutdown.sh）
│   └── helm/                           Kubernetes Helm Chart
├── ui/                                 React 前端（独立构建）
├── pom.xml                             父 POM
├── Makefile                            后端构建入口
└── CLAUDE.md                           Claude Code 开发指南
```

依赖层级：`boot` → `web` → `biz` → `integration`

---

## 🚀 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- Maven 3.9+
- ChromaDB 服务（可使用 Docker 独立部署）

### 后端

```bash
# 清理构建文件
make clean

# 运行单元测试
make test

# 构建（指定环境 profile）
make package ENV=local
make package ENV=dev

# 解压构建产物
make package.uncompress

# 启动（解压后）
target/app/bin/startup.sh

# 停止
target/app/bin/shutdown.sh
```

### 前端

```bash
cd ui

# 安装依赖
make install

# 启动开发服务器（默认 .env.development，端口 8000）
make run

# 构建
make package ENV=dev
make package ENV=prod
```

---

## 📦 制品结构

`make package.uncompress` 解压后，`target/app/` 目录结构如下：

```
target/app/
├── bin/                              运维脚本
│   ├── startup.sh                     启动脚本
│   ├── shutdown.sh                    停止脚本
│   └── restart.sh                    重启脚本
├── boot/                             应用 JAR 包
│   └── chatting-rag-boot-*.jar
├── lib/                              依赖 JAR 包
├── conf/                             配置文件（来自 config/<env>/）
├── logs/                             运行日志
├── gc/                               GC 日志（gc.%t.log）
└── dump/                             堆内存 dump 文件
```

---

## ⚙️ 配置说明

应用默认端口 **8001**，敏感信息通过 profile 配置文件管理：

1. 复制 `config/application-llm.yml.example` → `config/<profile>/application-llm.yml`
2. 填入真实的 LLM 凭证（provider、base-url、api-key、model 等）

> ⚠️ `config/<profile>/application-llm.yml` 已在 `.gitignore` 中，**请勿提交到 git**。

后端完整配置由 Spring Profile 分组管理，详见 `config/<profile>/`。

---

## 📚 RAG 流程

```
用户提问
    ↓
Embedding 模型向量化 query
    ↓
ChromaDB 相似度检索（top-K）
    ↓
召回片段 + 原问题 → 构建 prompt
    ↓
LLM 流式生成（Server-Sent Events）
```

---

## 📚 文档导航

| 文档 | 说明 |
|------|------|
| **README.md** | 项目整体说明 |
| **CLAUDE.md** | Claude Code 开发指南 |
| **ui/README.md** | 前端项目说明 |

---

## 🤝 贡献指南

欢迎所有形式的贡献，包括但不限于：

- 🐛 **Bug 报告**
- 💡 **功能建议**
- 📝 **文档改进**
- 🔧 **代码贡献**

### 贡献流程

1. **Fork 项目**
2. **创建功能分支**: `git checkout -b feature/your-feature`
3. **提交更改**: `git commit -m 'feat: add your feature'`
4. **推送分支**: `git push origin feature/your-feature`
5. **创建 Pull Request**

---

## 📞 联系方式

- **项目地址**: [GitHub](https://github.com/mingsha/chatting-rag)
- **问题反馈**: [Issues](https://github.com/mingsha/chatting-rag/issues)
- **讨论交流**: [Discussions](https://github.com/mingsha/chatting-rag/discussions)

---

## 📄 许可证

本项目采用 [MIT License](./LICENSE) 许可证。

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个 Star！**

**🚀 欢迎一起构建更智能的 RAG 对话系统！**

</div>
