import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { RefreshCcw, Lock, Calculator, AlertTriangle, CheckCircle2, Camera } from 'lucide-react';

const DifferentialScene: React.FC = () => {
  const [step, setStep] = useState(0);
  const [traditionalVal, setTraditionalVal] = useState(150);
  
  // 传统方法的数据抖动
  useEffect(() => {
    const interval = setInterval(() => {
      setTraditionalVal(Math.floor(Math.random() * (170 - 130 + 1) + 130));
    }, 400);
    return () => clearInterval(interval);
  }, []);

  // 自动播放步骤 - 只播放一遍
  useEffect(() => {
    if (step < 3) {
      const timer = setTimeout(() => {
        setStep(prev => prev + 1);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [step]);

  const baseline = 150;
  const currentResidue = step >= 2 ? 90 : 150;
  const consumed = baseline - currentResidue;

  return (
    <div className="h-full flex flex-col items-center justify-center p-8">
      <motion.p 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="text-slate-400 mb-8 text-lg"
      >
        "一次锁定，持续追踪" vs "重复识别"
      </motion.p>

      <div className="grid grid-cols-2 gap-16 w-full max-w-6xl">
        {/* 传统方法 */}
        <motion.div 
          initial={{ x: -50, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="bg-gradient-to-br from-slate-800/60 to-red-900/20 p-8 rounded-3xl border border-red-500/30 relative overflow-hidden"
        >
          <motion.div 
            className="absolute top-0 right-0 bg-red-500/30 text-red-200 px-5 py-2 text-sm font-bold rounded-bl-2xl"
            animate={{ opacity: [0.7, 1, 0.7] }}
            transition={{ duration: 2, repeat: Infinity }}
          >
            ❌ 传统方案
          </motion.div>
          
          <div className="flex flex-col items-center h-full">
            <div className="text-center mb-6">
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
              >
                <RefreshCcw className="w-16 h-16 text-red-400 mx-auto mb-3" />
              </motion.div>
              <h3 className="text-2xl font-bold text-red-200">每帧重新计算</h3>
              <p className="text-sm text-slate-500 mt-1">每次都要重新识别食物</p>
            </div>
            
            <div className="flex flex-col gap-3 w-full">
              {[1, 2, 3].map((i) => (
                <motion.div 
                  key={i} 
                  className="bg-slate-900/80 p-4 rounded-xl flex justify-between items-center border border-red-500/20"
                  animate={{ borderColor: ['rgba(239,68,68,0.2)', 'rgba(239,68,68,0.5)', 'rgba(239,68,68,0.2)'] }}
                  transition={{ duration: 1, repeat: Infinity, delay: i * 0.2 }}
                >
                   <div className="flex items-center gap-3">
                     <Camera className="text-slate-600" size={20} />
                     <span className="text-slate-500 text-sm">扫描 {i}</span>
                   </div>
                   <div className="text-right">
                      <motion.div 
                        className="text-2xl font-mono text-red-400 font-bold"
                        key={traditionalVal + i}
                        initial={{ scale: 1.2 }}
                        animate={{ scale: 1 }}
                      >
                        {traditionalVal + (i * 3 - 6)}g
                      </motion.div>
                      <div className="text-xs text-red-400/60">"牛肉" / "牵排" / "肉类"</div>
                   </div>
                </motion.div>
              ))}
            </div>

            <motion.div 
              className="mt-6 flex items-center gap-3 text-red-300 bg-red-900/30 px-5 py-3 rounded-xl border border-red-500/30"
              animate={{ scale: [1, 1.02, 1] }}
              transition={{ duration: 1.5, repeat: Infinity }}
            >
              <AlertTriangle size={24} />
              <div>
                <div className="font-bold">误差累积</div>
                <div className="text-xs text-red-400/70">结果不稳定，波动大</div>
              </div>
            </motion.div>
          </div>
        </motion.div>

        {/* VisEat 方法 */}
        <motion.div 
          initial={{ x: 50, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="bg-gradient-to-br from-slate-800/60 to-green-900/20 p-8 rounded-3xl border border-green-500/40 relative overflow-hidden shadow-[0_0_60px_rgba(34,197,94,0.15)]"
        >
          <motion.div 
            className="absolute top-0 right-0 bg-green-500/30 text-green-200 px-5 py-2 text-sm font-bold rounded-bl-2xl"
            animate={{ opacity: [0.7, 1, 0.7] }}
            transition={{ duration: 2, repeat: Infinity }}
          >
            ✅ VisEat 创新
          </motion.div>
          
          <div className="flex flex-col h-full">
            <div className="flex items-center gap-3 mb-6">
              <motion.div
                animate={{ scale: [1, 1.1, 1] }}
                transition={{ duration: 2, repeat: Infinity }}
              >
                <Lock className="text-green-400" size={28} />
              </motion.div>
              <div>
                <h3 className="text-2xl font-bold text-white">差分追踪引擎</h3>
                <p className="text-sm text-slate-500">基线锁定 + 实时差分</p>
              </div>
            </div>

            {/* 步骤指示器 */}
            <div className="flex gap-2 mb-6">
              {['建立基线', '用户进食', '计算差分'].map((label, idx) => (
                <motion.div
                  key={idx}
                  className={`flex-1 py-2 px-3 rounded-lg text-center text-sm font-medium transition-all ${
                    step > idx ? 'bg-green-500/30 text-green-300 border border-green-500/50' :
                    step === idx ? 'bg-cyan-500/30 text-cyan-300 border border-cyan-500/50' :
                    'bg-slate-800 text-slate-500 border border-slate-700'
                  }`}
                  animate={step === idx ? { scale: [1, 1.05, 1] } : {}}
                  transition={{ duration: 0.5, repeat: step === idx ? Infinity : 0 }}
                >
                  {label}
                </motion.div>
              ))}
            </div>

            <div className="flex-1 flex flex-col justify-center gap-4">
                {/* 基线条 */}
                <div className="relative">
                    <div className="flex justify-between text-sm mb-2 text-slate-400">
                        <span className="flex items-center gap-2">
                          <Lock size={14} className="text-blue-400" />
                          基线快照
                        </span>
                        <span className="font-mono text-blue-400">{baseline}g</span>
                    </div>
                    <div className="h-14 w-full bg-slate-700/50 rounded-xl overflow-hidden relative border border-slate-600">
                        <motion.div 
                            initial={{ width: 0 }}
                            animate={{ width: "100%" }}
                            transition={{ duration: 1 }}
                            className="h-full bg-gradient-to-r from-blue-600/60 to-blue-500/60"
                        />
                        <motion.div 
                          className="absolute inset-0 flex items-center justify-center text-blue-200 font-bold tracking-widest text-lg"
                          animate={{ opacity: [0.3, 0.6, 0.3] }}
                          transition={{ duration: 2, repeat: Infinity }}
                        >
                          🔒 已锁定
                        </motion.div>
                    </div>
                </div>

                {/* 减法符号 */}
                <AnimatePresence>
                    {step >= 2 && (
                        <motion.div 
                            initial={{ opacity: 0, scale: 0 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0 }}
                            className="flex justify-center"
                        >
                            <span className="text-4xl font-bold text-cyan-400">−</span>
                        </motion.div>
                    )}
                </AnimatePresence>

                {/* 当前剩余 */}
                <div className="relative">
                    <div className="flex justify-between text-sm mb-2 text-slate-400">
                        <span>当前剩余（实时）</span>
                        <motion.span 
                          className="font-mono"
                          key={currentResidue}
                          initial={{ color: '#22d3ee' }}
                          animate={{ color: '#94a3b8' }}
                        >
                          {currentResidue}g
                        </motion.span>
                    </div>
                    <div className="h-14 w-full bg-slate-700/50 rounded-xl overflow-hidden border border-slate-600">
                        <motion.div 
                            animate={{ width: `${(currentResidue / baseline) * 100}%` }}
                            transition={{ duration: 0.8, type: "spring" }}
                            className="h-full bg-gradient-to-r from-blue-500 to-cyan-500"
                        />
                    </div>
                </div>

                <div className="h-px bg-gradient-to-r from-transparent via-slate-600 to-transparent my-2"></div>

                {/* 结果 */}
                <motion.div 
                  className="bg-slate-900/80 p-5 rounded-2xl flex items-center justify-between border border-green-500/40"
                  animate={{ boxShadow: step >= 2 ? '0 0 30px rgba(34, 197, 94, 0.2)' : 'none' }}
                >
                    <div className="flex items-center gap-3">
                        <Calculator className="text-green-400" size={28} />
                        <span className="text-slate-300 text-lg">已摄入量</span>
                    </div>
                    <div className="text-right">
                        <motion.span 
                             key={consumed}
                             initial={{ scale: 1.5, color: '#4ade80' }}
                             animate={{ scale: 1, color: '#ffffff' }}
                             className="text-4xl font-black font-mono block"
                        >
                            {consumed}g
                        </motion.span>
                        <span className="text-green-400 text-sm flex items-center justify-end gap-1 mt-1">
                            <CheckCircle2 size={16} /> 精度 99.5%
                        </span>
                    </div>
                </motion.div>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
};

export default DifferentialScene;
