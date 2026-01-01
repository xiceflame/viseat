import React from 'react';
import { motion } from 'framer-motion';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { SceneType } from '../types';

interface SceneControllerProps {
  currentScene: SceneType;
  onNext: () => void;
  onPrev: () => void;
  sceneOrder: SceneType[];
  sceneTitles: Record<SceneType, string>;
}

const SceneController: React.FC<SceneControllerProps> = ({ currentScene, onNext, onPrev, sceneOrder, sceneTitles }) => {
  const currentIndex = sceneOrder.indexOf(currentScene);

  return (
    <motion.div 
      initial={{ opacity: 0, y: 50 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.5 }}
      className="fixed bottom-8 left-1/2 transform -translate-x-1/2 flex items-center gap-4 bg-slate-900/95 backdrop-blur-xl px-6 py-4 rounded-2xl border border-slate-700/50 shadow-2xl z-50"
    >
      <button 
        onClick={onPrev}
        className="p-2 rounded-full hover:bg-slate-700 transition-colors text-slate-400 hover:text-white"
      >
        <ChevronLeft size={24} />
      </button>

      <div className="flex gap-3">
        {sceneOrder.map((scene, idx) => (
          <motion.button
            key={scene}
            onClick={() => {}}
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.95 }}
            className="relative group"
          >
            <div 
              className={`h-2 rounded-full transition-all duration-500 ${
                idx === currentIndex 
                  ? 'w-12 bg-gradient-to-r from-cyan-400 to-blue-500' 
                  : idx < currentIndex 
                    ? 'w-2 bg-cyan-400/50' 
                    : 'w-2 bg-slate-600'
              }`}
            />
            {/* 悬浮提示 */}
            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-2 py-1 bg-slate-800 rounded text-xs whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
              {sceneTitles[scene]}
            </div>
          </motion.button>
        ))}
      </div>

      <button 
        onClick={onNext}
        className="p-2 rounded-full hover:bg-slate-700 transition-colors text-slate-400 hover:text-white"
      >
        <ChevronRight size={24} />
      </button>
    </motion.div>
  );
};

export default SceneController;
