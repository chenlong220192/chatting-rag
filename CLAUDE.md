# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

**chatting-rag** is a RAG (Retrieval-Augmented Generation) chat system. Users upload documents, they are chunked and indexed into ChromaDB, and chat queries retrieve relevant chunks to augment LLM responses.

- **Backend**: Java 21 + Spring Boot 4.0.5 (multi-module Maven project)
- **Frontend**: React 18 + Vite (separate, in `ui/`)
- **Vector DB**: ChromaDB
- **LLM/Embedding**: Configurable providers (OpenAI, Ollama)
- **License**: MIT

---

## Git Worktree Development Workflow

This project uses **git worktrees** for parallel development. Each feature or bugfix runs in its own isolated directory with its own branch, allowing multiple tasks to be developed simultaneously without interfering with each other.

### Core Principle

> **Never develop on `main`. Always develop on a feature/bugfix branch inside a dedicated worktree.**

```
main worktree (main)          → production code, always clean and buildable
feature/xxx worktree          → feature development
bugfix/yyy worktree            → bug fix development
```

### Branch Naming Convention

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature/` | New feature development | `feature/rate-limiting` |
| `bugfix/` | Bug fix | `bugfix/document-delete` |
| `hotfix/` | Urgent production fix | `hotfix/cors-vulnerability` |
| `refactor/` | Code refactoring | `refactor/chroma-client` |

---

### Workflow: Start a New Task

**Step 1 — Sync main**

Always start from the latest `main`:

```bash
# In the main repository (chatting-rag/)
git checkout main
git pull origin main
```

**Step 2 — Create a worktree for the new branch**

```bash
# Syntax: git worktree add <path> <branch>
git worktree add ../chatting-rag-feature-xxx -b feature/xxx
```

This simultaneously:
- Creates a new directory `../chatting-rag-feature-xxx/`
- Checks out a new branch `feature/xxx` based on `main`
- Attaches the worktree to the main repository

**Step 3 — Develop**

```bash
cd ../chatting-rag-feature-xxx

# Backend — in the worktree directory
make package ENV=dev
target/app/bin/startup.sh

# Frontend — in the ui/ subdirectory
cd ui && make run
```

**Step 4 — Commit and push**

```bash
cd ../chatting-rag-feature-xxx
git add .
git commit -m "feat: add rate limiting for chat endpoint"

# Push to remote to create PR
git push -u origin feature/xxx
```

**Step 5 — Create PR on GitHub**

Create a Pull Request on GitHub. Assign reviewers, address feedback, and merge once approved.

**Step 6 — Cleanup after merge**

```bash
# Back in the main repository
git checkout main
git pull origin main

# Remove the merged worktree directory
git worktree remove ../chatting-rag-feature-xxx

# Prune stale remote tracking branches
git fetch --prune origin
git branch -d feature/xxx          # delete local branch reference
git push origin --delete feature/xxx  # delete remote branch
```

---

### Workflow: Switch Between Tasks

You can have multiple worktrees simultaneously:

```bash
# List all active worktrees
git worktree list

# Example output:
# /data/repos/mingsha/chatting-rag          main      (repository)
# /data/repos/mingsha/chatting-rag-feature-xxx  feature/xxx  ...
# /data/repos/mingsha/chatting-rag-bugfix-yyy  bugfix/yyy  ...

# Switch to a different task
cd ../chatting-rag-bugfix-yyy
# ... do work, commit, push
```

---

### Workflow: Handle a Bug on `main` While Mid-Task

If you're developing a feature and a critical bug is reported:

```bash
# Stash current work (don't commit half-done changes)
git stash

# Create a hotfix worktree
git worktree add ../chatting-rag-hotfix-urgent -b hotfix/urgent-bug

# ... fix the bug, commit, push, merge ...

# Back to your feature
git checkout feature/xxx
git stash pop
```

---

### Workflow: Rebase onto Latest `main`

If `main` has moved forward while you're developing:

```bash
cd ../chatting-rag-feature-xxx

# Rebase your feature onto latest main
git fetch origin main
git rebase origin/main

# If conflicts occur, resolve them and continue:
git rebase --continue
```

---

## Build & Run Commands

```bash
# Java backend
make clean                              # clean build artifacts
make test                               # run unit tests
make package ENV=local                  # build for local profile
make package ENV=dev                    # build for dev profile
make package.uncompress                 # extract tar.gz to target/app/

# Run (use the startup script, NOT java -jar directly)
target/app/bin/startup.sh
target/app/bin/shutdown.sh

# Docker
make docker.run ENV=dev                # build + run container
make docker.stop ENV=dev              # stop container

# Helm (Kubernetes)
make helm.upgrade ENV=test             # build, push, and deploy to k8s

# Frontend (run from ui/ directory)
cd ui && make install                  # install dependencies
cd ui && make run                      # dev server (port 8000)
cd ui && make package ENV=dev          # build dev artifact
```

**Profiles**: `local`, `dev`, `test`, `prod` — selected via `-P` flag or `ENV=` in Makefile. Each profile has its own config under `config/<profile>/`.

---

## Architecture

```
boot/             → Spring Boot entry point (site.mingsha.chatting.rag)
app/web/          → REST controllers (ChatController /api/chat, DocumentController /api/documents)
app/biz/          → Business logic (RAGService, DocumentService, ChromaService)
app/integration/   → External clients (LlmClient, ChromaClient, EmbeddingClient)
app/test/         → Unit tests
ui/               → React frontend (Vite, separate from Spring)
```

**Dependency chain**: `boot` → `web` → `biz` → `integration`

**RAG flow**: User query → embed → ChromaDB similarity search → build prompt with top-K chunks → LLM streaming response (SSE).

---

## Configuration

Application runs on **port 8001**. Sensitive values are injected via environment variables (see `.env.example`). Spring Profile YAMLs reference them as `${ENV_VAR}`:

| File | Purpose |
|------|---------|
| `.env` | LLM and Embedding credentials (NOT committed to git) |
| `.env.example` | Template for `.env`, safe to commit |
| `config/app.properties` | App name, port, version, Docker/Helm metadata |
| `config/local/application.yml` | Imports all profile-specific YAMLs |
| `config/local/application-llm.yml` | LLM provider, base-url, api-key, model |
| `config/local/application-chroma.yml` | ChromaDB service URL |
| `config/local/application-embedding.yml` | Embedding service URL and model |
| `config/local/application-rag.yml` | RAG params: `top-k`, `min-score`, `chunk.size`, `chunk.overlap` |

---

## Key Technical Notes

- **SSE streaming**: `/api/chat` returns Server-Sent Events for real-time LLM responses.
- **Prometheus metrics**: Micrometer + actuator enabled; metrics at `/actuator/prometheus`.
- **Docker base image**: `mingsha/jdk:dragonwell-21-alpine-3.23`.
- **K8s init containers**: Wait for `ollama` and `chromadb` services before starting the app.
- **Sensitive data**: Never commit `.env` or any file containing real credentials. Use `.env.example` as the template.
