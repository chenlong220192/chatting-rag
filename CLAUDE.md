# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**chatting-rag** is a RAG (Retrieval-Augmented Generation) chat system. Users upload documents, they are chunked and indexed into ChromaDB, and chat queries retrieve relevant chunks to augment LLM responses.

- **Backend**: Java 21 + Spring Boot 4.0.5 (multi-module Maven project)
- **Frontend**: React 18 + Vite (separate, in `ui/`)
- **Vector DB**: ChromaDB
- **LLM/Embedding**: Configurable providers (OpenAI, Ollama)

## Build & Run Commands

```bash
# Java backend
./mvnw clean test                           # run unit tests
./mvnw clean package -P local               # build for local profile
./mvnw clean package -P dev                 # build for dev profile
make package ENV=dev                         # build via Makefile, patches deploy scripts
make package.uncompress                      # extract tar.gz to target/app/

# Docker
make docker.run ENV=dev                      # build + run container
make docker.stop ENV=dev                     # stop container

# Helm (Kubernetes)
make helm.upgrade ENV=test                   # build, push, and deploy to k8s

# Frontend (run from ui/ directory)
cd ui && npm install && npm run dev          # dev server (default port 8000, .env.development)
cd ui && npm run build -- --mode production  # production build
```

**Profiles**: `local`, `dev`, `test`, `prod` — selected via `-P` flag or `ENV=` in Makefile. Each profile has its own config under `config/<profile>/`.

## Architecture

```
boot/            → Spring Boot entry point (site.mingsha.chatting.rag)
app/web/         → REST controllers (ChatController /api/chat, DocumentController /api/documents)
app/biz/         → Business logic (RAGService, DocumentService, ChromaService)
app/integration/ → External clients (LlmClient, ChromaClient, EmbeddingClient)
app/test/        → Unit tests
ui/              → React frontend (Vite, separate from Spring)
```

**Dependency chain**: `boot` → `web` → `biz` → `integration`

**RAG flow**: User query → embed → ChromaDB similarity search → build prompt with top-K chunks → LLM streaming response (SSE).

## Configuration

Application runs on **port 8001**. Sensitive values are injected via environment variables (see `.env.example`). Spring Profile YAMLs reference them as `${ENV_VAR}`:

| File | Purpose |
|------|---------|
| `config/app.properties` | App name, port, version, Docker/Helm metadata |
| `config/local/application.yml` | Imports all profile-specific YAMLs |
| `config/local/application-llm.yml` | LLM provider, base-url, api-key, model |
| `config/local/application-chroma.yml` | ChromaDB service URL |
| `config/local/application-embedding.yml` | Embedding service URL and model |
| `config/local/application-rag.yml` | RAG params: `top-k`, `min-score`, `chunk.size`, `chunk.overlap` |

## Key Technical Notes

- **SSE streaming**: `/api/chat` returns Server-Sent Events for real-time LLM responses.
- **Prometheus metrics**: Micrometer + actuator enabled; metrics at `/actuator/prometheus`.
- **Docker base image**: `mingsha/jdk:dragonwell-21-alpine-3.23`.
- **K8s init containers**: Wait for `ollama` and `chromadb` services before starting the app.
