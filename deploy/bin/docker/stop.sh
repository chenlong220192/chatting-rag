#! /bin/bash

#======================================================================
# author: mingsha
# date: 2020-08-10
#======================================================================

# ----------------------- params <---------------------------
# 容器名
DOCKER_CONTAINER_NAME=chatting-rag-boot
# 环境标志
PROFILE=dev
if [ -z "$PROFILE" ]; then
    PROFILE=dev
fi
# ----------------------- params >---------------------------

# stop
function stop() {
  str="Stop container"
  echo "${str}."
  docker ps -a | grep ${DOCKER_CONTAINER_NAME}-${PROFILE}
  if [[ $? -eq 0 ]]
  then
    CONTAINER_ID=$(docker ps -a | grep ${DOCKER_CONTAINER_NAME}-${PROFILE} | grep -v grep | awk '{ print $1}')
    docker stop ${CONTAINER_ID}
    if [[ $? -eq 0 ]]
    then
      echo "Stop container[" ${CONTAINER_ID} "] Success."
    else
      echo "Stop container[" ${CONTAINER_ID} "] Failure."
      exit 1
    fi
  else
    echo "There is no target container."
  fi

}

stop
