import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Layers, AlertCircle, Utensils, ArrowUpCircle, Eye } from 'lucide-react';

const DynamicScene: React.FC = () => {
  const [phase, setPhase] = useState<'initial' | 'eating' | 'revealed'>('initial');

  // 自动播放动画 - 只播放一遍
  useEffect(() => {
    const timer1 = setTimeout(() => setPhase('eating'), 2000);
    const timer2 = setTimeout(() => setPhase('revealed'), 3500);
    
    return () => {
      clearTimeout(timer1);
      clearTimeout(timer2);
    };
  }, []);

  return (
    <div className="h-full flex flex-col items-center justify-center p-8">
      <motion.p 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="text-slate-400 mb-8 text-lg"
      >
        处理遮挡问题：当隐藏食物被发现时怎么办？
      </motion.p>

      <div className="flex items-center gap-20">
        
        {/* 碗的可视化 */}
        <motion.div 
          initial={{ x: -50, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          className="relative w-80"
        >
          <h3 className="text-center text-slate-300 mb-6 font-semibold text-lg">🍜 真实场景演示</h3>
          
          <div className="relative h-72 flex items-end justify-center">
            {/* 碗的底部 */}
            <div className="absolute bottom-0 w-64 h-36 bg-gradient-to-b from-slate-600 to-slate-700 rounded-b-[6rem] border-4 border-slate-500 shadow-2xl"></div>
            
            {/* 米饭（初始隐藏） */}
            <motion.div 
              className="absolute bottom-6 w-52 h-20 bg-gradient-to-br from-white to-gray-100 rounded-[3rem] flex items-center justify-center shadow-lg z-10 border-2 border-gray-200"
              animate={{ 
                boxShadow: phase === 'revealed' ? '0 0 30px rgba(255,255,255,0.5)' : '0 4px 6px rgba(0,0,0,0.1)'
              }}
            >
               <span className="text-slate-700 font-bold text-lg">🍚 米饭 (200g)</span>
            </motion.div>

            {/* 牛肉（覆盖米饭） */}
            <AnimatePresence>
                {(phase === 'initial' || phase === 'eating') && (
                    <motion.div 
                        initial={{ opacity: 1, y: 0, scale: 1 }}
                        animate={{ 
                          opacity: phase === 'eating' ? 0.6 : 1,
                          scale: phase === 'eating' ? 0.9 : 1,
                          y: phase === 'eating' ? -20 : 0
                        }}
                        exit={{ opacity: 0, y: -80, scale: 0.5, rotate: 10 }}
                        transition={{ duration: 1.2, ease: "easeOut" }}
                        className="absolute bottom-12 w-56 h-24 bg-gradient-to-br from-amber-600 to-amber-800 rounded-[4rem] flex items-center justify-center shadow-xl z-20 border-3 border-amber-900"
                    >
                        <span className="text-amber-100 font-bold text-lg">🥩 牛肉 (覆盖)</span>
                        {phase === 'initial' && (
                          <motion.div 
                            className="absolute -top-8 text-sm bg-cyan-500/90 px-4 py-2 rounded-full text-white font-medium shadow-lg"
                            animate={{ y: [0, -5, 0] }}
                            transition={{ duration: 1, repeat: Infinity }}
                          >
                            正在进食...
                          </motion.div>
                        )}
                    </motion.div>
                )}
            </AnimatePresence>

            {/* 发现标记 */}
            <AnimatePresence>
              {phase === 'revealed' && (
                <motion.div
                  initial={{ opacity: 0, scale: 0 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="absolute top-4 right-4 bg-yellow-500 text-yellow-900 px-3 py-1 rounded-full text-sm font-bold flex items-center gap-1"
                >
                  <Eye size={16} /> 发现新食物!
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </motion.div>

        {/* 系统逻辑 */}
        <motion.div 
          initial={{ x: 50, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="w-[420px] bg-gradient-to-br from-slate-800/80 to-slate-900/80 rounded-3xl p-8 border border-slate-700/50 min-h-[360px] relative backdrop-blur-xl"
        >
            <h3 className="text-center text-slate-200 mb-6 font-bold text-xl border-b border-slate-700 pb-4">🧠 系统处理逻辑</h3>

            <div className="space-y-4">
                {/* 初始状态 */}
                <motion.div 
                    animate={{ 
                      opacity: phase === 'initial' ? 1 : 0.5,
                      scale: phase === 'initial' ? 1 : 0.98
                    }}
                    className="flex justify-between items-center bg-slate-900/80 p-4 rounded-xl border border-slate-700"
                >
                    <div className="flex items-center gap-3">
                        <Layers size={22} className="text-blue-400"/>
                        <span className="font-medium">基线食物</span>
                    </div>
                    <span className="font-mono text-cyan-400">牛肉, 蔬菜</span>
                </motion.div>

                {/* 进食动作 */}
                <AnimatePresence>
                    {phase === 'eating' && (
                         <motion.div 
                            initial={{ opacity: 0, x: -30, height: 0 }}
                            animate={{ opacity: 1, x: 0, height: 'auto' }}
                            exit={{ opacity: 0, x: 30, height: 0 }}
                            className="flex items-center gap-3 text-yellow-300 bg-yellow-900/30 p-4 rounded-xl border border-yellow-500/30"
                        >
                            <motion.div
                              animate={{ rotate: [0, 15, -15, 0] }}
                              transition={{ duration: 0.5, repeat: Infinity }}
                            >
                              <Utensils size={22} />
                            </motion.div>
                            <span className="font-medium">用户正在吃牛肉...</span>
                        </motion.div>
                    )}
                </AnimatePresence>

                {/* 发现警报 */}
                <AnimatePresence>
                    {phase === 'revealed' && (
                        <motion.div 
                            initial={{ opacity: 0, scale: 0.8 }}
                            animate={{ opacity: 1, scale: 1 }}
                            className="bg-gradient-to-r from-red-900/40 to-orange-900/40 border border-red-500/50 text-red-100 p-5 rounded-xl"
                        >
                            <div className="flex items-center gap-3 mb-3">
                                <motion.div
                                  animate={{ scale: [1, 1.2, 1] }}
                                  transition={{ duration: 0.5, repeat: Infinity }}
                                >
                                  <AlertCircle className="text-red-400" size={24} />
                                </motion.div>
                                <span className="font-bold text-lg">⚠️ 检测到新食材！</span>
                            </div>
                            <p className="text-base">发现: <span className="font-bold text-yellow-300">米饭</span> （之前被遮挡）</p>
                        </motion.div>
                    )}
                </AnimatePresence>

                 {/* 更新操作 */}
                 <AnimatePresence>
                    {phase === 'revealed' && (
                        <motion.div 
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: 0.5 }}
                            className="flex items-center justify-between bg-gradient-to-r from-green-900/40 to-emerald-900/40 border border-green-500/50 p-5 rounded-xl"
                        >
                            <div className="flex items-center gap-3 text-green-300">
                                <motion.div
                                  animate={{ y: [0, -5, 0] }}
                                  transition={{ duration: 1, repeat: Infinity }}
                                >
                                  <ArrowUpCircle size={24} />
                                </motion.div>
                                <span className="font-bold">✅ 更新基线</span>
                            </div>
                            <div className="text-right">
                                <span className="block text-sm text-slate-400 line-through">355 kcal</span>
                                <motion.span 
                                  className="block font-bold text-2xl text-green-300"
                                  initial={{ scale: 1.3 }}
                                  animate={{ scale: 1 }}
                                >
                                  615 kcal
                                </motion.span>
                            </div>
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
        </motion.div>
      </div>
    </div>
  );
};

export default DynamicScene;
