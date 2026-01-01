import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Glasses, Smartphone, Cloud, Database, Zap } from 'lucide-react';

const IntroScene: React.FC = () => {
  const [dataFlowStep, setDataFlowStep] = useState(0);
  
  // 数据流动画 - 只播放一遍
  useEffect(() => {
    if (dataFlowStep < 4) {
      const timer = setTimeout(() => {
        setDataFlowStep(prev => prev + 1);
      }, 1200);
      return () => clearTimeout(timer);
    }
  }, [dataFlowStep]);

  return (
    <div className="flex flex-col items-center justify-center h-full relative overflow-hidden">
      {/* 主标题 - 打字机效果 */}
      <motion.div
        initial={{ opacity: 0, y: -30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8 }}
        className="text-center mb-8"
      >
        <motion.h1 
          className="text-7xl font-black text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 via-blue-500 to-purple-500 mb-4"
          animate={{ backgroundPosition: ['0%', '100%', '0%'] }}
          transition={{ duration: 5, repeat: Infinity }}
          style={{ backgroundSize: '200%' }}
        >
          VisEat
        </motion.h1>
        <motion.p 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.5 }}
          className="text-2xl text-slate-300 font-light"
        >
          新一代 <span className="text-cyan-400 font-semibold">AI + AR</span> 智能营养追踪架构
        </motion.p>
      </motion.div>

      {/* 架构图 */}
      <div className="flex items-center gap-6 relative z-10">
        {/* AR眼镜 */}
        <motion.div
          initial={{ x: -100, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          transition={{ delay: 0.3, type: "spring" }}
          className="relative"
        >
          <motion.div 
            className="flex flex-col items-center p-6 bg-gradient-to-br from-slate-800/80 to-slate-900/80 backdrop-blur-xl rounded-2xl border border-purple-500/30 w-52"
            whileHover={{ scale: 1.05, borderColor: 'rgba(168, 85, 247, 0.5)' }}
            animate={{ boxShadow: dataFlowStep === 0 ? '0 0 40px rgba(168, 85, 247, 0.4)' : '0 0 0px transparent' }}
          >
            <motion.div 
              className="bg-purple-500/20 p-5 rounded-full mb-4"
              animate={{ scale: [1, 1.1, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
            >
              <Glasses size={52} className="text-purple-400" />
            </motion.div>
            <h3 className="font-bold text-xl text-white">Rokid AR眼镜</h3>
            <p className="text-sm text-slate-400 text-center mt-2">实时拍摄 · 语音交互</p>
          </motion.div>
        </motion.div>

        {/* 连接线1 - 动态数据流 */}
        <div className="relative w-20 h-2">
          <div className="absolute inset-0 bg-slate-700 rounded-full"></div>
          <motion.div 
            className="absolute h-full bg-gradient-to-r from-purple-500 to-blue-500 rounded-full"
            animate={{ width: dataFlowStep >= 1 ? '100%' : '0%' }}
            transition={{ duration: 0.3 }}
          />
          <motion.div
            className="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-cyan-400 rounded-full shadow-[0_0_10px_#22d3ee]"
            animate={{ x: dataFlowStep === 1 ? [0, 80] : 0, opacity: dataFlowStep === 1 ? 1 : 0 }}
            transition={{ duration: 0.5 }}
          />
          <span className="absolute -bottom-5 left-1/2 -translate-x-1/2 text-xs text-slate-500">BLE</span>
        </div>

        {/* 手机App */}
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.5, type: "spring" }}
          className="relative"
        >
          <motion.div 
            className="flex flex-col items-center p-6 bg-gradient-to-br from-slate-800/80 to-slate-900/80 backdrop-blur-xl rounded-2xl border border-blue-500/30 w-52"
            whileHover={{ scale: 1.05, borderColor: 'rgba(59, 130, 246, 0.5)' }}
            animate={{ boxShadow: dataFlowStep === 1 || dataFlowStep === 2 ? '0 0 40px rgba(59, 130, 246, 0.4)' : '0 0 0px transparent' }}
          >
            <motion.div 
              className="bg-blue-500/20 p-5 rounded-full mb-4"
              animate={{ scale: [1, 1.1, 1] }}
              transition={{ duration: 2, repeat: Infinity, delay: 0.3 }}
            >
              <Smartphone size={52} className="text-blue-400" />
            </motion.div>
            <h3 className="font-bold text-xl text-white">手机应用</h3>
            <p className="text-sm text-slate-400 text-center mt-2">数据展示 · CXR SDK</p>
          </motion.div>
        </motion.div>

        {/* 连接线2 */}
        <div className="relative w-20 h-2">
          <div className="absolute inset-0 bg-slate-700 rounded-full"></div>
          <motion.div 
            className="absolute h-full bg-gradient-to-r from-blue-500 to-cyan-500 rounded-full"
            animate={{ width: dataFlowStep >= 2 ? '100%' : '0%' }}
            transition={{ duration: 0.3 }}
          />
          <motion.div
            className="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-cyan-400 rounded-full shadow-[0_0_10px_#22d3ee]"
            animate={{ x: dataFlowStep === 2 ? [0, 80] : 0, opacity: dataFlowStep === 2 ? 1 : 0 }}
            transition={{ duration: 0.5 }}
          />
          <span className="absolute -bottom-5 left-1/2 -translate-x-1/2 text-xs text-slate-500">HTTPS</span>
        </div>

        {/* 云端大脑 */}
        <motion.div
          initial={{ x: 100, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          transition={{ delay: 0.7, type: "spring" }}
          className="relative"
        >
          <motion.div 
            className="flex flex-col items-center p-6 bg-gradient-to-br from-slate-800/80 to-slate-900/80 backdrop-blur-xl rounded-2xl border border-cyan-500/50 w-52"
            whileHover={{ scale: 1.05 }}
            animate={{ 
              boxShadow: dataFlowStep === 3 ? '0 0 60px rgba(6, 182, 212, 0.5)' : '0 0 30px rgba(6, 182, 212, 0.2)',
              borderColor: dataFlowStep === 3 ? 'rgba(6, 182, 212, 0.8)' : 'rgba(6, 182, 212, 0.5)'
            }}
          >
            <motion.div 
              className="bg-cyan-500/20 p-5 rounded-full mb-4 relative"
              animate={{ scale: [1, 1.15, 1] }}
              transition={{ duration: 2, repeat: Infinity, delay: 0.6 }}
            >
              <Cloud size={52} className="text-cyan-400" />
              <motion.div
                className="absolute -top-1 -right-1"
                animate={{ rotate: 360 }}
                transition={{ duration: 3, repeat: Infinity, ease: "linear" }}
              >
                <Zap size={20} className="text-yellow-400" />
              </motion.div>
            </motion.div>
            <h3 className="font-bold text-xl text-white">云端 AI 大脑</h3>
            <p className="text-sm text-slate-400 text-center mt-2">Qwen-VL Max · 知识图谱</p>
          </motion.div>
        </motion.div>
      </div>

      {/* 底部数据库信息 */}
      <motion.div
         initial={{ opacity: 0, y: 30 }}
         animate={{ opacity: 1, y: 0 }}
         transition={{ delay: 1 }}
         className="mt-12 flex items-center gap-6"
      >
        <motion.div 
          className="flex items-center gap-3 text-slate-400 bg-slate-900/80 px-6 py-3 rounded-full border border-slate-700"
          whileHover={{ scale: 1.05, borderColor: 'rgba(34, 211, 238, 0.5)' }}
        >
          <Database size={20} className="text-cyan-400" />
          <span><span className="text-cyan-400 font-bold">6,300+</span> 食物数据库</span>
        </motion.div>
        <motion.div 
          className="flex items-center gap-3 text-slate-400 bg-slate-900/80 px-6 py-3 rounded-full border border-slate-700"
          whileHover={{ scale: 1.05, borderColor: 'rgba(34, 211, 238, 0.5)' }}
        >
          <span>🇨🇳 中国CDC + 🇺🇸 USDA</span>
        </motion.div>
      </motion.div>

      {/* 装饰网格 */}
      <div className="absolute inset-0 -z-10 h-full w-full bg-[linear-gradient(to_right,#80808008_1px,transparent_1px),linear-gradient(to_bottom,#80808008_1px,transparent_1px)] bg-[size:40px_40px]"></div>
    </div>
  );
};

export default IntroScene;
