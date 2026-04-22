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
function logs() {
  str="logs"
  echo "${str}."
  kubectl logs -f -n ${NAMESPACE}-${ENV} $(kubectl get pods -n ${NAMESPACE}-${ENV} | grep ${POD_NAME} | awk '{print $1}' | sed "s@ @:@g")
}

logs
