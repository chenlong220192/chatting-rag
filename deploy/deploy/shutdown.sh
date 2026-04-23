#! /bin/bash

#======================================================================
# shell脚本
# 通过项目名称查找到PID
# 然后kill -9 pid
#
# author: mingsha
# date: 2024-04-27
#======================================================================

# 项目名称
APPLICATION=chatting-rag-boot

# bin目录绝对路径
BIN_PATH=$(cd `dirname $0`; pwd)
# 进入bin目录
cd `dirname $0`
# 返回到上一级项目根目录路径
cd ..
# 打印项目根目录绝对路径
BASE_PATH=`pwd`

# 项目启动jar包名称（动态读取boot目录下的JAR文件）
APPLICATION_JAR=chatting-rag-boot-2026.04.22.jar

PID=$(ps -eo user,pid,tty,args | grep "${APPLICATION_JAR}" | grep -v grep | awk '{ print $2 }')
if [[ -z "$PID" ]]
then
    echo ${APPLICATION} is already stopped
else
    echo "kill -9 ${PID}"
    kill ${PID}
    if [[ $? -eq 0 ]]
  	then
    	echo "${APPLICATION} stopped successfully"
  	else
    	echo "${APPLICATION} stopped Failure"
    	exit 1
  	fi
fi
