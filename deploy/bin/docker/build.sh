#! /bin/bash

#======================================================================
# author: mingsha
# date: 2024-04-27
#======================================================================

# ----------------------- params <---------------------------
# 仓库名
DOCKER_REPOSITORY_NAME=$1
# 镜像名
DOCKER_IMAGE_NAME=$2
# tag
DOCKER_IMAGE_TAG=$3
# 环境标志
PROFILE=$4
if [ -z "$PROFILE" ]; then
    PROFILE=local
fi
# ----------------------- params <---------------------------

# integration目录绝对路径
INTEGRATION_PATH=$(cd `dirname $0`; pwd) && cd `dirname $0` && cd .. && cd .. && cd .. && BASE_PATH=`pwd`

# build
function build() {
  str="Build Target Env ${PROFILE}"
  echo "${str}."
  docker build --load --platform linux/amd64 -f ${BASE_PATH}/deploy/docker/Dockerfile-boot --rm -t ${DOCKER_REPOSITORY_NAME}:${DOCKER_IMAGE_NAME}-${DOCKER_IMAGE_TAG}-${PROFILE} ${BASE_PATH}
  if [[ $? -eq 0 ]]
  then
    echo "${str} Success."
  else
    echo "${str} Failure."
    exit 1
  fi
}

build
