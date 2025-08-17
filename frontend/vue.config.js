module.exports = {
  devServer: {
    port: 3000,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  },
  
  // 生产环境配置
  publicPath: './',
  outputDir: 'dist',
  assetsDir: 'static',
  
  // 关闭生产环境的source map
  productionSourceMap: false,
  
  // webpack配置
  configureWebpack: {
    resolve: {
      alias: {
        '@': require('path').resolve(__dirname, 'src')
      }
    }
  }
}