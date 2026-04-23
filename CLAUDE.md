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

### Directory Convention

All worktrees are created under `.claude/worktrees/` with sub-directories by type:

```
.claude/worktrees/
├── feature/          → feature branches
├── bugfix/           → bug fix branches
├── hotfix/           → urgent production fix branches
└── refactor/         → refactoring branches
```

> Example: `.claude/worktrees/feature/unit-tests/` for branch `feature/unit-tests`

### Core Principle

> **Never develop on `main`. Always develop on a feature/bugfix branch inside a dedicated worktree.**

```
main worktree (main)                           → production code, always clean and buildable
.claude/worktrees/feature/xxx (feature/xxx)    → feature development
.claude/worktrees/bugfix/yyy (bugfix/yyy)      → bug fix development
```

### Branch Naming Convention

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature/` | New feature development | `feature/unit-tests` |
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
# Syntax: git worktree add <path> -b <branch>
# Example: feature/unit-tests → .claude/worktrees/feature/unit-tests/
git worktree add .claude/worktrees/feature/unit-tests -b feature/unit-tests
```

This simultaneously:
- Creates a new directory `.claude/worktrees/feature/unit-tests/`
- Checks out a new branch `feature/unit-tests` based on `main`
- Attaches the worktree to the main repository

**Step 3 — Develop**

```bash
cd .claude/worktrees/feature/unit-tests

# Backend — in the worktree directory
make package ENV=dev
target/app/bin/startup.sh

# Frontend — in the ui/ subdirectory
cd ui && make run
```

**Step 4 — Commit and push**

```bash
cd .claude/worktrees/feature/unit-tests
git add .
git commit -m "feat: add unit tests for integration and biz layers"

# Push to remote to create PR
git push -u origin feature/unit-tests
```

**Step 5 — Create PR on GitHub**

Create a Pull Request on GitHub. Assign reviewers, address feedback, and merge once approved.

**Step 6 — Cleanup after merge**

```bash
# Back in the main repository
git checkout main
git pull origin main

# Remove the merged worktree directory
git worktree remove .claude/worktrees/feature/unit-tests

# Prune stale remote tracking branches
git fetch --prune origin
git branch -d feature/unit-tests          # delete local branch reference
git push origin --delete feature/unit-tests  # delete remote branch
```

---

### Workflow: Switch Between Tasks

You can have multiple worktrees simultaneously:

```bash
# List all active worktrees
git worktree list

# Example output:
# /data/repos/mingsha/chatting-rag               main      (repository)
# /data/repos/mingsha/chatting-rag/.claude/worktrees/feature/unit-tests  feature/unit-tests  ...
# /data/repos/mingsha/chatting-rag/.claude/worktrees/bugfix/upload-validation  bugfix/upload-validation  ...

# Switch to a different task
cd .claude/worktrees/bugfix/upload-validation
# ... do work, commit, push
```

---

### Workflow: Handle a Bug on `main` While Mid-Task

If you're developing a feature and a critical bug is reported:

```bash
# Stash current work (don't commit half-done changes)
# Note: git stash is repository-wide, not worktree-specific
git stash push -m "feature/xxx work in progress"

# Create a hotfix worktree
git worktree add .claude/worktrees/hotfix/urgent-bug -b hotfix/urgent-bug

# ... fix the bug, commit, push, merge ...

# Back to your feature
git checkout feature/xxx
git stash pop
```

---

### Workflow: Rebase onto Latest `main`

If `main` has moved forward while you're developing:

```bash
cd .claude/worktrees/feature/xxx

# Rebase your feature onto latest main
git fetch origin main
git rebase origin/main

# If conflicts occur, resolve them and continue:
git rebase --continue
```

---

### Workflow: Resolve Merge Conflicts

If `git rebase` pauses due to conflicts:

```bash
cd .claude/worktrees/feature/xxx
git rebase origin/main

# Git marks conflicts like this:
# <<<<<<< HEAD
# 你的更改
# =======
# origin/main 的更改
# >>>>>>> <commit-hash>
```

Resolve each conflicted file, then:

```bash
git add .
git rebase --continue
```

> **Do NOT use `git rebase --skip`** — it discards commits from `main`.

To abort and give up:

```bash
git rebase --abort
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
make helm.upgrade ENV=dev             # build, push, and deploy to k8s

# Frontend (run from ui/ directory)
cd ui && make install                  # install dependencies
cd ui && make run                      # dev server (port 8000)
cd ui && make package ENV=dev          # build dev artifact
```

**Profiles**: `local`, `dev` — selected via `ENV=` in Makefile. `local` for local dev, `dev` for k8s deployment. Each profile has its own config under `config/<profile>/`.

---

## Architecture

```
boot/               → Spring Boot entry point (site.mingsha.chatting.rag)
app/web/            → REST controllers (ChatController /api/chat, DocumentController /api/documents)
app/biz/            → Business logic (RAGService, DocumentService, ChromaService)
                     └── langchain4j/ → LangChain4j RAG (ChatMemoryService: conversation memory)
app/integration/    → External clients (SpringAiLlmClient, SpringAiEmbeddingClient, ChromaClient)
app/test/           → Unit tests
assembly/           → Maven assembly packaging, produces tar.gz release bundle
ui/                 → React frontend (Vite, separate from Spring)
```

**Dependency chain**: `boot` → `web` → `biz` → `integration`

**RAG flow**: User query → embed → ChromaDB similarity search → build prompt with top-K chunks + conversation memory → LLM streaming response (SSE).

**AI stack**:
- Spring AI 1.0.0-M6 → LLM calls (MiniMax OpenAI-compatible) + Embedding calls (Ollama)
- LangChain4j 1.0.0 → Conversation memory (MessageWindowChatMemory) + Document parsing (Apache Tika) + Smart chunking (RecursiveCharacterTextSplitter)

---

## Configuration

Application runs on **port 8001**. Profile-specific configuration files are placed under `config/<profile>/`. Sensitive LLM credentials are managed via `config/<profile>/application-llm.yml` (see Sensitive Data Policy below):

| File | Purpose |
|------|---------|
| `config/app.properties` | App name, port, version, Docker/Helm metadata |
| `config/local/application.yml` | Imports all profile-specific YAMLs |
| `config/application-llm.yml.example` | Template for LLM credentials (safe to commit) |
| `config/<profile>/application-llm.yml` | LLM provider credentials (gitignored) |
| `config/local/application-chroma.yml` | ChromaDB service URL |
| `config/local/application-embedding.yml` | Embedding service URL and model |
| `config/local/application-rag.yml` | RAG params: `top-k`, `min-score`, `chunk.size`, `chunk.overlap` |

---

## Sensitive Data Policy

**Never commit files containing real credentials.** The following must never be pushed to git:

- `config/<profile>/application-llm.yml` — contains real LLM API keys and tokens (gitignored via `config/**/application-llm.yml`)

When setting up a new environment:

1. Copy `config/application-llm.yml.example` → `config/<profile>/application-llm.yml` (e.g. `config/dev/application-llm.yml`)
2. Fill in real credentials for the target profile

---

## Key Technical Notes

- **SSE streaming**: `/api/chat` returns Server-Sent Events for real-time LLM responses.
- **Prometheus metrics**: Micrometer + actuator enabled; metrics at `/actuator/prometheus`.
- **Docker base image**: `mingsha/jdk:dragonwell-21-alpine-3.23`.
- **K8s init containers**: Wait for `ollama` and `chromadb` services before starting the app.
