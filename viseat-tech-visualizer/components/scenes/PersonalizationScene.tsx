import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Activity, HeartPulse, User, Target, Sparkles } from 'lucide-react';

const PersonalizationScene: React.FC = () => {
  const [food, setFood] = useState<'burger' | 'salad'>('burger');
  const [showRecommendation, setShowRecommendation] = useState(false);
  const [switched, setSwitched] = useState(false);

  // 自动切换食物 - 只切换一次
  useEffect(() => {
    // 初始显示
    const showTimer = setTimeout(() => setShowRecommendation(true), 500);
    
    // 3秒后切换一次
    const switchTimer = setTimeout(() => {
      if (!switched) {
        setShowRecommendation(false);
        setTimeout(() => {
          setFood('salad');
          setSwitched(true);
          setTimeout(() => setShowRecommendation(true), 500);
        }, 300);
      }
    }, 3500);
    
    return () => {
      clearTimeout(showTimer);
      clearTimeout(switchTimer);
    };
  }, [switched]);

  const personas = [
    {
      id: 'A',
      name: '用户A',
      condition: '糖尿病 + 高血压',
      icon: HeartPulse,
      iconBg: 'bg-red-500/20',
      iconColor: 'text-red-400',
      age: 55,
      goal: '控制血糖',
      borderColor: 'border-red-500/30',
      recommendations: {
        burger: {
          type: 'warning',
          emoji: '⚠️',
          title: '高风险警告',
          content: '高钠高脂肪！超出每日钠摄入量40%。建议去掉培根和酱料，或更换为沙拉。',
          color: 'text-red-300',
          bgColor: 'from-red-900/40 to-orange-900/30',
        },
        salad: {
          type: 'success',
          emoji: '✅',
          title: '优秀选择',
          content: '低升糖指数，膜食纤维有助于稳定血糖水平。非常适合您的健康目标！',
          color: 'text-green-300',
          bgColor: 'from-green-900/40 to-emerald-900/30',
        }
      }
    },
    {
      id: 'B',
      name: '用户B',
      condition: '运动员（增肌期）',
      icon: Activity,
      iconBg: 'bg-blue-500/20',
      iconColor: 'text-blue-400',
      age: 24,
      goal: '增加肌肉',
      borderColor: 'border-blue-500/30',
      recommendations: {
        burger: {
          type: 'good',
          emoji: '💪',
          title: '不错的选择',
          content: '优质蛋白质(35g)，符合训练后热量需求。建议搭配水果补充微量元素。',
          color: 'text-blue-300',
          bgColor: 'from-blue-900/40 to-cyan-900/30',
        },
        salad: {
          type: 'info',
          emoji: 'ℹ️',
          title: '热量不足',
          content: '健康但热量不足。建议加双份鸡胸或蛋白粉来满足增肌目标。',
          color: 'text-yellow-300',
          bgColor: 'from-yellow-900/40 to-amber-900/30',
        }
      }
    }
  ];

  return (
    <div className="h-full flex flex-col items-center justify-center p-8">
      <motion.p 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="text-slate-400 mb-6 text-lg"
      >
        基于用户健康档案的 <span className="text-cyan-400 font-bold">个性化建议</span>
      </motion.p>

      {/* 食物选择器 */}
      <motion.div 
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="mb-8 flex items-center gap-6"
      >
        <span className="text-slate-500">当前食物：</span>
        <div className="flex bg-slate-800/80 p-1.5 rounded-xl border border-slate-700">
          <motion.div
            className={`px-6 py-3 rounded-lg text-lg font-bold flex items-center gap-2 transition-all ${
              food === 'burger' 
                ? 'bg-gradient-to-r from-amber-600 to-orange-600 text-white shadow-lg' 
                : 'text-slate-500'
            }`}
            animate={{ scale: food === 'burger' ? 1 : 0.95 }}
          >
            🍔 汉堡
          </motion.div>
          <motion.div
            className={`px-6 py-3 rounded-lg text-lg font-bold flex items-center gap-2 transition-all ${
              food === 'salad' 
                ? 'bg-gradient-to-r from-green-600 to-emerald-600 text-white shadow-lg' 
                : 'text-slate-500'
            }`}
            animate={{ scale: food === 'salad' ? 1 : 0.95 }}
          >
            🥗 沙拉
          </motion.div>
        </div>
        <motion.div
          animate={{ rotate: [0, 360] }}
          transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
        >
          <Sparkles className="text-cyan-400" size={20} />
        </motion.div>
      </motion.div>

      <div className="flex gap-10 w-full max-w-6xl justify-center">
        {personas.map((persona, index) => (
          <motion.div 
            key={persona.id}
            initial={{ x: index === 0 ? -50 : 50, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            transition={{ delay: index * 0.2 }}
            className={`w-1/2 bg-gradient-to-br from-slate-800/70 to-slate-900/70 rounded-3xl p-6 border ${persona.borderColor} backdrop-blur-xl`}
          >
            {/* 用户信息 */}
            <div className="flex items-center gap-4 mb-6 border-b border-slate-700/50 pb-5">
              <motion.div 
                className={`${persona.iconBg} p-4 rounded-2xl`}
                animate={{ scale: [1, 1.1, 1] }}
                transition={{ duration: 2, repeat: Infinity }}
              >
                <persona.icon className={persona.iconColor} size={28} />
              </motion.div>
              <div className="flex-1">
                <h3 className="text-xl font-bold text-white flex items-center gap-2">
                  <User size={18} className="text-slate-500" />
                  {persona.name}: {persona.condition}
                </h3>
                <div className="flex items-center gap-4 mt-1">
                  <span className="text-sm text-slate-400">年龄: {persona.age}岁</span>
                  <span className="text-sm text-slate-400 flex items-center gap-1">
                    <Target size={14} className="text-cyan-400" />
                    目标: {persona.goal}
                  </span>
                </div>
              </div>
            </div>

            {/* AI建议 */}
            <AnimatePresence mode="wait">
              {showRecommendation && (
                <motion.div 
                  key={food + persona.id}
                  initial={{ opacity: 0, y: 20, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -20, scale: 0.95 }}
                  transition={{ duration: 0.4 }}
                  className={`bg-gradient-to-br ${persona.recommendations[food].bgColor} rounded-2xl p-5 border border-slate-700/50 min-h-[140px]`}
                >
                  <div className="flex items-center gap-2 mb-3">
                    <span className="text-2xl">{persona.recommendations[food].emoji}</span>
                    <span className={`font-bold text-lg ${persona.recommendations[food].color}`}>
                      {persona.recommendations[food].title}
                    </span>
                  </div>
                  <motion.p 
                    className={`${persona.recommendations[food].color} leading-relaxed`}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.2 }}
                  >
                    {persona.recommendations[food].content}
                  </motion.p>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        ))}
      </div>

      {/* 底部说明 */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5 }}
        className="mt-8 text-center text-slate-500 text-sm"
      >
        🧠 AI 根据用户健康档案自动生成个性化建议
      </motion.div>
    </div>
  );
};

export default PersonalizationScene;