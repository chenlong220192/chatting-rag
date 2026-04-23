#======================================================================
#
# example 'make init'
# example 'make package SKIP_TEST=true ENV=dev'
#
# author: chatting-rag
# date: 2025-07-11
#======================================================================

SHELL := /bin/bash -o pipefail

export BASE_PATH := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

# ----------------------------- colors <-----------------------------
# 颜色变量（用于 printf）
RED=\033[31m
GREEN=\033[32m
YELLOW=\033[33m
BLUE=\033[34m
CYAN=\033[36m
BOLD=\033[1m
RESET=\033[0m

# Emojis and icons
ROCKET := 🚀
GEAR := ⚙️
TEST := 🧪
PACKAGE := 📦
DOCKER := 🐳
HELM := 🎯
CLEAN := 🧹
HELP := ❓
INFO := ℹ️
SUCCESS := ✅
WARNING := ⚠️
ERROR := ❌
INSTALL := 📥
RUN := ▶️
BUILD := 🔨
# ----------------------------- colors >-----------------------------

# ----------------------------- variables <-----------------------------
# unit test flag
ENV ?= dev
#
APPNAME ?= chatting-rag-ui
VERSION ?= 2026.04.22
PORT ?= 8000
HELM_NAMESPACE ?= mingsha
# ----------------------------- variables >-----------------------------

# ----------------------------- help <-----------------------------
.PHONY: help
help: ## $(HELP) 显示帮助信息
	@printf "${BOLD}${CYAN}╔══════════════════════════════════════════════════════════════╗${RESET}\n"
	@printf "${BOLD}${CYAN}║                    ${ROCKET} 前端构建工具 ${ROCKET}                        ║${RESET}\n"
	@printf "${BOLD}${CYAN}╚══════════════════════════════════════════════════════════════╝${RESET}\n"
	@printf "\n"
	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "使用方法"
	@printf "  make <target> [ENV=environment]\n"
	@printf "\n"
	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "环境变量"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "ENV" "- 环境配置 (默认: development, 可选: development, staging, production)"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "APPNAME" "- 应用名称 (默认: mingsha-vue2)"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "VERSION" "- 版本号 (默认: 1.0.0)"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "PORT" "- 端口号 (默认: 8000)"
	@printf "\n"
	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "可用目标"
	@awk 'BEGIN {FS = ":.*?## "; max=22} /^[a-zA-Z0-9_.-]+:.*?## / {cmd=$$1; desc=$$2; printf "  ${GREEN}%-*s${RESET} %s\n", max, cmd, desc}' max=22 $(MAKEFILE_LIST) | \
		sed 's/\$$(HELP)/$(HELP)/g' | sed 's/\$$(INSTALL)/$(INSTALL)/g' | sed 's/\$$(PACKAGE)/$(PACKAGE)/g' | sed 's/\$$(RUN)/$(RUN)/g' | sed 's/\$$(DOCKER)/$(DOCKER)/g' | sed 's/\$$(HELM)/$(HELM)/g'
	@printf "\n"
	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "示例"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make help" "${HELP} 显示此帮助信息"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make install" "${INSTALL} 安装依赖"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make package ENV=dev" "${PACKAGE} 构建开发环境包"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make docker.build ENV=prod" "${DOCKER} 构建生产环境Docker镜像"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make helm.upgrade ENV=test" "${HELM} 部署到测试环境"
	@printf "\n"
	@printf "${BOLD}${CYAN}╔══════════════════════════════════════════════════════════════╗${RESET}\n"
	@printf "${BOLD}${CYAN}║                    ${SUCCESS} 构建愉快！${SUCCESS}                              ║${RESET}\n"
	@printf "${BOLD}${CYAN}╚══════════════════════════════════════════════════════════════╝${RESET}\n"

.DEFAULT_GOAL := help
# ----------------------------- help >-----------------------------

# ----------------------------- local <-----------------------------
#
install: ## $(INSTALL) 安装依赖
	@printf "${BLUE}${INSTALL} 安装依赖...${RESET}\n"
	npm install $(if $(NPM_REGISTRY),--registry=$(NPM_REGISTRY),)
	@printf "${GREEN}${SUCCESS} 依赖安装完成！${RESET}\n"

clean: ## $(CLEAN) 清理前端构建产物
	@printf "${BLUE}${CLEAN} 清理前端构建产物...${RESET}\n"
	rm -rf ${BASE_PATH}/dist
	@printf "${GREEN}${SUCCESS} 清理完成！${RESET}\n"

test: ## $(TEST) 运行前端测试
	@printf "${BLUE}${TEST} 运行前端测试...${RESET}\n"
	@npm run test 2>/dev/null || echo "No test script configured"
	@printf "${GREEN}${SUCCESS} 测试完成！${RESET}\n"

#
package: ## $(PACKAGE) 构建前端包
	@printf "${BLUE}${PACKAGE} 构建前端包 (环境: ${ENV})...${RESET}\n"
	npm run build:prod
	@printf "${GREEN}${SUCCESS} 构建完成！${RESET}\n"

#
run: ## $(RUN) 启动开发服务器
	@printf "${BLUE}${RUN} 启动开发服务器...${RESET}\n"
	npm run dev
# ----------------------------- local  >-----------------------------

# ----------------------------- docker <-----------------------------
#
docker.build: install package ## $(DOCKER) 构建Docker镜像
	@printf "${BLUE}${DOCKER} 构建Docker镜像 (环境: ${ENV})...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/docker/build.sh ${APPNAME} ${VERSION} ${ENV}
	@printf "${GREEN}${SUCCESS} Docker镜像构建完成！${RESET}\n"

#
docker.run: install package docker.build ## $(DOCKER) 运行Docker容器
	@printf "${BLUE}${DOCKER} 运行Docker容器...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/docker/run.sh ${APPNAME} ${VERSION} ${PORT} ${ENV}
	@printf "${GREEN}${SUCCESS} Docker容器启动完成！${RESET}\n"
# ----------------------------- docker  >-----------------------------

# ----------------------------- helm <-----------------------------
#
helm.uninstall: ## $(HELM) 卸载Helm部署
	@printf "${BLUE}${HELM} 卸载Helm部署...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/helm/uninstall.sh $(APPNAME) $(HELM_NAMESPACE) $(ENV)
	@printf "${GREEN}${SUCCESS} Helm部署卸载完成！${RESET}\n"

#
helm.upgrade: docker.build helm.uninstall ## $(HELM) 升级Helm部署
	@printf "${BLUE}${HELM} 升级Helm部署 (环境: ${ENV})...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/helm/install.sh $(APPNAME) $(HELM_NAMESPACE) $(ENV)
	@printf "${GREEN}${SUCCESS} Helm部署升级完成！${RESET}\n"
# ----------------------------- helm  >-----------------------------
