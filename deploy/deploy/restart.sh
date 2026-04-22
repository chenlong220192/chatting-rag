#! /bin/bash

#======================================================================
# shell脚本
# 先调用shutdown.sh停服
# 然后调用startup.sh启动服务
#
# author: mingsha
# date: 2024-04-27
#======================================================================

# 项目名称
APPLICATION=chatting-rag-boot
# 项目启动jar包名称
APPLICATION_JAR=chatting-rag-boot-2026.04.22.jar

# bin目录绝对路径
BIN_PATH=$(cd `dirname $0`; pwd)
# 进入bin目录
cd `dirname $0`
# 返回到上一级项目根目录路径
cd ..
# 打印项目根目录绝对路径
# `pwd` 执行系统命令并获得结果
BASE_PATH=`pwd`

# 停服
echo stop ${APPLICATION} Application...
sh ${BIN_PATH}/shutdown.sh

# 启动服务
echo start ${APPLICATION} Application...
sh ${BIN_PATH}/startup.sh
