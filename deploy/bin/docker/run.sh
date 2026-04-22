#! /bin/bash

#======================================================================
# author: mingsha
# date: 2020-08-10
#======================================================================

# ----------------------- params <---------------------------
# 仓库名
DOCKER_REPOSITORY_NAME=mingsha/app
# 镜像名
DOCKER_IMAGE_NAME=chatting-rag-boot
# tag
DOCKER_IMAGE_TAG=2026.04.22
# 容器名
DOCKER_CONTAINER_NAME=chatting-rag-boot
# 端口号
APPLICATION_PORT=8001
# 环境标志
PROFILE=dev
if [ -z "$PROFILE" ]; then
    PROFILE=dev
fi
# ----------------------- params >---------------------------

# run
function run() {
  str="Run"
  echo "${str}."
  mkdir -p /data/containers/${DOCKER_CONTAINER_NAME}-${PROFILE}/logs
  mkdir -p /data/containers/${DOCKER_CONTAINER_NAME}-${PROFILE}/dump
  mkdir -p /data/containers/${DOCKER_CONTAINER_NAME}-${PROFILE}/gc
  docker run -d -it \
    -p ${APPLICATION_PORT}:${APPLICATION_PORT} \
    -v /data/containers/${DOCKER_CONTAINER_NAME}-${PROFILE}/logs:/app/logs \
    -v /data/containers/${DOCKER_CONTAINER_NAME}-${PROFILE}/dump:/app/dump \
    -v /data/containers/${DOCKER_CONTAINER_NAME}-${PROFILE}/gc:/app/gc \
    --memory 2048m \
    --restart=always \
    --name ${DOCKER_CONTAINER_NAME}-${PROFILE} \
    ${DOCKER_REPOSITORY_NAME}:${DOCKER_IMAGE_NAME}-${DOCKER_IMAGE_TAG}-${PROFILE}
  if [[ $? -eq 0 ]]
  then
    echo "${str} Success."
  else
    echo "${str} Failure."
    exit 1
  fi
}

run
