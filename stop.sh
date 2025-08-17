#!/bin/bash

echo "=========================================="
echo "        手机中控系统停止脚本"
echo "=========================================="

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "📁 工作目录: $SCRIPT_DIR"
echo ""

# 停止后端服务
if [ -f logs/backend.pid ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    echo "🛑 停止后端服务 (PID: $BACKEND_PID)..."
    
    # 优雅停止
    kill $BACKEND_PID 2>/dev/null
    
    # 等待进程结束
    sleep 3
    
    # 强制停止（如果还在运行）
    if ps -p $BACKEND_PID > /dev/null; then
        echo "⚡ 强制停止后端服务..."
        kill -9 $BACKEND_PID 2>/dev/null
    fi
    
    # 清理PID文件
    rm -f logs/backend.pid
    echo "✅ 后端服务已停止"
else
    echo "ℹ️  未找到后端服务PID文件"
fi

# 停止前端服务
if [ -f logs/frontend.pid ]; then
    FRONTEND_PID=$(cat logs/frontend.pid)
    echo "🛑 停止前端服务 (PID: $FRONTEND_PID)..."
    
    # 优雅停止
    kill $FRONTEND_PID 2>/dev/null
    
    # 等待进程结束
    sleep 2
    
    # 强制停止（如果还在运行）
    if ps -p $FRONTEND_PID > /dev/null; then
        echo "⚡ 强制停止前端服务..."
        kill -9 $FRONTEND_PID 2>/dev/null
    fi
    
    # 清理PID文件
    rm -f logs/frontend.pid
    echo "✅ 前端服务已停止"
else
    echo "ℹ️  未找到前端服务PID文件"
fi

# 查找并停止可能遗留的进程
echo ""
echo "🔍 检查遗留进程..."

# 停止Spring Boot进程
SPRING_PIDS=$(ps aux | grep "spring-boot:run" | grep -v grep | awk '{print $2}')
if [ ! -z "$SPRING_PIDS" ]; then
    echo "🛑 停止Spring Boot进程: $SPRING_PIDS"
    echo $SPRING_PIDS | xargs kill -9 2>/dev/null
fi

# 停止npm dev进程
NPM_PIDS=$(ps aux | grep "npm run dev" | grep -v grep | awk '{print $2}')
if [ ! -z "$NPM_PIDS" ]; then
    echo "🛑 停止npm dev进程: $NPM_PIDS"
    echo $NPM_PIDS | xargs kill -9 2>/dev/null
fi

# 停止node进程（前端开发服务器）
NODE_PIDS=$(ps aux | grep "vue-cli-service serve" | grep -v grep | awk '{print $2}')
if [ ! -z "$NODE_PIDS" ]; then
    echo "🛑 停止Vue开发服务器: $NODE_PIDS"
    echo $NODE_PIDS | xargs kill -9 2>/dev/null
fi

echo ""
echo "=========================================="
echo "✅ 所有服务已停止"
echo ""
echo "📝 日志文件保留在 logs/ 目录中"
echo "🚀 重新启动: ./start.sh"
echo "=========================================="