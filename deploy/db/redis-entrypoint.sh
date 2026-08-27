#!/bin/sh
# Redis 自定义入口脚本
# 先启动 Redis，然后执行初始化脚本（仅在首次启动时）

set -e

# 检查是否是首次启动（数据目录为空）
if [ ! -f /data/dump.rdb ] && [ -z "$(ls -A /data 2>/dev/null)" ]; then
  echo "检测到首次启动，将执行初始化脚本..."
  
  # 启动 Redis 服务器（后台运行）
  redis-server --daemonize yes
  
  # 等待 Redis 就绪
  until redis-cli ping > /dev/null 2>&1; do
    echo "等待 Redis 启动..."
    sleep 1
  done
  
  echo "Redis 已启动，开始执行初始化脚本..."
  
  # 执行初始化脚本
  if [ -f /docker-entrypoint-initdb.d/redis-init.sh ]; then
    sh /docker-entrypoint-initdb.d/redis-init.sh
    echo "初始化脚本执行完成"
  fi
  
  # 停止后台 Redis 进程
  redis-cli shutdown
  
  echo "初始化完成，以前台模式启动 Redis..."
else
  echo "检测到已有数据，跳过初始化"
fi

# 以前台模式启动 Redis（作为主进程）
exec redis-server
