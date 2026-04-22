#! /bin/bash

#======================================================================
# author: mingsha
# date: 2024-04-09
#======================================================================

# bin目录绝对路径
BIN_PATH=$(cd `dirname $0`; pwd)
# 进入bin目录
cd `dirname $0`
# 返回到上一级项目根目录路径
cd ../../../
# 打印项目根目录绝对路径
# `pwd` 执行系统命令并获得结果
BASE_PATH=`pwd`

APPNAME=$1
VERSION=$2
PORT=$3
ENV=$4

docker run -d -it \
  -p ${PORT}:${PORT} \
  --memory 128m \
  --restart=always \
  --name ${APPNAME}-${ENV} \
  mingsha.site:5555/app/${APPNAME}-${ENV}:${VERSION}
