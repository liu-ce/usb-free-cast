#!/bin/bash

echo "=========================================="
echo "        手机中控系统启动脚本"
echo "=========================================="

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到Java环境，请安装JDK 1.8+"
    exit 1
fi

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: 未找到Maven，请安装Maven 3.6+"
    exit 1
fi

# 检查Node.js环境
if ! command -v node &> /dev/null; then
    echo "❌ 错误: 未找到Node.js，请安装Node.js 14+"
    exit 1
fi

echo "✅ 环境检查通过"
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "📁 工作目录: $SCRIPT_DIR"
echo ""

# 编译后端
echo "🔨 编译后端项目..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "❌ 后端编译失败"
    exit 1
fi
echo "✅ 后端编译完成"

# 检查前端依赖
echo "📦 检查前端依赖..."
cd frontend
if [ ! -d "node_modules" ]; then
    echo "📦 安装前端依赖..."
    npm install
    if [ $? -ne 0 ]; then
        echo "❌ 前端依赖安装失败"
        exit 1
    fi
fi
echo "✅ 前端依赖就绪"

cd "$SCRIPT_DIR"

# 创建日志目录
mkdir -p logs

echo ""
echo "🚀 启动服务..."
echo ""

# 启动后端服务（后台运行）
echo "🟢 启动后端服务 (端口: 8080)..."
nohup mvn spring-boot:run > logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "后端PID: $BACKEND_PID"

# 等待后端启动
echo "⏳ 等待后端服务启动..."
sleep 10

# 检查后端是否启动成功
if ps -p $BACKEND_PID > /dev/null; then
    echo "✅ 后端服务启动成功"
else
    echo "❌ 后端服务启动失败"
    exit 1
fi

# 启动前端服务（后台运行）
echo "🟡 启动前端服务 (端口: 3000)..."
cd frontend
nohup npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "前端PID: $FRONTEND_PID"

cd "$SCRIPT_DIR"

# 保存PID到文件
echo $BACKEND_PID > logs/backend.pid
echo $FRONTEND_PID > logs/frontend.pid

echo ""
echo "=========================================="
echo "🎉 启动完成！"
echo ""
echo "📱 前端地址: http://localhost:3000"
echo "🔧 后端地址: http://localhost:8080"
echo ""
echo "📋 进程信息:"
echo "   后端PID: $BACKEND_PID"
echo "   前端PID: $FRONTEND_PID"
echo ""
echo "📝 日志文件:"
echo "   后端日志: logs/backend.log"
echo "   前端日志: logs/frontend.log"
echo ""
echo "⏹️  停止服务: ./stop.sh"
echo "=========================================="

# 等待用户输入来保持脚本运行
echo ""
echo "按 Ctrl+C 或运行 ./stop.sh 来停止服务"

# 创建信号处理函数
cleanup() {
    echo ""
    echo "🛑 正在停止服务..."
    
    if [ -f logs/backend.pid ]; then
        BACKEND_PID=$(cat logs/backend.pid)
        kill $BACKEND_PID 2>/dev/null
        echo "停止后端服务 (PID: $BACKEND_PID)"
    fi
    
    if [ -f logs/frontend.pid ]; then
        FRONTEND_PID=$(cat logs/frontend.pid)
        kill $FRONTEND_PID 2>/dev/null
        echo "停止前端服务 (PID: $FRONTEND_PID)"
    fi
    
    # 清理PID文件
    rm -f logs/backend.pid logs/frontend.pid
    
    echo "✅ 服务已停止"
    exit 0
}

# 捕获Ctrl+C信号
trap cleanup SIGINT SIGTERM

# 监控服务状态
while true; do
    sleep 5
    
    # 检查后端服务状态
    if [ -f logs/backend.pid ]; then
        BACKEND_PID=$(cat logs/backend.pid)
        if ! ps -p $BACKEND_PID > /dev/null; then
            echo "❌ 后端服务异常退出，请检查日志: logs/backend.log"
            cleanup
        fi
    fi
    
    # 检查前端服务状态
    if [ -f logs/frontend.pid ]; then
        FRONTEND_PID=$(cat logs/frontend.pid)
        if ! ps -p $FRONTEND_PID > /dev/null; then
            echo "❌ 前端服务异常退出，请检查日志: logs/frontend.log"
            cleanup
        fi
    fi
done