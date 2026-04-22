#======================================================================
#
# example 'make init'
# example 'make package SKIP_TEST=true ENV=dev'
#
# author: mingsha
# date: 2025-07-11
#======================================================================

SHELL := /bin/bash -o pipefail

export BASE_PATH := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

# ----------------------------- colors <-----------------------------
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
# 颜色变量（用于 printf）
RED=\033[31m
GREEN=\033[32m
YELLOW=\033[33m
BLUE=\033[34m
CYAN=\033[36m
BOLD=\033[1m
RESET=\033[0m
# ----------------------------- colors >-----------------------------

# ----------------------------- variables <-----------------------------
# unit test flag
SKIP_TEST ?= false
# env
ENV ?= dev
# ----------------------------- variables >-----------------------------

# ----------------------------- help <-----------------------------
.PHONY: help
help: ## $(HELP) 显示帮助信息
	@printf "${BOLD}${CYAN}╔══════════════════════════════════════════════════════════════╗${RESET}\n"
	@printf "${BOLD}${CYAN}║                    ${ROCKET} 构建工具 ${ROCKET}                    ║${RESET}\n"
	@printf "${BOLD}${CYAN}╚══════════════════════════════════════════════════════════════╝${RESET}\n"
	@printf "\n"
	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "使用方法"

	@printf "  make <target> [ENV=environment] [SKIP_TEST=true|false]\n"
	@printf "\n"

	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "环境变量"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "ENV" "- 环境配置 (默认: local, 可选: dev, test, prod)"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "SKIP_TEST" "- 跳过测试 (默认: false, 可选: true)"
	@printf "\n"

	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "可用目标"
	@awk 'BEGIN {FS = ":.*?## "; max=22} /^[a-zA-Z0-9_.-]+:.*?## / {cmd=$$1; desc=$$2; printf "  ${GREEN}%-*s${RESET} %s\n", max, cmd, desc}' max=22 $(MAKEFILE_LIST) | \
		sed 's/\$$(HELP)/$(HELP)/g' | sed 's/\$$(CLEAN)/$(CLEAN)/g' | sed 's/\$$(TEST)/$(TEST)/g' | sed 's/\$$(PACKAGE)/$(PACKAGE)/g' | sed 's/\$$(DOCKER)/$(DOCKER)/g' | sed 's/\$$(HELM)/$(HELM)/g'
	@printf "\n"

	@printf "${BOLD}${YELLOW}%-8s:${RESET}\n" "示例"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make help" "${HELP} 显示此帮助信息"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make clean" "${CLEAN} 清理构建文件"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make package ENV=dev" "${PACKAGE} 构建开发环境包"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make docker.build ENV=prod" "${DOCKER} 构建生产环境Docker镜像"
	@printf "  ${GREEN}%-22s${RESET} %s\n" "make helm.upgrade ENV=test" "${HELM} 部署到测试环境"
	@printf "\n"
	@printf "${BOLD}${CYAN}╔══════════════════════════════════════════════════════════════╗${RESET}\n"
	@printf "${BOLD}${CYAN}║                    ${SUCCESS} 构建愉快！${SUCCESS}                              ║${RESET}\n"
	@printf "${BOLD}${CYAN}╚══════════════════════════════════════════════════════════════╝${RESET}\n"

.DEFAULT_GOAL := help
# ----------------------------- help >-----------------------------

# ----------------------------- app.properties <-----------------------------
# app name
APPLICATION_NAME := $(shell grep "app.name" $(BASE_PATH)/config/app.properties | cut -d'=' -f2| sed 's/\ //')
# jar name
APPLICATION_JAR := $(shell grep "app.name" $(BASE_PATH)/config/app.properties | cut -d'=' -f2| sed 's/\ //')-$(shell grep "app.version" $(BASE_PATH)/config/app.properties | cut -d'=' -f2| sed 's/\ //').jar
# port
APPLICATION_PORT := $(shell grep "app.port" $(BASE_PATH)/config/app.properties | cut -d'=' -f2| sed 's/\ //')
# docker.repository.name
DOCKER_REPOSITORY_NAME := $(shell grep "docker.repository.name" $(BASE_PATH)/config/app.properties | cut -d'=' -f2| sed 's/\ //')
# docker image name
DOCKER_IMAGE_NAME := $(shell grep "docker.image.name" $(BASE_PATH)/config/app.properties | cut -d'=' -f2| sed 's/\ //')
# docker image tag
DOCKER_IMAGE_TAG := $(shell grep "docker.image.tag" $(BASE_PATH)/config/app.properties | cut -d'=' -f2| sed 's/\ //')
# docker container name
DOCKER_CONTAINER_NAME := $(shell grep "docker.container.name" $(BASE_PATH)/config/app.properties | cut -d'=' -f2| sed 's/\ //')
# helm namespace
HELM_NAMESPACE := $(shell grep "helm.namespace" $(BASE_PATH)/config/app.properties | cut -d'=' -f2| sed 's/\ //')
# ----------------------------- app.properties >-----------------------------

# ----------------------------- maven <-----------------------------
#
clean: ## $(CLEAN) 清理Maven构建文件
	@printf "${BLUE}${CLEAN} 清理构建文件...${RESET}\n"
	$(BASE_PATH)/mvnw --batch-mode --errors -f ${BASE_PATH}/pom.xml clean
	@printf "${GREEN}${SUCCESS} 清理完成！${RESET}\n"

#
test: ## $(TEST) 运行单元测试
	@printf "${BLUE}${TEST} 运行单元测试...${RESET}\n"
	$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml clean test -D test=*Test -DfailIfNoTests=false
	@printf "${GREEN}${SUCCESS} 测试完成！${RESET}\n"

#
package: ## $(PACKAGE) 构建应用包
	@printf "${BLUE}${PACKAGE} 构建应用包 (环境: ${ENV})...${RESET}\n"
	@cat ${BASE_PATH}/deploy/deploy/debugger.sh | \
		sed 's/APPLICATION=[^<]*/APPLICATION=${APPLICATION_NAME}/1' | \
		sed 's/APPLICATION_JAR=[^<]*/APPLICATION_JAR=${APPLICATION_JAR}/1' \
		> ${BASE_PATH}/deploy/deploy/debugger.sh.newVersion && mv ${BASE_PATH}/deploy/deploy/debugger.sh.newVersion ${BASE_PATH}/deploy/deploy/debugger.sh
	@cat ${BASE_PATH}/deploy/deploy/startup.sh | \
		sed 's/APPLICATION=[^<]*/APPLICATION=${APPLICATION_NAME}/1' | \
		sed 's/APPLICATION_JAR=[^<]*/APPLICATION_JAR=${APPLICATION_JAR}/1' \
		> ${BASE_PATH}/deploy/deploy/startup.sh.newVersion && mv ${BASE_PATH}/deploy/deploy/startup.sh.newVersion ${BASE_PATH}/deploy/deploy/startup.sh
	@cat ${BASE_PATH}/deploy/deploy/shutdown.sh | \
		sed 's/APPLICATION=[^<]*/APPLICATION=${APPLICATION_NAME}/1' | \
		sed 's/APPLICATION_JAR=[^<]*/APPLICATION_JAR=${APPLICATION_JAR}/1' \
		> ${BASE_PATH}/deploy/deploy/shutdown.sh.newVersion && mv ${BASE_PATH}/deploy/deploy/shutdown.sh.newVersion ${BASE_PATH}/deploy/deploy/shutdown.sh
	@cat ${BASE_PATH}/deploy/deploy/restart.sh | \
		sed 's/APPLICATION=[^<]*/APPLICATION=${APPLICATION_NAME}/1' | \
		sed 's/APPLICATION_JAR=[^<]*/APPLICATION_JAR=${APPLICATION_JAR}/1' \
		> ${BASE_PATH}/deploy/deploy/restart.sh.newVersion && mv ${BASE_PATH}/deploy/deploy/restart.sh.newVersion ${BASE_PATH}/deploy/deploy/restart.sh
	$(BASE_PATH)/mvnw --batch-mode --errors --fail-at-end --update-snapshots -f ${BASE_PATH}/pom.xml clean package -P ${ENV} -D maven.test.skip=$(SKIP_TEST)
	@printf "${GREEN}${SUCCESS} 构建完成！${RESET}\n"

#
package.uncompress: package ## 📂 解压构建包
	@printf "${BLUE}📂 解压构建包...${RESET}\n"
	mkdir -p ${BASE_PATH}/target/app
	tar -zxvf ${BASE_PATH}/target/*.tar.gz -C ${BASE_PATH}/target/app --strip-components 1
	@printf "${GREEN}解压完成！${RESET}\n"
# ----------------------------- maven >-----------------------------


# ----------------------------- docker <-----------------------------
docker.init: ## $(DOCKER) 初始化Docker配置
	@printf "${BLUE}${DOCKER} 初始化Docker配置...${RESET}\n"
	@cat ${BASE_PATH}/deploy/bin/docker/stop.sh | \
		sed 's#DOCKER_CONTAINER_NAME=[^<]*#DOCKER_CONTAINER_NAME=${DOCKER_CONTAINER_NAME}#1' | \
		sed 's#PROFILE=[^<]*#PROFILE=${ENV}#1' \
		> ${BASE_PATH}/deploy/bin/docker/stop.sh.newVersion && \
		mv ${BASE_PATH}/deploy/bin/docker/stop.sh.newVersion ${BASE_PATH}/deploy/bin/docker/stop.sh
	@cat ${BASE_PATH}/deploy/bin/docker/remove.sh | \
		sed 's#DOCKER_CONTAINER_NAME=[^<]*#DOCKER_CONTAINER_NAME=${DOCKER_CONTAINER_NAME}#1' | \
		sed 's#PROFILE=[^<]*#PROFILE=${ENV}#1' \
		> ${BASE_PATH}/deploy/bin/docker/remove.sh.newVersion && \
		mv ${BASE_PATH}/deploy/bin/docker/remove.sh.newVersion ${BASE_PATH}/deploy/bin/docker/remove.sh
	@cat ${BASE_PATH}/deploy/bin/docker/run.sh | \
		sed 's#DOCKER_REPOSITORY_NAME=[^<]*#DOCKER_REPOSITORY_NAME=${DOCKER_REPOSITORY_NAME}#1' | \
		sed 's#DOCKER_IMAGE_NAME=[^<]*#DOCKER_IMAGE_NAME=${DOCKER_IMAGE_NAME}#1' | \
		sed 's#DOCKER_IMAGE_TAG=[^<]*#DOCKER_IMAGE_TAG=${DOCKER_IMAGE_TAG}#1' | \
		sed 's#DOCKER_CONTAINER_NAME=[^<]*#DOCKER_CONTAINER_NAME=${DOCKER_CONTAINER_NAME}#1' | \
		sed 's#APPLICATION_PORT=[^<]*#APPLICATION_PORT=${APPLICATION_PORT}#1' | \
		sed 's#PROFILE=[^<]*#PROFILE=${ENV}#1' \
		> ${BASE_PATH}/deploy/bin/docker/run.sh.newVersion && \
		mv ${BASE_PATH}/deploy/bin/docker/run.sh.newVersion ${BASE_PATH}/deploy/bin/docker/run.sh
	@printf "${GREEN}${DOCKER} Docker配置初始化完成！${RESET}\n"

#
docker.build: ## $(DOCKER) 构建Docker镜像
	@printf "${BLUE}${DOCKER} 构建Docker镜像 (环境: ${ENV})...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/docker/build.sh $(DOCKER_REPOSITORY_NAME) $(DOCKER_IMAGE_NAME) $(DOCKER_IMAGE_TAG) $(ENV)
	@printf "${GREEN}${DOCKER} Docker镜像构建完成！${RESET}\n"

#
docker.push: ## $(DOCKER) 构建Docker镜像
	@printf "${BLUE}${DOCKER} 推送Docker镜像 (环境: ${ENV})...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/docker/push.sh $(DOCKER_REPOSITORY_NAME) $(DOCKER_IMAGE_NAME) $(DOCKER_IMAGE_TAG) $(ENV)
	@printf "${GREEN}${DOCKER} Docker镜像推送完成！${RESET}\n"

#
docker.run: package.uncompress docker.init docker.build ## $(DOCKER) 运行Docker容器
	@printf "${BLUE}${DOCKER} 运行Docker容器...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/docker/run.sh $(DOCKER_REPOSITORY_NAME) $(DOCKER_IMAGE_NAME) $(DOCKER_IMAGE_TAG) $(DOCKER_CONTAINER_NAME) $(ENV)
	# docker logs -f $(DOCKER_CONTAINER_NAME)-$(ENV)
	@printf "${GREEN}${DOCKER} Docker容器启动完成！${RESET}\n"

#
docker.stop: ## 🛑 停止Docker容器
	@printf "${RED}🛑 停止Docker容器...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/docker/stop.sh $(DOCKER_CONTAINER_NAME) $(ENV)
	@printf "${GREEN}Docker容器已停止！${RESET}\n"

#
docker.remove: ## 🗑️ 删除Docker容器
	@printf "${RED}🗑️ 删除Docker容器...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/docker/remove.sh $(DOCKER_CONTAINER_NAME) $(ENV)
	@printf "${GREEN}Docker容器已删除！${RESET}\n"

#
docker.clear: ## 🧹 清理Docker资源
	@printf "${RED}🧹 清理Docker资源...${RESET}\n"
	docker rm -f $(docker ps -a | grep $(DOCKER_CONTAINER_NAME) | grep -v grep | awk '{ print $1 }')
	docker rmi -f $(docker images | grep $(DOCKER_CONTAINER_NAME) | grep -v grep | awk '{ print $3 }')
	@printf "${GREEN}Docker资源清理完成！${RESET}\n"
# ----------------------------- docker >-----------------------------

# ----------------------------- helm <-----------------------------
#
helm.uninstall: ## $(HELM) 卸载Helm部署
	@printf "${BLUE}${HELM} 卸载Helm部署...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/helm/uninstall.sh $(APPLICATION_NAME) $(HELM_NAMESPACE) $(ENV)
	@printf "${GREEN}${HELM} Helm部署卸载完成！${RESET}\n"
#
helm.upgrade: package.uncompress docker.init docker.build helm.uninstall ## $(HELM) 升级Helm部署
	@printf "${BLUE}${HELM} 升级Helm部署 (环境: ${ENV})...${RESET}\n"
	sh ${BASE_PATH}/deploy/bin/helm/install.sh $(APPLICATION_NAME) $(HELM_NAMESPACE) $(ENV)
	@printf "${GREEN}${HELM} Helm部署升级完成！${RESET}\n"
# ----------------------------- helm >-----------------------------
