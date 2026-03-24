import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Search, Database, Globe, BrainCircuit, Check, Sparkles } from 'lucide-react';

const FusionScene: React.FC = () => {
  const [activeStep, setActiveStep] = useState(0);

  // 只播放一遍
  useEffect(() => {
    if (activeStep < 4) {
      const timer = setTimeout(() => {
        setActiveStep(prev => prev + 1);
      }, 1200);
      return () => clearTimeout(timer);
    }
  }, [activeStep]);

  const steps = [
    { id: 1, title: '精确匹配', subtitle: '名称直接查找', icon: Search, color: 'text-blue-400', bgColor: 'from-blue-500/20 to-blue-600/10' },
    { id: 2, title: '中国CDC库', subtitle: '本土食物数据', icon: Database, color: 'text-red-400', bgColor: 'from-red-500/20 to-red-600/10' },
    { id: 3, title: 'USDA数据库', subtitle: '国际标准数据', icon: Globe, color: 'text-green-400', bgColor: 'from-green-500/20 to-green-600/10' },
    { id: 4, title: 'AI智能估算', subtitle: '深度学习推理', icon: BrainCircuit, color: 'text-purple-400', bgColor: 'from-purple-500/20 to-purple-600/10' },
  ];

  return (
    <div className="h-full flex flex-col items-center justify-center p-8">
      <motion.p 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="text-slate-400 mb-10 text-lg"
      >
        智能决策引擎，覆盖 <span className="text-cyan-400 font-bold">95%</span> 食物种类
      </motion.p>

      <div className="flex gap-6 w-full max-w-6xl items-center justify-center relative">
        
        {/* 输入 */}
        <motion.div 
          initial={{ x: -30, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          className="flex flex-col items-center"
        >
            <motion.div 
              className="w-28 h-28 rounded-2xl bg-gradient-to-br from-slate-800 to-slate-900 border-2 border-cyan-500/50 flex items-center justify-center mb-4 shadow-[0_0_30px_rgba(6,182,212,0.2)]"
              animate={{ borderColor: ['rgba(6,182,212,0.5)', 'rgba(6,182,212,0.8)', 'rgba(6,182,212,0.5)'] }}
              transition={{ duration: 2, repeat: Infinity }}
            >
                <span className="text-5xl">🥗</span>
            </motion.div>
            <div className="text-center">
              <span className="font-bold text-white block">输入图像</span>
              <span className="font-mono text-cyan-400 text-sm">"绿色沙拉碗"</span>
            </div>
        </motion.div>

        {/* 箭头 */}
        <motion.div
          animate={{ x: [0, 5, 0] }}
          transition={{ duration: 1, repeat: Infinity }}
          className="text-3xl text-slate-600"
        >
          ➜
        </motion.div>

        {/* 处理管道 */}
        <div className="flex items-center gap-4 relative">
            {steps.map((step, index) => {
                const isActive = activeStep === index + 1;
                const isPassed = activeStep > index + 1;
                
                return (
                    <motion.div 
                      key={step.id} 
                      className="relative flex flex-col items-center"
                      initial={{ y: 20, opacity: 0 }}
                      animate={{ y: 0, opacity: 1 }}
                      transition={{ delay: index * 0.1 }}
                    >
                        <motion.div 
                            animate={{ 
                                scale: isActive ? 1.15 : 1,
                                boxShadow: isActive ? '0 0 40px rgba(255,255,255,0.3)' : isPassed ? '0 0 20px rgba(74,222,128,0.3)' : 'none'
                            }}
                            transition={{ duration: 0.3 }}
                            className={`w-20 h-20 rounded-2xl border-2 flex items-center justify-center z-10 transition-all duration-300 bg-gradient-to-br ${
                              isActive ? `${step.bgColor} border-white` : 
                              isPassed ? 'from-green-900/30 to-green-800/20 border-green-500' : 
                              'from-slate-800 to-slate-900 border-slate-700'
                            }`}
                        >
                           {isPassed ? (
                             <Check className="text-green-400" size={32} />
                           ) : (
                             <step.icon className={`${isActive ? step.color : 'text-slate-600'}`} size={32} />
                           )}
                        </motion.div>

                        <div className="mt-3 text-center w-24">
                            <h4 className={`text-sm font-bold transition-colors ${isActive ? 'text-white' : isPassed ? 'text-green-400' : 'text-slate-500'}`}>
                              {step.title}
                            </h4>
                            {isActive && (
                                <motion.div 
                                    initial={{ opacity: 0, y: -5 }} 
                                    animate={{ opacity: 1, y: 0 }}
                                    className="text-xs text-cyan-400 mt-1 flex items-center justify-center gap-1"
                                >
                                    <motion.span
                                      animate={{ opacity: [1, 0.5, 1] }}
                                      transition={{ duration: 0.5, repeat: Infinity }}
                                    >
                                      ⚡
                                    </motion.span>
                                    查询中...
                                </motion.div>
                            )}
                            {isPassed && (
                              <div className="text-xs text-green-400/70 mt-1">✓ 已完成</div>
                            )}
                        </div>

                         {/* 连接线 */}
                         {index < steps.length - 1 && (
                           <div className="absolute top-10 left-full w-4 h-0.5 bg-slate-700">
                             <motion.div 
                               className="h-full bg-cyan-400"
                               animate={{ width: isPassed || isActive ? '100%' : '0%' }}
                               transition={{ duration: 0.3 }}
                             />
                           </div>
                         )}
                    </motion.div>
                );
            })}
        </div>

        {/* 箭头 */}
        <motion.div
          animate={{ x: [0, 5, 0] }}
          transition={{ duration: 1, repeat: Infinity }}
          className="text-3xl text-slate-600"
        >
          ➜
        </motion.div>

        {/* 输出 */}
        <motion.div 
          initial={{ x: 30, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          className="flex flex-col items-center"
        >
             <motion.div 
                animate={{ 
                    scale: activeStep === 4 ? [1, 1.1, 1] : 1,
                    boxShadow: activeStep === 4 ? "0 0 50px rgba(34,197,94,0.5)" : "none"
                }}
                transition={{ duration: 0.5 }}
                className={`w-28 h-28 rounded-2xl border-2 flex flex-col items-center justify-center mb-4 transition-all duration-500 ${
                  activeStep === 4 
                    ? 'bg-gradient-to-br from-green-900/40 to-emerald-900/40 border-green-400' 
                    : 'bg-gradient-to-br from-slate-800 to-slate-900 border-slate-700'
                }`}
             >
                {activeStep === 4 ? (
                  <>
                    <Sparkles className="text-green-400 mb-1" size={28} />
                    <span className="text-green-300 font-bold text-lg">450</span>
                    <span className="text-green-400/70 text-xs">kcal</span>
                  </>
                ) : (
                  <motion.div 
                    className="text-slate-600 text-2xl"
                    animate={{ opacity: [0.3, 0.7, 0.3] }}
                    transition={{ duration: 1, repeat: Infinity }}
                  >
                    ...
                  </motion.div>
                )}
             </motion.div>
            <div className="text-center">
              <span className={`font-bold block transition-colors ${activeStep === 4 ? 'text-green-400' : 'text-slate-500'}`}>
                {activeStep === 4 ? '✅ 识别成功' : '等待结果...'}
              </span>
              {activeStep === 4 && (
                <motion.span 
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="text-xs text-slate-400"
                >
                  精度: 98.2%
                </motion.span>
              )}
            </div>
        </motion.div>
      </div>

      {/* 底部说明 */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
        className="mt-12 flex gap-6"
      >
        {[
          { emoji: '🇨🇳', label: '中国CDC', count: '2,800+' },
          { emoji: '🇺🇸', label: 'USDA', count: '3,500+' },
          { emoji: '🤖', label: 'AI补充', count: '无限' },
        ].map((item, i) => (
          <motion.div
            key={i}
            className="flex items-center gap-2 bg-slate-800/50 px-4 py-2 rounded-full border border-slate-700"
            whileHover={{ scale: 1.05, borderColor: 'rgba(34, 211, 238, 0.5)' }}
          >
            <span>{item.emoji}</span>
            <span className="text-slate-400 text-sm">{item.label}</span>
            <span className="text-cyan-400 font-bold text-sm">{item.count}</span>
          </motion.div>
        ))}
      </motion.div>
    </div>
  );
};

export default FusionScene;
