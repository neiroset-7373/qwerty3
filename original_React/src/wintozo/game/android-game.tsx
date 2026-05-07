// =====================================================
// Spidi Clicker v3.1 — Android/Mobile Layout
// =====================================================

import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  GameState, ICON_URLS, LOGO_URL, MUSIC_TRACKS, WALLPAPERS,
  formatNumber, saveState, getTodayStr, recalculateStats, formatTimeRemaining,
} from './gameState';
import { audioManager } from './audioManager';
import { OOBEReset } from '../ui-wintozo/oobespidiOS-qwerty';

interface AndroidGameProps {
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

export const AndroidGame: React.FC<AndroidGameProps> = ({ state, onStateChange }) => {
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

  useEffect(() => {
    saveRef.current = setInterval(() => {
      onStateChange(s => { saveState(s); return s; });
    }, 5000);
    return () => { if (saveRef.current) clearInterval(saveRef.current); };
  }, [onStateChange]);

  useEffect(() => {
    recalcRef.current = setInterval(() => {
      onStateChange(s => recalculateStats(s));
    }, 1000);
    return () => { if (recalcRef.current) clearInterval(recalcRef.current); };
  }, [onStateChange]);

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
          return { ...s, coins: newCoins, totalCoins: newTotal, totalClicks: newTotalClicks, medal100k: s.medal100k || medal };
        });
      }, 1000);
    }
    return () => { if (autoClickRef.current) clearInterval(autoClickRef.current); };
  }, [state.autoClickPerSecond, state.clickPower, onStateChange]);

  useEffect(() => {
    audioManager.init(state.selectedMusic, state.musicEnabled, state.musicVolume, state.customMusic);
  }, [state.selectedMusic, state.musicEnabled, state.musicVolume, state.customMusic]);

  useEffect(() => {
    if (medalShown) {
      showToast('🏅 Медаль 100K получена!', 'success');
      setMedalShown(false);
    }
  }, [medalShown]);

  const showToast = (msg: string, type: 'success' | 'error' | 'info' = 'info') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  };

  const handleClick = useCallback((e: React.TouchEvent<HTMLButtonElement> | React.MouseEvent<HTMLButtonElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    let clientX: number, clientY: number;

    if ('touches' in e) {
      clientX = e.touches[0]?.clientX ?? rect.left + rect.width / 2;
      clientY = e.touches[0]?.clientY ?? rect.top + rect.height / 2;
    } else {
      clientX = (e as React.MouseEvent).clientX;
      clientY = (e as React.MouseEvent).clientY;
    }

    const x = clientX - rect.left;
    const y = clientY - rect.top;

    setClickAnimation(true);
    setTimeout(() => setClickAnimation(false), 200);

    const id = ++coinIdRef.current;
    setFloatCoins(prev => [...prev, { id, x, y, value: state.clickPower }]);
    setTimeout(() => setFloatCoins(prev => prev.filter(c => c.id !== id)), 700);

    onStateChange(s => {
      const newCoins = s.coins + s.clickPower;
      const newTotal = s.totalCoins + s.clickPower;
      const newTotalClicks = s.totalClicks + 1;
      const medal = !s.medal100k && newTotalClicks >= 100000;
      if (medal) setMedalShown(true);
      return { ...s, coins: newCoins, totalCoins: newTotal, totalClicks: newTotalClicks, medal100k: s.medal100k || medal };
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

      showToast(`✨ ${up.name} активирован!`, 'success');

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
      showToast('Подарок уже получен сегодня!', 'info');
      return;
    }
    const gift = state.dailyGifts.find(g => g.day === day);
    if (!gift || gift.claimed) return;

    onStateChange(s => {
      const newGifts = s.dailyGifts.map(g => g.day === day ? { ...g, claimed: true } : g);
      const coinsGained = gift.coins || 0;
      const goldenSpidi = gift.specialReward === 'golden_spidi';
      showToast(goldenSpidi ? '🌟 Золотой Спиди! +50 к силе!' : `+${formatNumber(coinsGained)} монет!`, 'success');

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

  const handleCustomMusicUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    Array.from(files).forEach(file => {
      // Проверяем расширение файла
      const ext = file.name.split('.').pop()?.toLowerCase();
      if (!ext || !['mp3', 'm4a', 'wav', 'mpeg', 'ogg', 'aac'].includes(ext)) {
        showToast(`Файл "${file.name}" не поддерживается`, 'error');
        return;
      }
      const url = URL.createObjectURL(file);
      const name = file.name.replace(/\.[^.]+$/, '');
      onStateChange(s => ({ ...s, customMusic: [...(s.customMusic || []), { name, url }] }));
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

  const handleSettingsChange = (updates: Partial<GameState>) => {
    onStateChange(s => ({ ...s, ...updates }));
    if ('selectedMusic' in updates) audioManager.setTrack(updates.selectedMusic!, state.customMusic);
    if ('musicEnabled' in updates) audioManager.setEnabled(updates.musicEnabled!);
    if ('musicVolume' in updates) audioManager.setVolume(updates.musicVolume!);
  };

  const allWallpapers = [...WALLPAPERS, ...(state.customWallpapers || [])];
  const allMusicTracks = [
    ...MUSIC_TRACKS,
    ...(state.customMusic || []).map((m, i) => ({ id: `custom_${i}`, name: m.name, url: m.url })),
  ];

  const TABS: { id: Tab; label: string; icon: string }[] = [
    { id: 'game', label: 'Игра', icon: ICON_URLS.game },
    { id: 'upgrades', label: 'Улучш.', icon: ICON_URLS.upgrades },
    { id: 'gifts', label: 'Подарки', icon: ICON_URLS.gifts },
    { id: 'settings', label: 'Настр.', icon: ICON_URLS.settings },
  ];

  return (
    <div
      className="fixed inset-0 wallpaper-bg flex flex-col overflow-hidden"
      style={{ backgroundImage: `url(${state.selectedWallpaper})` }}
    >
      <div className="absolute inset-0 bg-white/25 backdrop-blur-[2px]" />

      {showResetOOBE && (
        <OOBEReset
          currentState={state}
          onComplete={(updates) => { onStateChange(s => ({ ...s, ...updates })); setShowResetOOBE(false); showToast('Настройки обновлены!', 'success'); }}
          onCancel={() => setShowResetOOBE(false)}
        />
      )}

      {toast && (
        <div
          className="fixed top-4 left-4 right-4 z-50 px-4 py-3 rounded-2xl text-white font-semibold shadow-2xl text-sm text-center toast-in"
          style={{
            background: toast.type === 'success' ? '#22c55e' : toast.type === 'error' ? '#ef4444' : '#3b82f6',
          }}
        >
          {toast.msg}
        </div>
      )}

      {/* Top header (mobile) */}
      <header
        className="relative z-10 glass flex items-center justify-between px-4 pt-3 pb-3"
        style={{ paddingTop: 'max(12px, env(safe-area-inset-top, 12px))', borderBottom: '1px solid rgba(255,255,255,0.6)' }}
      >
        <div className="flex items-center gap-2">
          <img src={LOGO_URL} alt="Spidi" className="w-8 h-8 rounded-xl" />
          <span className="font-black text-gray-800 text-sm">Spidi Clicker</span>
        </div>

        <div className="flex items-center gap-2">
          {state.medal100k && (
            <img src={ICON_URLS.medal100k} alt="Медаль" className="w-6 h-6 animate-spin-slow" />
          )}
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl"
            style={{ background: 'rgba(59,130,246,0.12)', border: '1px solid rgba(59,130,246,0.2)' }}>
            <img src={ICON_URLS.coin} alt="" className="w-5 h-5" />
            <span className="text-sm font-black text-gray-800">{formatNumber(state.coins)}</span>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="relative z-10 flex-1 overflow-hidden">
        {activeTab === 'game' && (
          <MobileGameTab
            state={state}
            floatCoins={floatCoins}
            clickAnimation={clickAnimation}
            onClick={handleClick}
          />
        )}
        {activeTab === 'upgrades' && (
          <MobileUpgradesTab state={state} onBuy={handleBuyUpgrade} />
        )}
        {activeTab === 'gifts' && (
          <MobileGiftsTab state={state} onClaim={handleClaimGift} />
        )}
        {activeTab === 'settings' && (
          <MobileSettingsTab
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

      {/* Bottom navigation */}
      <nav
        className="relative z-10 glass flex items-center justify-around px-2 py-2"
        style={{
          paddingBottom: 'max(8px, env(safe-area-inset-bottom, 8px))',
          borderTop: '1px solid rgba(255,255,255,0.6)',
          boxShadow: '0 -4px 20px rgba(0,0,0,0.08)',
        }}
      >
        {TABS.map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className="flex flex-col items-center gap-1 px-4 py-2 rounded-xl transition-all duration-200 relative"
            style={{
              background: activeTab === tab.id ? 'rgba(59,130,246,0.12)' : 'transparent',
              flex: 1,
            }}
          >
            <img src={tab.icon} alt={tab.label} className="w-6 h-6" />
            <span
              className="text-xs font-semibold"
              style={{ color: activeTab === tab.id ? '#3b82f6' : '#9ca3af' }}
            >
              {tab.label}
            </span>
            {activeTab === tab.id && (
              <div className="absolute -top-0.5 left-1/2 -translate-x-1/2 w-6 h-0.5 bg-blue-500 rounded-full" />
            )}
          </button>
        ))}
      </nav>

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

// ---- Mobile Game Tab ----
const MobileGameTab: React.FC<{
  state: GameState;
  floatCoins: FloatCoin[];
  clickAnimation: boolean;
  onClick: (e: React.TouchEvent<HTMLButtonElement> | React.MouseEvent<HTMLButtonElement>) => void;
}> = ({ state, floatCoins, clickAnimation, onClick }) => (
  <div className="h-full flex flex-col items-center justify-center px-4 py-4 gap-4 animate-fade-in">
    {/* Coins */}
    <div className="glass rounded-3xl px-8 py-5 text-center shadow-xl w-full max-w-xs">
      <div className="flex items-center justify-center gap-2 mb-1">
        <img src={ICON_URLS.coin} alt="" className="w-8 h-8" />
        <span className="text-4xl font-black text-gray-800">{formatNumber(state.coins)}</span>
      </div>
      <p className="text-gray-500 text-xs">монет</p>
    </div>

    {/* Click button */}
    <div className="relative">
      {floatCoins.map(coin => (
        <div
          key={coin.id}
          className="absolute pointer-events-none animate-coin-float font-black text-blue-600 text-base z-50"
          style={{ left: coin.x - 20, top: coin.y - 30, whiteSpace: 'nowrap' }}
        >
          +{formatNumber(coin.value)}
        </div>
      ))}

      <button
        onTouchStart={onClick}
        onClick={onClick}
        className="click-btn"
        style={{ background: 'none', border: 'none', padding: 0, cursor: 'pointer', WebkitTapHighlightColor: 'transparent' }}
      >
        <img
          src={ICON_URLS.clickBtn}
          alt="Кликнуть"
          className="rounded-full"
          style={{
            width: '180px',
            height: '180px',
            objectFit: 'contain',
            transform: clickAnimation ? 'scale(0.91)' : 'scale(1)',
            transition: 'transform 0.1s ease',
            boxShadow: '0 12px 40px rgba(59,130,246,0.35), 0 0 0 3px rgba(255,255,255,0.8)',
          }}
        />
      </button>
    </div>

    {/* Stats */}
    <div className="flex gap-3 w-full max-w-xs">
      <div className="flex-1 glass rounded-2xl p-2.5 flex flex-col items-center gap-1 shadow">
        <img src={ICON_URLS.clickPower} alt="" className="w-5 h-5" />
        <span className="text-xs text-gray-500">Сила</span>
        <span className="text-sm font-bold text-blue-600">{formatNumber(state.clickPower)}</span>
      </div>
      {state.autoClickPerSecond > 0 && (
        <div className="flex-1 glass rounded-2xl p-2.5 flex flex-col items-center gap-1 shadow">
          <img src={ICON_URLS.autoClicker} alt="" className="w-5 h-5 animate-spin-slow" />
          <span className="text-xs text-gray-500">Авто/с</span>
          <span className="text-sm font-bold text-green-600">{formatNumber(state.autoClickPerSecond)}</span>
        </div>
      )}
      <div className="flex-1 glass rounded-2xl p-2.5 flex flex-col items-center gap-1 shadow">
        <span className="text-lg">👆</span>
        <span className="text-xs text-gray-500">Кликов</span>
        <span className="text-sm font-bold text-gray-700">{formatNumber(state.totalClicks)}</span>
      </div>
    </div>
  </div>
);

// ---- Mobile Upgrades Tab ----
const MobileUpgradesTab: React.FC<{ state: GameState; onBuy: (id: string) => void }> = ({ state, onBuy }) => (
  <div className="h-full overflow-y-auto px-3 py-3 animate-fade-in">
    <div className="flex items-center gap-2 mb-3">
      <img src={ICON_URLS.upgradesSection} alt="" className="w-6 h-6" />
      <h2 className="text-base font-black text-gray-800">Улучшения на 24ч</h2>
      <div className="ml-auto flex items-center gap-1.5 glass px-3 py-1 rounded-xl">
        <img src={ICON_URLS.coin} alt="" className="w-4 h-4" />
        <span className="text-sm font-black text-gray-800">{formatNumber(state.coins)}</span>
      </div>
    </div>
    <div className="flex flex-col gap-2.5">
      {state.upgrades.map((upgrade, idx) => {
        const canAfford = state.coins >= upgrade.cost;
        const isActive = upgrade.active && upgrade.expiresAt && Date.now() < upgrade.expiresAt;
        const timeLeft = upgrade.expiresAt ? formatTimeRemaining(upgrade.expiresAt) : '';
        return (
          <div
            key={upgrade.id}
            className="glass rounded-2xl p-3 shadow animate-fade-in"
            style={{ animationDelay: `${idx * 0.04}s`, border: isActive ? '2px solid rgba(34,197,94,0.5)' : undefined }}
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center text-base font-black flex-shrink-0"
                style={{ background: isActive ? 'linear-gradient(135deg,#22c55e,#16a34a)' : 'rgba(59,130,246,0.1)', color: isActive ? 'white' : '#3b82f6' }}>
                {upgrade.icon}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1.5 mb-0.5">
                  <span className="font-bold text-gray-800 text-sm">{upgrade.name}</span>
                  {isActive && <span className="text-xs px-1.5 py-0.5 rounded-full bg-green-100 text-green-600 font-bold">{timeLeft}</span>}
                </div>
                <div className="text-xs text-gray-500">{upgrade.description}</div>
              </div>
              <button
                onClick={() => !isActive && onBuy(upgrade.id)}
                disabled={Boolean(!canAfford || isActive)}
                className="flex flex-col items-center gap-0.5 px-3 py-2 rounded-xl transition-all flex-shrink-0"
                style={{
                  background: isActive ? 'rgba(34,197,94,0.15)' : canAfford ? 'linear-gradient(135deg,#3b82f6,#2563eb)' : 'rgba(0,0,0,0.06)',
                  color: isActive ? '#22c55e' : canAfford ? 'white' : '#9ca3af',
                  cursor: isActive || !canAfford ? 'not-allowed' : 'pointer',
                  minWidth: '65px',
                }}
              >
                {isActive ? (
                  <span className="text-xs font-bold">✓</span>
                ) : (
                  <>
                    <div className="flex items-center gap-0.5">
                      <img src={ICON_URLS.coin} alt="" className="w-3 h-3" />
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
);

// ---- Mobile Gifts Tab ----
const MobileGiftsTab: React.FC<{ state: GameState; onClaim: (day: number) => void }> = ({ state, onClaim }) => {
  const todayStr = getTodayStr();
  const canClaimToday = state.lastDailyCheck !== todayStr;
  const nextClaimableDay = state.dailyGifts.find(g => !g.claimed);

  return (
    <div className="h-full overflow-y-auto px-3 py-3 animate-fade-in">
      <div className="flex items-center gap-2 mb-1">
        <img src={ICON_URLS.gifts} alt="" className="w-6 h-6" />
        <h2 className="text-base font-black text-gray-800">Подарки</h2>
      </div>
      <p className="text-gray-500 text-xs mb-3">
        {canClaimToday && nextClaimableDay ? 'Можно получить подарок!' : '⏰ Возвращайся завтра!'}
      </p>
      <div className="flex flex-col gap-2.5">
        {state.dailyGifts.map((gift, idx) => {
          const isCurrentDay = nextClaimableDay?.day === gift.day;
          const canClaim = isCurrentDay && canClaimToday;
          return (
            <div
              key={gift.day}
              className="gift-day-card glass rounded-2xl p-4 shadow animate-fade-in"
              style={{
                animationDelay: `${idx * 0.06}s`,
                border: gift.claimed ? '2px solid rgba(34,197,94,0.4)' : canClaim ? '2px solid rgba(59,130,246,0.5)' : '1.5px solid rgba(255,255,255,0.6)',
              }}
            >
              <div className="flex items-center gap-3">
                <div
                  className="w-8 h-8 rounded-full flex items-center justify-center text-sm font-black text-white flex-shrink-0"
                  style={{ background: gift.claimed ? '#22c55e' : canClaim ? '#3b82f6' : 'rgba(0,0,0,0.1)' }}
                >
                  {gift.claimed ? '✓' : gift.day}
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-0.5">
                    <span className="font-bold text-gray-800 text-sm">День {gift.day}</span>
                    {canClaim && <span className="text-xs bg-blue-100 text-blue-600 px-2 py-0.5 rounded-full font-bold animate-pulse-soft">Сейчас!</span>}
                  </div>
                  {gift.specialReward === 'golden_spidi' ? (
                    <div className="flex items-center gap-2">
                      <img src={ICON_URLS.goldenSpidi} alt="" className="w-7 h-7 rounded-full" />
                      <span className="text-xs font-semibold text-amber-500">Золотой Спиди! +50</span>
                    </div>
                  ) : (
                    <div className="flex items-center gap-1.5">
                      <img src={ICON_URLS.coin} alt="" className="w-5 h-5" />
                      <span className="text-sm font-bold text-gray-700">{gift.reward}</span>
                    </div>
                  )}
                </div>
                <button
                  onClick={() => canClaim && onClaim(gift.day)}
                  disabled={!!(!canClaim || gift.claimed)}
                  className="px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1 flex-shrink-0"
                  style={{
                    background: gift.claimed ? 'rgba(34,197,94,0.15)' : canClaim ? 'linear-gradient(135deg,#3b82f6,#2563eb)' : 'rgba(0,0,0,0.05)',
                    color: gift.claimed ? '#22c55e' : canClaim ? 'white' : '#9ca3af',
                    cursor: gift.claimed || !canClaim ? 'not-allowed' : 'pointer',
                    minWidth: '65px',
                  }}
                >
                  {gift.claimed ? (
                    <span>✓</span>
                  ) : canClaim ? (
                    <>
                      <img src={ICON_URLS.gifts} alt="" className="w-3.5 h-3.5" />
                      <span>Взять</span>
                    </>
                  ) : (
                    <span>🔒</span>
                  )}
                </button>
              </div>
            </div>
          );
        })}
      </div>
      {state.medal100k && (
        <div
          className="mt-4 glass rounded-2xl p-4 flex items-center gap-3 shadow"
          style={{ border: '2px solid rgba(245,158,11,0.4)' }}
        >
          <img src={ICON_URLS.medal100k} alt="" className="w-10 h-10 animate-float" />
          <div>
            <span className="font-black medal-shine text-sm">Медаль 100K!</span>
            <p className="text-gray-600 text-xs">Ты легенда Spidi!</p>
          </div>
        </div>
      )}
    </div>
  );
};

// ---- Mobile Settings Tab ----
const MobileSettingsTab: React.FC<{
  state: GameState;
  allWallpapers: string[];
  allMusicTracks: { id: string; name: string; url?: string }[];
  onSettingsChange: (u: Partial<GameState>) => void;
  onOpenResetOOBE: () => void;
  onUploadMusic: () => void;
  onUploadWallpaper: () => void;
}> = ({ state, allWallpapers, allMusicTracks, onSettingsChange, onOpenResetOOBE, onUploadMusic, onUploadWallpaper }) => {
  const [confirmReset, setConfirmReset] = useState(false);
  return (
    <div className="h-full overflow-y-auto px-3 py-3 animate-fade-in">
      <div className="flex items-center gap-2 mb-4">
        <img src={ICON_URLS.settingsSection} alt="" className="w-6 h-6" />
        <h2 className="text-base font-black text-gray-800">Настройки</h2>
      </div>
      <div className="flex flex-col gap-3">
        {/* Wallpaper */}
        <div className="glass rounded-2xl p-4 shadow">
          <div className="flex items-center gap-2 mb-3">
            <span>🖼️</span>
            <span className="font-bold text-gray-800 text-sm">Обои</span>
            <button onClick={onUploadWallpaper} className="ml-auto text-xs text-blue-500 font-semibold">+ Добавить</button>
          </div>
          <div className="grid gap-1.5" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
            {allWallpapers.map((url, i) => (
              <button
                key={i}
                onClick={() => onSettingsChange({ selectedWallpaper: url })}
                className="relative rounded-xl overflow-hidden"
                style={{
                  aspectRatio: '16/9',
                  border: state.selectedWallpaper === url ? '2.5px solid #3b82f6' : '1.5px solid rgba(255,255,255,0.4)',
                }}
              >
                <img src={url} alt="" className="w-full h-full object-cover" />
                {state.selectedWallpaper === url && (
                  <div className="absolute inset-0 flex items-center justify-center bg-blue-500/25">
                    <span className="text-white text-sm">✓</span>
                  </div>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Music */}
        <div className="glass rounded-2xl p-4 shadow">
          <div className="flex items-center gap-2 mb-3">
            <span>🎵</span>
            <span className="font-bold text-gray-800 text-sm">Музыка</span>
            <div className="ml-auto flex items-center gap-2">
              <button onClick={onUploadMusic} className="text-xs text-blue-500 font-semibold">+ Добавить</button>
              <button
                onClick={() => onSettingsChange({ musicEnabled: !state.musicEnabled })}
                className="relative w-10 h-5 rounded-full transition-all"
                style={{ background: state.musicEnabled ? '#3b82f6' : '#d1d5db' }}
              >
                <div
                  className="absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-all"
                  style={{ left: state.musicEnabled ? '22px' : '2px' }}
                />
              </button>
            </div>
          </div>
          <div className="flex flex-col gap-2">
            {allMusicTracks.map(track => (
              <button
                key={track.id}
                onClick={() => onSettingsChange({ selectedMusic: track.id })}
                className="flex items-center gap-2 p-2.5 rounded-xl transition-all text-left"
                style={{
                  background: state.selectedMusic === track.id ? 'rgba(59,130,246,0.12)' : 'rgba(0,0,0,0.03)',
                  border: state.selectedMusic === track.id ? '1.5px solid rgba(59,130,246,0.3)' : '1.5px solid transparent',
                }}
              >
                <div
                  className="w-7 h-7 rounded-lg flex items-center justify-center text-white text-xs"
                  style={{ background: state.selectedMusic === track.id ? '#3b82f6' : '#94a3b8' }}
                >
                  🎵
                </div>
                <span className="text-sm font-semibold text-gray-700 flex-1">{track.name}</span>
                {state.selectedMusic === track.id && <div className="w-2 h-2 rounded-full bg-blue-500" />}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-2 mt-3">
            <span className="text-xs">🔉</span>
            <input
              type="range" min="0" max="1" step="0.05"
              value={state.musicVolume}
              onChange={e => onSettingsChange({ musicVolume: parseFloat(e.target.value) })}
              className="flex-1 accent-blue-500"
            />
            <span className="text-xs text-gray-400">{Math.round(state.musicVolume * 100)}%</span>
          </div>
        </div>

        {/* Danger */}
        <div className="glass rounded-2xl p-4 shadow">
          <button onClick={onOpenResetOOBE} className="w-full py-2.5 rounded-xl text-sm font-semibold mb-2"
            style={{ background: 'rgba(59,130,246,0.1)', color: '#3b82f6' }}>
            ⚙️ Пересоздать мастер
          </button>
          {!confirmReset ? (
            <button onClick={() => setConfirmReset(true)} className="w-full py-2.5 rounded-xl text-sm font-semibold"
              style={{ background: 'rgba(239,68,68,0.1)', color: '#ef4444' }}>
              🗑️ Сбросить прогресс
            </button>
          ) : (
            <div className="p-3 rounded-xl bg-red-50 border border-red-200">
              <p className="text-red-600 text-xs font-semibold mb-2 text-center">Всё будет удалено!</p>
              <div className="flex gap-2">
                <button onClick={() => { localStorage.removeItem('spidi_clicker_v31_save'); window.location.reload(); }}
                  className="flex-1 py-2 rounded-xl text-xs font-bold text-white" style={{ background: '#ef4444' }}>
                  Сбросить!
                </button>
                <button onClick={() => setConfirmReset(false)}
                  className="flex-1 py-2 rounded-xl text-xs font-semibold" style={{ background: 'rgba(0,0,0,0.06)', color: '#555' }}>
                  Отмена
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AndroidGame;
