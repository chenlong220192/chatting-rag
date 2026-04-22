#! /bin/bash

#======================================================================
# 1、打包
# 2、构建镜像
#
# example: 'sh run.sh [<local>|<dev>|<test>|<prod>]'
#
# local - 本地（默认）
# dev   - 开发
# test  - 测试
# prod  - 生产
#
# author: mingsha
# date: 2020-08-10
#======================================================================

BASE_PATH=`pwd`

# ----------------------- params <---------------------------
# POD_NAME
POD_NAME=$1
# NAMESPACE
NAMESPACE=$2
# ENV
ENV=$3
# ----------------------- params >---------------------------

# 
function install() {
  str="helm install"
  echo "${str}."
  helm upgrade --install --create-namespace ${POD_NAME} --namespace ${NAMESPACE}-${ENV} --version 1.0.0 ${BASE_PATH}/deploy/helm-chart
  if [[ $? -eq 0 ]]
  then
    echo "${str} Success."
  else
    echo "${str} Failure."
    exit 1
  fi
}

install
