// =====================================================
// Spidi Clicker v3.1 — PC & Tablet Layout
// =====================================================

import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  GameState, ICON_URLS, LOGO_URL, MUSIC_TRACKS, WALLPAPERS,
  formatNumber, saveState, getTodayStr, recalculateStats, formatTimeRemaining,
} from './gameState';
import { audioManager } from './audioManager';
import { OOBEReset } from '../ui-wintozo/oobespidiOS-qwerty';

interface GamePCProps {
  state: GameState;
  onStateChange: (updater: (s: GameState) => GameState) => void;
}

type Tab = 'game' | 'upgrades' | 'gifts' | 'settings';

interface FloatCoin {
  id: number;
  x: number;
  y: number;
  value: number;
}

export const GamePC: React.FC<GamePCProps> = ({ state, onStateChange }) => {
  const [activeTab, setActiveTab] = useState<Tab>('game');
  const [floatCoins, setFloatCoins] = useState<FloatCoin[]>([]);
  const [clickAnimation, setClickAnimation] = useState(false);
  const [showResetOOBE, setShowResetOOBE] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' | 'info' } | null>(null);
  const [medalShown, setMedalShown] = useState(false);
  const coinIdRef = useRef(0);
  const autoClickRef = useRef<NodeJS.Timeout | null>(null);
  const saveRef = useRef<NodeJS.Timeout | null>(null);
  const recalcRef = useRef<NodeJS.Timeout | null>(null);
  const customMusicInputRef = useRef<HTMLInputElement>(null);
  const wallpaperInputRef = useRef<HTMLInputElement>(null);

  // Auto save every 5s
  useEffect(() => {
    saveRef.current = setInterval(() => {
      onStateChange(s => { saveState(s); return s; });
    }, 5000);
    return () => { if (saveRef.current) clearInterval(saveRef.current); };
  }, [onStateChange]);

  // Recalculate upgrades every second
  useEffect(() => {
    recalcRef.current = setInterval(() => {
      onStateChange(s => recalculateStats(s));
    }, 1000);
    return () => { if (recalcRef.current) clearInterval(recalcRef.current); };
  }, [onStateChange]);

  // Auto clicker
  useEffect(() => {
    if (autoClickRef.current) clearInterval(autoClickRef.current);
    if (state.autoClickPerSecond > 0) {
      autoClickRef.current = setInterval(() => {
        onStateChange(s => {
          const earned = s.autoClickPerSecond * s.clickPower;
          const newCoins = s.coins + earned;
          const newTotal = s.totalCoins + earned;
          const newTotalClicks = s.totalClicks + s.autoClickPerSecond;
          const medal = !s.medal100k && newTotalClicks >= 100000;
          if (medal) setMedalShown(true);
          return {
            ...s,
            coins: newCoins,
            totalCoins: newTotal,
            totalClicks: newTotalClicks,
            medal100k: s.medal100k || medal,
          };
        });
      }, 1000);
    }
    return () => { if (autoClickRef.current) clearInterval(autoClickRef.current); };
  }, [state.autoClickPerSecond, state.clickPower, onStateChange]);

  // Audio
  useEffect(() => {
    audioManager.init(state.selectedMusic, state.musicEnabled, state.musicVolume, state.customMusic);
  }, [state.selectedMusic, state.musicEnabled, state.musicVolume, state.customMusic]);

  // Medal notification
  useEffect(() => {
    if (medalShown) {
      showToast('🏅 Медаль 100K получена! Поздравляем!', 'success');
      setMedalShown(false);
    }
  }, [medalShown]);

  const showToast = (msg: string, type: 'success' | 'error' | 'info' = 'info') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  };

  const handleClick = useCallback((e: React.MouseEvent<HTMLButtonElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    setClickAnimation(true);
    setTimeout(() => setClickAnimation(false), 300);

    const id = ++coinIdRef.current;
    setFloatCoins(prev => [...prev, { id, x, y, value: state.clickPower }]);
    setTimeout(() => setFloatCoins(prev => prev.filter(c => c.id !== id)), 800);

    onStateChange(s => {
      const newCoins = s.coins + s.clickPower;
      const newTotal = s.totalCoins + s.clickPower;
      const newTotalClicks = s.totalClicks + 1;
      const medal = !s.medal100k && newTotalClicks >= 100000;
      if (medal) setMedalShown(true);
      return {
        ...s,
        coins: newCoins,
        totalCoins: newTotal,
        totalClicks: newTotalClicks,
        medal100k: s.medal100k || medal,
      };
    });
  }, [state.clickPower, onStateChange]);

  const handleBuyUpgrade = (upgradeId: string) => {
    onStateChange(s => {
      const upIdx = s.upgrades.findIndex(u => u.id === upgradeId);
      if (upIdx === -1) return s;
      const up = s.upgrades[upIdx];
      
      if (s.coins < up.cost) {
        showToast('Недостаточно монет!', 'error');
        return s;
      }

      if (up.active && up.expiresAt) {
        showToast('Улучшение уже активно!', 'info');
        return s;
      }

      const now = Date.now();
      const newUpgrades = [...s.upgrades];
      newUpgrades[upIdx] = {
        ...up,
        active: true,
        expiresAt: now + up.duration,
      };

      let newClickPower = s.clickPower;
      let newAutoClick = s.autoClickPerSecond;

      if (up.isAutoClicker) {
        newAutoClick = s.autoClickPerSecond + up.multiplier;
      } else {
        newClickPower = s.baseClickPower * up.multiplier;
      }

      showToast(`✨ ${up.name} активирован на 24 часа!`, 'success');

      return {
        ...s,
        coins: s.coins - up.cost,
        upgrades: newUpgrades,
        clickPower: newClickPower,
        autoClickPerSecond: newAutoClick,
      };
    });
  };

  const handleClaimGift = (day: number) => {
    const todayStr = getTodayStr();
    if (state.lastDailyCheck === todayStr) {
      showToast('Подарок уже получен сегодня. Возвращайся завтра!', 'info');
      return;
    }
    const gift = state.dailyGifts.find(g => g.day === day);
    if (!gift || gift.claimed) {
      showToast('Этот подарок уже получен', 'info');
      return;
    }

    onStateChange(s => {
      const newGifts = s.dailyGifts.map(g =>
        g.day === day ? { ...g, claimed: true } : g
      );
      const coinsGained = gift.coins || 0;
      const goldenSpidi = gift.specialReward === 'golden_spidi';

      showToast(
        goldenSpidi
          ? '🌟 Золотой Спиди! +50 к силе клика навсегда!'
          : `Получено ${formatNumber(coinsGained)} монет!`,
        'success'
      );

      const newBase = goldenSpidi ? s.baseClickPower + 50 : s.baseClickPower;
      const activeMultUpgrade = s.upgrades.find(u => !u.isAutoClicker && u.active);
      const newClick = activeMultUpgrade ? newBase * activeMultUpgrade.multiplier : newBase;

      return {
        ...s,
        coins: s.coins + coinsGained,
        totalCoins: s.totalCoins + coinsGained,
        baseClickPower: newBase,
        clickPower: newClick,
        goldenSpidi: s.goldenSpidi || goldenSpidi,
        dailyGifts: newGifts,
        lastDailyCheck: todayStr,
        currentDay: Math.min(s.currentDay + 1, 5),
      };
    });
  };

  const handleSettingsChange = (updates: Partial<GameState>) => {
    onStateChange(s => ({ ...s, ...updates }));
    if ('selectedMusic' in updates) {
      audioManager.setTrack(updates.selectedMusic!, state.customMusic);
    }
    if ('musicEnabled' in updates) {
      audioManager.setEnabled(updates.musicEnabled!);
    }
    if ('musicVolume' in updates) {
      audioManager.setVolume(updates.musicVolume!);
    }
  };

  const handleResetComplete = (updates: Partial<GameState>) => {
    onStateChange(s => ({ ...s, ...updates }));
    setShowResetOOBE(false);
    showToast('Настройки обновлены!', 'success');
  };

  const handleCustomMusicUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    Array.from(files).forEach(file => {
      // Проверяем расширение файла
      const ext = file.name.split('.').pop()?.toLowerCase();
      if (!ext || !['mp3', 'm4a', 'wav', 'mpeg', 'ogg', 'aac'].includes(ext)) {
        showToast(`Файл "${file.name}" не поддерживается. Только MP3, M4A, WAV`, 'error');
        return;
      }
      const url = URL.createObjectURL(file);
      const name = file.name.replace(/\.[^.]+$/, '');
      onStateChange(s => ({
        ...s,
        customMusic: [...(s.customMusic || []), { name, url }],
      }));
      showToast(`♫ "${name}" добавлена!`, 'success');
    });
    e.target.value = '';
  };

  const handleWallpaperUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    Array.from(files).forEach(file => {
      const url = URL.createObjectURL(file);
      onStateChange(s => ({ ...s, customWallpapers: [...(s.customWallpapers || []), url] }));
    });
    e.target.value = '';
    showToast('Обои добавлены!', 'success');
  };

  const allWallpapers = [...WALLPAPERS, ...(state.customWallpapers || [])];
  const allMusicTracks = [
    ...MUSIC_TRACKS,
    ...(state.customMusic || []).map((m, i) => ({ id: `custom_${i}`, name: m.name, url: m.url })),
  ];

  const TABS: { id: Tab; label: string; icon: string }[] = [
    { id: 'game', label: 'ИГРА', icon: ICON_URLS.game },
    { id: 'upgrades', label: 'УЛУЧШЕНИЯ', icon: ICON_URLS.upgrades },
    { id: 'gifts', label: 'ПОДАРКИ', icon: ICON_URLS.gifts },
    { id: 'settings', label: 'НАСТРОЙКИ', icon: ICON_URLS.settings },
  ];

  return (
    <div
      className="fixed inset-0 wallpaper-bg overflow-hidden"
      style={{ backgroundImage: `url(${state.selectedWallpaper})` }}
    >
      {/* Background overlay */}
      <div className="absolute inset-0 bg-white/20 backdrop-blur-[2px]" />

      {showResetOOBE && (
        <OOBEReset
          currentState={state}
          onComplete={handleResetComplete}
          onCancel={() => setShowResetOOBE(false)}
        />
      )}

      {/* Toast */}
      {toast && (
        <div
          className="fixed top-5 right-5 z-50 px-5 py-3 rounded-2xl text-white font-semibold shadow-2xl toast-in"
          style={{
            background: toast.type === 'success'
              ? 'linear-gradient(135deg,#22c55e,#16a34a)'
              : toast.type === 'error'
              ? 'linear-gradient(135deg,#ef4444,#dc2626)'
              : 'linear-gradient(135deg,#3b82f6,#2563eb)',
            maxWidth: '300px',
          }}
        >
          {toast.msg}
        </div>
      )}

      <div className="relative z-10 h-full flex flex-col">
        {/* Top Header */}
        <header
          className="glass flex items-center justify-between px-6 py-3 shadow-lg"
          style={{ borderBottom: '1px solid rgba(255,255,255,0.6)' }}
        >
          <div className="flex items-center gap-3">
            <img src={LOGO_URL} alt="Spidi Clicker" className="w-10 h-10 rounded-xl shadow" />
            <div>
              <h1 className="font-black text-gray-800 text-lg leading-tight">Spidi Clicker</h1>
              <p className="text-gray-400 text-xs font-medium">v3.1</p>
            </div>
          </div>

          {/* Stats */}
          <div className="flex items-center gap-4">
            {state.medal100k && (
              <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl animate-fade-in"
                style={{ background: 'rgba(245,158,11,0.15)', border: '1px solid rgba(245,158,11,0.3)' }}>
                <img src={ICON_URLS.medal100k} alt="Медаль" className="w-5 h-5 animate-spin-slow" />
                <span className="text-xs font-bold medal-shine">100K</span>
              </div>
            )}
            {state.goldenSpidi && (
              <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl"
                style={{ background: 'rgba(245,158,11,0.15)', border: '1px solid rgba(245,158,11,0.3)' }}>
                <img src={ICON_URLS.goldenSpidi} alt="Золотой Спиди" className="w-5 h-5 rounded-full" />
                <span className="text-xs font-bold text-amber-500">Golden</span>
              </div>
            )}

            <div className="flex items-center gap-2 px-3 py-2 rounded-2xl"
              style={{ background: 'rgba(59,130,246,0.1)', border: '1px solid rgba(59,130,246,0.2)' }}>
              <img src={ICON_URLS.clickPower} alt="Сила" className="w-5 h-5" />
              <span className="text-sm font-bold text-blue-600">{formatNumber(state.clickPower)}</span>
            </div>

            {state.autoClickPerSecond > 0 && (
              <div className="flex items-center gap-2 px-3 py-2 rounded-2xl"
                style={{ background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.2)' }}>
                <img src={ICON_URLS.autoClicker} alt="Авто" className="w-5 h-5 animate-spin-slow" />
                <span className="text-sm font-bold text-green-600">{formatNumber(state.autoClickPerSecond)}/с</span>
              </div>
            )}

            <div className="flex items-center gap-2 px-4 py-2 rounded-2xl"
              style={{ background: 'rgba(59,130,246,0.12)', border: '1.5px solid rgba(59,130,246,0.25)' }}>
              <img src={ICON_URLS.coin} alt="Монеты" className="w-6 h-6" />
              <span className="text-base font-black text-gray-800 counter-num">{formatNumber(state.coins)}</span>
            </div>
          </div>

          {/* Tabs */}
          <nav className="flex items-center gap-1">
            {TABS.map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className="flex items-center gap-2 px-4 py-2 rounded-xl transition-all duration-200 relative"
                style={{
                  background: activeTab === tab.id
                    ? 'linear-gradient(135deg, rgba(59,130,246,0.2), rgba(59,130,246,0.08))'
                    : 'transparent',
                  color: activeTab === tab.id ? '#3b82f6' : '#6b7280',
                  fontWeight: activeTab === tab.id ? '700' : '500',
                }}
              >
                <img src={tab.icon} alt={tab.label} className="w-5 h-5" />
                <span className="text-xs">{tab.label}</span>
                {activeTab === tab.id && (
                  <div
                    className="absolute bottom-0 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-blue-500"
                  />
                )}
              </button>
            ))}
          </nav>
        </header>

        {/* Main content */}
        <main className="flex-1 overflow-hidden">
          {activeTab === 'game' && (
            <GameTab
              state={state}
              floatCoins={floatCoins}
              clickAnimation={clickAnimation}
              onClickCoin={handleClick}
            />
          )}
          {activeTab === 'upgrades' && (
            <UpgradesTab state={state} onBuy={handleBuyUpgrade} />
          )}
          {activeTab === 'gifts' && (
            <GiftsTab state={state} onClaim={handleClaimGift} />
          )}
          {activeTab === 'settings' && (
            <SettingsTab
              state={state}
              allWallpapers={allWallpapers}
              allMusicTracks={allMusicTracks}
              onSettingsChange={handleSettingsChange}
              onOpenResetOOBE={() => setShowResetOOBE(true)}
              onUploadMusic={() => customMusicInputRef.current?.click()}
              onUploadWallpaper={() => wallpaperInputRef.current?.click()}
            />
          )}
        </main>
      </div>

      {/* Hidden inputs */}
      <input
        ref={customMusicInputRef}
        type="file"
        accept="audio/*,.mp3,.m4a,.wav,.mpeg,.ogg,.aac"
        multiple
        className="hidden"
        onChange={handleCustomMusicUpload}
      />
      <input
        ref={wallpaperInputRef}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={handleWallpaperUpload}
      />
    </div>
  );
};

// ============================================================
// GAME TAB
// ============================================================
const GameTab: React.FC<{
  state: GameState;
  floatCoins: FloatCoin[];
  clickAnimation: boolean;
  onClickCoin: (e: React.MouseEvent<HTMLButtonElement>) => void;
}> = ({ state, floatCoins, clickAnimation, onClickCoin }) => (
  <div className="h-full flex items-center justify-center p-8 animate-fade-in">
    <div className="flex flex-col items-center gap-8">
      {/* Coins display */}
      <div
        className="glass px-10 py-6 rounded-3xl text-center shadow-xl animate-slide-in-down"
        style={{ minWidth: '280px' }}
      >
        <div className="flex items-center justify-center gap-3 mb-1">
          <img src={ICON_URLS.coin} alt="Монеты" className="w-10 h-10" />
          <span className="text-5xl font-black text-gray-800 counter-num">{formatNumber(state.coins)}</span>
        </div>
        <p className="text-gray-500 text-sm font-medium">монет • всего {formatNumber(state.totalCoins)}</p>
      </div>

      {/* Click Button */}
      <div className="relative">
        {floatCoins.map(coin => (
          <div
            key={coin.id}
            className="absolute pointer-events-none animate-coin-float font-black text-blue-600 text-lg z-50"
            style={{ left: coin.x - 20, top: coin.y - 30, whiteSpace: 'nowrap' }}
          >
            +{formatNumber(coin.value)}
          </div>
        ))}

        <button
          onClick={onClickCoin}
          className="click-btn relative"
          style={{
            background: 'none',
            border: 'none',
            padding: 0,
            cursor: 'pointer',
          }}
        >
          <div
            className="transition-all duration-150"
            style={{
              transform: clickAnimation ? 'scale(0.93)' : 'scale(1)',
              filter: clickAnimation ? 'brightness(0.9)' : 'brightness(1)',
            }}
          >
            <img
              src={ICON_URLS.clickBtn}
              alt="Кликнуть"
              className="w-48 h-48 rounded-full shadow-2xl animate-pulse-soft"
              style={{
                boxShadow: clickAnimation
                  ? '0 4px 20px rgba(59,130,246,0.3)'
                  : '0 16px 48px rgba(59,130,246,0.35), 0 0 0 3px rgba(255,255,255,0.8)',
                transition: 'box-shadow 0.15s ease',
              }}
            />
          </div>
        </button>
      </div>

      {/* Stats row */}
      <div className="flex gap-4">
        <div className="glass px-5 py-3 rounded-2xl flex items-center gap-2 shadow animate-slide-in-left">
          <img src={ICON_URLS.clickPower} alt="" className="w-6 h-6" />
          <div>
            <div className="text-xs text-gray-500">Сила клика</div>
            <div className="font-bold text-blue-600">{formatNumber(state.clickPower)}</div>
          </div>
        </div>

        {state.autoClickPerSecond > 0 && (
          <div className="glass px-5 py-3 rounded-2xl flex items-center gap-2 shadow animate-fade-in">
            <img src={ICON_URLS.autoClicker} alt="" className="w-6 h-6 animate-spin-slow" />
            <div>
              <div className="text-xs text-gray-500">Авто/сек</div>
              <div className="font-bold text-green-600">{formatNumber(state.autoClickPerSecond)}</div>
            </div>
          </div>
        )}

        <div className="glass px-5 py-3 rounded-2xl flex items-center gap-2 shadow animate-slide-in-right">
          <span className="text-xl">👆</span>
          <div>
            <div className="text-xs text-gray-500">Всего кликов</div>
            <div className="font-bold text-gray-700">{formatNumber(state.totalClicks)}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
);

// ============================================================
// UPGRADES TAB
// ============================================================
const UpgradesTab: React.FC<{
  state: GameState;
  onBuy: (id: string) => void;
}> = ({ state, onBuy }) => (
  <div className="h-full overflow-y-auto p-6 animate-fade-in">
    <div className="max-w-4xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <img src={ICON_URLS.upgradesSection} alt="" className="w-8 h-8" />
        <h2 className="text-2xl font-black text-gray-800">Улучшения на 24 часа</h2>
        <div className="ml-auto flex items-center gap-2 glass px-4 py-2 rounded-2xl">
          <img src={ICON_URLS.coin} alt="" className="w-5 h-5" />
          <span className="font-black text-gray-800">{formatNumber(state.coins)}</span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {state.upgrades.map((upgrade, idx) => {
          const canAfford = state.coins >= upgrade.cost;
          const isActive = upgrade.active && upgrade.expiresAt && Date.now() < upgrade.expiresAt;
          const timeLeft = upgrade.expiresAt ? formatTimeRemaining(upgrade.expiresAt) : '';

          return (
            <div
              key={upgrade.id}
              className="upgrade-card glass rounded-2xl p-4 shadow animate-fade-in"
              style={{
                animationDelay: `${idx * 0.05}s`,
                opacity: isActive ? 1 : 0.95,
                border: isActive ? '2px solid rgba(34,197,94,0.5)' : undefined,
              }}
            >
              <div className="flex items-start gap-3">
                <div
                  className="w-12 h-12 rounded-xl flex items-center justify-center text-xl font-black flex-shrink-0"
                  style={{
                    background: isActive
                      ? 'linear-gradient(135deg,#22c55e,#16a34a)'
                      : canAfford
                      ? 'rgba(59,130,246,0.12)'
                      : 'rgba(0,0,0,0.05)',
                    color: isActive ? 'white' : canAfford ? '#3b82f6' : '#999',
                  }}
                >
                  {upgrade.icon}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    <h3 className="font-bold text-gray-800">{upgrade.name}</h3>
                    {isActive && (
                      <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-600 font-bold animate-pulse-soft">
                        {timeLeft}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-gray-500 mb-2">{upgrade.description}</p>
                  {upgrade.isAutoClicker && (
                    <span className="text-xs text-green-500 font-semibold">🤖 Автокликер +{upgrade.multiplier}/с</span>
                  )}
                  {!upgrade.isAutoClicker && (
                    <span className="text-xs text-blue-500 font-semibold">⚡ Множитель ✕{upgrade.multiplier}</span>
                  )}
                </div>

                {/* Buy button */}
                  <button
                  onClick={() => !isActive && onBuy(upgrade.id)}
                  disabled={Boolean(!canAfford || isActive)}
                  className="flex flex-col items-center gap-1 px-3 py-2 rounded-xl transition-all btn-press flex-shrink-0"
                  style={{
                    background: isActive
                      ? 'rgba(34,197,94,0.15)'
                      : canAfford
                      ? 'linear-gradient(135deg, #3b82f6, #2563eb)'
                      : 'rgba(0,0,0,0.06)',
                    color: isActive ? '#22c55e' : canAfford ? 'white' : '#9ca3af',
                    cursor: isActive || !canAfford ? 'not-allowed' : 'pointer',
                    minWidth: '80px',
                  }}
                >
                  {isActive ? (
                    <span className="text-xs font-bold">✓ Активен</span>
                  ) : (
                    <>
                      <div className="flex items-center gap-1">
                        <img src={ICON_URLS.coin} alt="" className="w-3.5 h-3.5" />
                        <span className="text-xs font-bold">{formatNumber(upgrade.cost)}</span>
                      </div>
                      <span className="text-xs opacity-80">Купить</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  </div>
);

// ============================================================
// GIFTS TAB
// ============================================================
const GiftsTab: React.FC<{
  state: GameState;
  onClaim: (day: number) => void;
}> = ({ state, onClaim }) => {
  const todayStr = getTodayStr();
  const canClaimToday = state.lastDailyCheck !== todayStr;
  const nextClaimableDay = state.dailyGifts.find(g => !g.claimed);

  return (
    <div className="h-full overflow-y-auto p-6 animate-fade-in">
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center gap-3 mb-2">
          <img src={ICON_URLS.gifts} alt="" className="w-8 h-8" />
          <h2 className="text-2xl font-black text-gray-800">Ежедневные подарки</h2>
        </div>
        <p className="text-gray-500 text-sm mb-6">
          {canClaimToday && nextClaimableDay
            ? 'Можно получить подарок сегодня!'
            : '⏰ Возвращайся завтра за следующим подарком'}
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {state.dailyGifts.map((gift, idx) => {
            const isCurrentDay = nextClaimableDay?.day === gift.day;
            const canClaim = isCurrentDay && canClaimToday;

            return (
              <div
                key={gift.day}
                className="gift-day-card glass rounded-2xl p-5 shadow animate-fade-in"
                style={{
                  animationDelay: `${idx * 0.08}s`,
                  border: gift.claimed
                    ? '2px solid rgba(34,197,94,0.4)'
                    : canClaim
                    ? '2px solid rgba(59,130,246,0.5)'
                    : '1.5px solid rgba(255,255,255,0.6)',
                  background: gift.claimed
                    ? 'rgba(34,197,94,0.08)'
                    : canClaim
                    ? 'rgba(59,130,246,0.06)'
                    : undefined,
                }}
              >
                <div className="flex items-center gap-2 mb-3">
                  <div
                    className="w-8 h-8 rounded-full flex items-center justify-center text-sm font-black text-white"
                    style={{
                      background: gift.claimed
                        ? 'linear-gradient(135deg,#22c55e,#16a34a)'
                        : canClaim
                        ? 'linear-gradient(135deg,#3b82f6,#2563eb)'
                        : 'rgba(0,0,0,0.1)',
                    }}
                  >
                    {gift.claimed ? '✓' : gift.day}
                  </div>
                  <span className="font-bold text-gray-700">День {gift.day}</span>
                  {canClaim && (
                    <span className="ml-auto text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-600 font-bold animate-pulse-soft">
                      Сегодня!
                    </span>
                  )}
                </div>

                {gift.specialReward === 'golden_spidi' ? (
                  <div className="flex flex-col items-center gap-2 mb-3">
                    <img
                      src={ICON_URLS.goldenSpidi}
                      alt="Золотой Спиди"
                      className="w-16 h-16 rounded-full shadow-lg animate-float"
                    />
                    <span className="text-sm font-bold text-amber-500">Золотой Спиди!</span>
                    <span className="text-xs text-gray-500">+50 к силе клика навсегда</span>
                  </div>
                ) : (
                  <div className="flex items-center gap-2 mb-3">
                    <img src={ICON_URLS.coin} alt="" className="w-8 h-8" />
                    <span className="text-lg font-black text-gray-800">{gift.reward}</span>
                  </div>
                )}

                <button
                  onClick={() => canClaim && onClaim(gift.day)}
                  disabled={!!(!canClaim || gift.claimed)}
                  className="w-full py-2.5 rounded-xl text-sm font-bold transition-all btn-press flex items-center justify-center gap-2"
                  style={{
                    background: gift.claimed
                      ? 'rgba(34,197,94,0.15)'
                      : canClaim
                      ? 'linear-gradient(135deg, #3b82f6, #2563eb)'
                      : 'rgba(0,0,0,0.05)',
                    color: gift.claimed ? '#22c55e' : canClaim ? 'white' : '#9ca3af',
                    cursor: gift.claimed || !canClaim ? 'not-allowed' : 'pointer',
                  }}
                >
                  {gift.claimed ? (
                    <>
                      <span>✓ Получено</span>
                    </>
                  ) : canClaim ? (
                    <>
                      <img src={ICON_URLS.gifts} alt="" className="w-4 h-4" />
                      <span>Получить!</span>
                    </>
                  ) : (
                    <>
                      <span>🔒 Заблокировано</span>
                    </>
                  )}
                </button>
              </div>
            );
          })}
        </div>

        {/* Medal */}
        {state.medal100k && (
          <div
            className="mt-6 glass rounded-2xl p-5 flex items-center gap-4 shadow animate-bounce-in"
            style={{ border: '2px solid rgba(245,158,11,0.4)', background: 'rgba(245,158,11,0.06)' }}
          >
            <img src={ICON_URLS.medal100k} alt="Медаль 100K" className="w-14 h-14 animate-float" />
            <div>
              <h3 className="font-black text-lg medal-shine">Медаль 100K!</h3>
              <p className="text-gray-600 text-sm">Достигнуто 100 000 кликов. Легенда!</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

// ============================================================
// SETTINGS TAB
// ============================================================
const SettingsTab: React.FC<{
  state: GameState;
  allWallpapers: string[];
  allMusicTracks: { id: string; name: string; url?: string }[];
  onSettingsChange: (updates: Partial<GameState>) => void;
  onOpenResetOOBE: () => void;
  onUploadMusic: () => void;
  onUploadWallpaper: () => void;
}> = ({ state, allWallpapers, allMusicTracks, onSettingsChange, onOpenResetOOBE, onUploadMusic, onUploadWallpaper }) => {
  const [confirmReset, setConfirmReset] = useState(false);

  return (
    <div className="h-full overflow-y-auto p-6 animate-fade-in">
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <img src={ICON_URLS.settingsSection} alt="" className="w-8 h-8" />
          <h2 className="text-2xl font-black text-gray-800">Настройки</h2>
        </div>

        <div className="grid gap-4">
          {/* Wallpapers */}
          <div className="glass rounded-2xl p-5 shadow">
            <div className="flex items-center gap-2 mb-4">
              <span className="text-lg">🖼️</span>
              <h3 className="font-bold text-gray-800">Обои</h3>
              <button
                onClick={onUploadWallpaper}
                className="ml-auto px-3 py-1.5 rounded-xl text-xs font-semibold btn-press"
                style={{ background: 'rgba(59,130,246,0.1)', color: '#3b82f6' }}
              >
                + Добавить
              </button>
            </div>
            <div className="grid gap-2" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
              {allWallpapers.map((url, i) => (
                <button
                  key={i}
                  onClick={() => onSettingsChange({ selectedWallpaper: url })}
                  className="wallpaper-item relative rounded-xl overflow-hidden"
                  style={{
                    aspectRatio: '16/9',
                    border: state.selectedWallpaper === url ? '3px solid #3b82f6' : '2px solid rgba(255,255,255,0.5)',
                    boxShadow: state.selectedWallpaper === url ? '0 0 0 2px #3b82f6' : 'none',
                  }}
                >
                  <img src={url} alt="" className="w-full h-full object-cover" />
                  {state.selectedWallpaper === url && (
                    <div className="absolute inset-0 flex items-center justify-center bg-blue-500/20">
                      <span className="text-white text-base">✓</span>
                    </div>
                  )}
                  {i >= 8 && (
                    <div className="absolute top-1 right-1 bg-blue-500 text-white text-xs px-1 rounded">My</div>
                  )}
                </button>
              ))}
            </div>
          </div>

          {/* Music */}
          <div className="glass rounded-2xl p-5 shadow">
            <div className="flex items-center gap-2 mb-4">
              <span className="text-lg">🎵</span>
              <h3 className="font-bold text-gray-800">Музыка</h3>
              <div className="ml-auto flex items-center gap-2">
                <button
                  onClick={onUploadMusic}
                  className="px-3 py-1.5 rounded-xl text-xs font-semibold btn-press"
                  style={{ background: 'rgba(59,130,246,0.1)', color: '#3b82f6' }}
                >
                  + Добавить MP3/M4A/WAV
                </button>
                <button
                  onClick={() => onSettingsChange({ musicEnabled: !state.musicEnabled })}
                  className="relative w-12 h-6 rounded-full transition-all duration-300"
                  style={{ background: state.musicEnabled ? '#3b82f6' : '#d1d5db' }}
                >
                  <div
                    className="absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-all duration-300"
                    style={{ left: state.musicEnabled ? '26px' : '2px' }}
                  />
                </button>
              </div>
            </div>

            <div className="flex flex-col gap-2 mb-4">
              {allMusicTracks.map(track => (
                <button
                  key={track.id}
                  onClick={() => onSettingsChange({ selectedMusic: track.id })}
                  className="flex items-center gap-3 p-3 rounded-xl transition-all text-left"
                  style={{
                    background: state.selectedMusic === track.id
                      ? 'rgba(59,130,246,0.12)'
                      : 'rgba(0,0,0,0.03)',
                    border: state.selectedMusic === track.id
                      ? '1.5px solid rgba(59,130,246,0.3)'
                      : '1.5px solid transparent',
                  }}
                >
                  <div
                    className="w-8 h-8 rounded-lg flex items-center justify-center text-white"
                    style={{ background: state.selectedMusic === track.id ? '#3b82f6' : '#94a3b8' }}
                  >
                    🎵
                  </div>
                  <span className="text-sm font-semibold text-gray-700">{track.name}</span>
                  {state.selectedMusic === track.id && (
                    <div className="ml-auto w-2 h-2 rounded-full bg-blue-500 animate-pulse-soft" />
                  )}
                </button>
              ))}
            </div>

            {/* Volume */}
            <div className="flex items-center gap-3">
              <span className="text-sm text-gray-500">🔉</span>
              <input
                type="range"
                min="0"
                max="1"
                step="0.05"
                value={state.musicVolume}
                onChange={e => onSettingsChange({ musicVolume: parseFloat(e.target.value) })}
                className="flex-1 accent-blue-500"
              />
              <span className="text-sm text-gray-500">🔊</span>
              <span className="text-xs text-gray-400 w-8">{Math.round(state.musicVolume * 100)}%</span>
            </div>
          </div>

          {/* Stats */}
          <div className="glass rounded-2xl p-5 shadow">
            <div className="flex items-center gap-2 mb-4">
              <span className="text-lg">📊</span>
              <h3 className="font-bold text-gray-800">Статистика</h3>
            </div>
            <div className="grid grid-cols-2 gap-3">
              {[
                { label: 'Монеты', value: formatNumber(state.coins) },
                { label: 'Всего монет', value: formatNumber(state.totalCoins) },
                { label: 'Всего кликов', value: formatNumber(state.totalClicks) },
                { label: 'Сила клика', value: formatNumber(state.clickPower) },
                { label: 'Авто/сек', value: formatNumber(state.autoClickPerSecond) },
                { label: 'Медаль 100K', value: state.medal100k ? '✅' : '❌' },
              ].map(({ label, value }) => (
                <div key={label} className="p-3 rounded-xl" style={{ background: 'rgba(0,0,0,0.03)' }}>
                  <div className="text-xs text-gray-500 mb-0.5">{label}</div>
                  <div className="font-bold text-gray-800">{value}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Danger zone */}
          <div className="glass rounded-2xl p-5 shadow" style={{ border: '1.5px solid rgba(239,68,68,0.2)' }}>
            <div className="flex items-center gap-2 mb-4">
              <span className="text-lg">⚠️</span>
              <h3 className="font-bold text-gray-800">Сброс</h3>
            </div>
            <div className="flex flex-col gap-2">
              <button
                onClick={onOpenResetOOBE}
                className="w-full py-2.5 rounded-xl text-sm font-semibold btn-press"
                style={{ background: 'rgba(59,130,246,0.1)', color: '#3b82f6' }}
              >
                ⚙️ Пересоздать мастер настройки
              </button>
              {!confirmReset ? (
                <button
                  onClick={() => setConfirmReset(true)}
                  className="w-full py-2.5 rounded-xl text-sm font-semibold btn-press"
                  style={{ background: 'rgba(239,68,68,0.1)', color: '#ef4444' }}
                >
                  🗑️ Сбросить весь прогресс
                </button>
              ) : (
                <div className="p-3 rounded-xl bg-red-50 border border-red-200">
                  <p className="text-red-600 text-sm font-semibold mb-2">Вы уверены? Весь прогресс будет удалён!</p>
                  <div className="flex gap-2">
                    <button
                      onClick={() => {
                        localStorage.removeItem('spidi_clicker_v31_save');
                        window.location.reload();
                      }}
                      className="flex-1 py-2 rounded-xl text-sm font-bold text-white"
                      style={{ background: '#ef4444' }}
                    >
                      Сбросить!
                    </button>
                    <button
                      onClick={() => setConfirmReset(false)}
                      className="flex-1 py-2 rounded-xl text-sm font-semibold"
                      style={{ background: 'rgba(0,0,0,0.06)', color: '#555' }}
                    >
                      Отмена
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GamePC;
