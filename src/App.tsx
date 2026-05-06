// =====================================================
// Spidi Clicker v3.1 — App Entry Point
// =====================================================

import React, { useState, useEffect, useCallback } from 'react';
import { GameState, loadState, saveState } from './wintozo/game/gameState';
import { OOBEWintozo } from './wintozo/ui-wintozo/OOBE-wintozo-SpidiClicker';

// Lazy imports for device-specific game views
const GamePC = React.lazy(() =>
  import('./wintozo/game/game-pc').then(m => ({ default: m.GamePC }))
);
const AndroidGame = React.lazy(() =>
  import('./wintozo/game/android-game').then(m => ({ default: m.AndroidGame }))
);

// Loading screen
const LoadingScreen: React.FC = () => (
  <div
    className="fixed inset-0 flex flex-col items-center justify-center"
    style={{ background: 'linear-gradient(135deg, #e8f0fe 0%, #dbeafe 50%, #ede9fe 100%)' }}
  >
    <div className="flex flex-col items-center gap-6 animate-bounce-in">
      <div className="relative">
        <img
          src="https://i.ibb.co/x8tjVJBT/9a537f7d-5259-44cd-b818-dff5f504aca3.jpg"
          alt="Spidi Clicker"
          className="w-28 h-28 rounded-3xl shadow-2xl animate-float"
          style={{ border: '3px solid rgba(59,130,246,0.3)' }}
        />
        <div
          className="absolute -bottom-2 -right-2 w-8 h-8 rounded-full flex items-center justify-center text-sm font-black text-white shadow"
          style={{ background: 'linear-gradient(135deg,#3b82f6,#2563eb)' }}
        >
          3
        </div>
      </div>
      <div className="text-center">
        <h1 className="text-3xl font-black text-gray-800">Spidi Clicker</h1>
        <p className="text-gray-500 text-sm font-medium mt-1">v3.1 — Loading...</p>
      </div>
      <div className="flex gap-2">
        {[0, 1, 2].map(i => (
          <div
            key={i}
            className="w-2.5 h-2.5 rounded-full"
            style={{
              background: '#3b82f6',
              animation: `pulse-soft 1s ease-in-out ${i * 0.2}s infinite`,
            }}
          />
        ))}
      </div>
    </div>
  </div>
);

// Error Boundary
class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean; error?: Error }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          className="fixed inset-0 flex items-center justify-center p-6"
          style={{ background: '#f8fafc' }}
        >
          <div className="glass rounded-3xl p-8 max-w-md w-full text-center shadow-2xl">
            <div className="text-4xl mb-4">⚠️</div>
            <h2 className="text-xl font-black text-gray-800 mb-2">Что-то пошло не так</h2>
            <p className="text-gray-500 text-sm mb-4">{this.state.error?.message}</p>
            <button
              onClick={() => { localStorage.removeItem('spidi_clicker_v31_save'); window.location.reload(); }}
              className="px-6 py-3 rounded-2xl text-white font-bold"
              style={{ background: 'linear-gradient(135deg,#3b82f6,#2563eb)' }}
            >
              Сбросить и перезапустить
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

const App: React.FC = () => {
  const [gameState, setGameState] = useState<GameState>(() => loadState());
  const [isReady, setIsReady] = useState(false);

  // Mark ready after brief animation
  useEffect(() => {
    const timer = setTimeout(() => setIsReady(true), 800);
    return () => clearTimeout(timer);
  }, []);

  // Save on unmount
  useEffect(() => {
    return () => saveState(gameState);
  }, [gameState]);

  const handleStateChange = useCallback((updater: (s: GameState) => GameState) => {
    setGameState(prev => updater(prev));
  }, []);

  const handleOOBEComplete = useCallback((updates: Partial<GameState>) => {
    setGameState(prev => {
      const newState = { ...prev, ...updates };
      saveState(newState);
      return newState;
    });
  }, []);

  if (!isReady) {
    return <LoadingScreen />;
  }

  // OOBE first launch
  if (!gameState.completedOnboarding) {
    return (
      <ErrorBoundary>
        <OOBEWintozo onComplete={handleOOBEComplete} />
      </ErrorBoundary>
    );
  }

  // Detect device — use saved preference if set during OOBE
  const deviceType = gameState.deviceType;
  const isMobileDevice = deviceType === 'phone';
  const isTabletDevice = deviceType === 'tablet';
  const useMobileLayout = isMobileDevice || (isTabletDevice && window.innerWidth < 900);

  return (
    <ErrorBoundary>
      <React.Suspense fallback={<LoadingScreen />}>
        {useMobileLayout ? (
          <AndroidGame state={gameState} onStateChange={handleStateChange} />
        ) : (
          <GamePC state={gameState} onStateChange={handleStateChange} />
        )}
      </React.Suspense>
    </ErrorBoundary>
  );
};

export default App;
