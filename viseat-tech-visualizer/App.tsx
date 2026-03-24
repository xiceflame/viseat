import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { SceneType } from './types';
import IntroScene from './components/scenes/IntroScene';
import DifferentialScene from './components/scenes/DifferentialScene';
import DynamicScene from './components/scenes/DynamicScene';
import FusionScene from './components/scenes/FusionScene';
import PersonalizationScene from './components/scenes/PersonalizationScene';
import SceneController from './components/SceneController';

const sceneOrder = [
  SceneType.INTRO,
  SceneType.DIFFERENTIAL,
  SceneType.DYNAMIC,
  SceneType.FUSION,
  SceneType.PERSONALIZATION
];

const sceneTitles: Record<SceneType, string> = {
  [SceneType.INTRO]: '系统架构总览',
  [SceneType.DIFFERENTIAL]: '创新一：基线差分追踪',
  [SceneType.DYNAMIC]: '创新二：动态基线更新',
  [SceneType.FUSION]: '创新三：多源数据融合',
  [SceneType.PERSONALIZATION]: '创新四：个性化健康引擎',
};

const SCENE_DURATION = 8000; // 每个场景8秒

const App: React.FC = () => {
  const [currentScene, setCurrentScene] = useState<SceneType>(SceneType.INTRO);
  const [isAutoPlay, setIsAutoPlay] = useState(true);
  const [progress, setProgress] = useState(0);

  const [isFinished, setIsFinished] = useState(false);

  const handleNext = useCallback(() => {
    const currentIndex = sceneOrder.indexOf(currentScene);
    if (currentIndex < sceneOrder.length - 1) {
      setCurrentScene(sceneOrder[currentIndex + 1]);
      setProgress(0);
    } else {
      // 播放结束，停止自动播放
      setIsAutoPlay(false);
      setIsFinished(true);
      setProgress(100);
    }
  }, [currentScene]);

  const handlePrev = () => {
    const currentIndex = sceneOrder.indexOf(currentScene);
    if (currentIndex > 0) {
      setCurrentScene(sceneOrder[currentIndex - 1]);
      setProgress(0);
    }
  };

  // 自动播放逻辑
  useEffect(() => {
    if (!isAutoPlay) return;
    
    const progressInterval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 100) {
          handleNext();
          return 0;
        }
        return prev + (100 / (SCENE_DURATION / 50));
      });
    }, 50);

    return () => clearInterval(progressInterval);
  }, [isAutoPlay, handleNext]);

  const renderScene = () => {
    switch (currentScene) {
      case SceneType.INTRO: return <IntroScene />;
      case SceneType.DIFFERENTIAL: return <DifferentialScene />;
      case SceneType.DYNAMIC: return <DynamicScene />;
      case SceneType.FUSION: return <FusionScene />;
      case SceneType.PERSONALIZATION: return <PersonalizationScene />;
      default: return <IntroScene />;
    }
  };

  const currentIndex = sceneOrder.indexOf(currentScene);

  return (
    <div className="w-full h-screen bg-slate-900 text-white overflow-hidden relative selection:bg-cyan-500 selection:text-black">
        {/* 背景光效 */}
        <div className="absolute top-[-20%] left-[-10%] w-[60%] h-[60%] bg-blue-600/20 blur-[150px] rounded-full pointer-events-none" />
        <div className="absolute bottom-[-20%] right-[-10%] w-[60%] h-[60%] bg-purple-600/20 blur-[150px] rounded-full pointer-events-none" />

        <div className="relative z-10 w-full h-full flex flex-col">
            {/* 顶部信息栏 */}
            <header className="absolute top-0 left-0 w-full p-6 flex justify-between items-center z-20">
                <motion.div 
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  className="flex items-center gap-4"
                >
                  <div className="text-2xl font-extrabold tracking-tight">
                    VisEat<span className="text-cyan-400">.cn</span>
                  </div>
                  <div className="h-6 w-px bg-slate-700"></div>
                  <div className="text-sm text-slate-400">智能营养追踪系统</div>
                </motion.div>
                <div className="flex items-center gap-4">
                  <motion.div 
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="text-sm text-cyan-400 font-mono bg-cyan-400/10 px-3 py-1 rounded-full"
                  >
                    {String(currentIndex + 1).padStart(2, '0')} / {String(sceneOrder.length).padStart(2, '0')}
                  </motion.div>
                  <button
                    onClick={() => setIsAutoPlay(!isAutoPlay)}
                    className={`px-4 py-1.5 rounded-full text-sm font-medium transition-all ${
                      isAutoPlay 
                        ? 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/50' 
                        : 'bg-slate-800 text-slate-400 border border-slate-700'
                    }`}
                  >
                    {isAutoPlay ? '⏸ 暂停' : '▶ 播放'}
                  </button>
                </div>
            </header>

            {/* 场景标题 */}
            <div className="absolute top-20 left-0 w-full flex justify-center z-20">
              <AnimatePresence mode="wait">
                <motion.div
                  key={currentScene}
                  initial={{ opacity: 0, y: -20 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: 20 }}
                  transition={{ duration: 0.5 }}
                  className="text-center"
                >
                  <h2 className="text-3xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500">
                    {sceneTitles[currentScene]}
                  </h2>
                </motion.div>
              </AnimatePresence>
            </div>

            <main className="flex-1 w-full h-full pt-16">
                <AnimatePresence mode="wait">
                    <motion.div
                        key={currentScene}
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 1.05 }}
                        transition={{ duration: 0.6, ease: "easeOut" }}
                        className="w-full h-full"
                    >
                        {renderScene()}
                    </motion.div>
                </AnimatePresence>
            </main>

            {/* 底部进度条 */}
            <div className="absolute bottom-0 left-0 w-full h-1 bg-slate-800">
              <motion.div 
                className="h-full bg-gradient-to-r from-cyan-500 to-blue-500"
                style={{ width: `${progress}%` }}
                transition={{ duration: 0.05 }}
              />
            </div>

            <SceneController 
                currentScene={currentScene} 
                onNext={handleNext} 
                onPrev={handlePrev} 
                sceneOrder={sceneOrder}
                sceneTitles={sceneTitles}
            />
        </div>
    </div>
  );
};

export default App;
