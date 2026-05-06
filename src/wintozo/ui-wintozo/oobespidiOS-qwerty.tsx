// =====================================================
// Spidi Clicker v3.1 — OOBE Reset Wizard
// =====================================================

import React, { useState, useRef } from 'react';
import {
  GameState, WALLPAPERS, MUSIC_TRACKS, LOGO_URL, ICON_URLS,
} from '../game/gameState';
import { audioManager } from '../game/audioManager';

interface OOBEResetProps {
  currentState: GameState;
  onComplete: (updates: Partial<GameState>) => void;
  onCancel: () => void;
}

const TOTAL_STEPS = 5;

export const OOBEReset: React.FC<OOBEResetProps> = ({ currentState, onComplete, onCancel }) => {
  const [step, setStep] = useState(1);
  const [deviceType, setDeviceType] = useState<'phone' | 'tablet' | 'pc'>(currentState.deviceType);
  const [selectedWallpaper, setSelectedWallpaper] = useState(currentState.selectedWallpaper);
  const [selectedIconPack] = useState<'new'>('new');
  const [iconPackError, setIconPackError] = useState('');
  const [localIconPack, setLocalIconPack] = useState<'new' | 'old'>('new');
  const [selectedMusic, setSelectedMusic] = useState(currentState.selectedMusic);
  const [musicEnabled, setMusicEnabled] = useState(currentState.musicEnabled);
  const [previewing, setPreviewing] = useState<string | null>(null);
  const [customWallpapers, setCustomWallpapers] = useState<string[]>(currentState.customWallpapers || []);
  const wallpaperInputRef = useRef<HTMLInputElement>(null);

  const allWallpapers = [...WALLPAPERS, ...customWallpapers];

  const goNext = () => {
    if (step === 3 && localIconPack === 'old') {
      setIconPackError('Старый пак удалён из Spidi Clicker');
      return;
    }
    setStep(s => Math.min(s + 1, TOTAL_STEPS));
  };

  const goPrev = () => {
    setStep(s => Math.max(s - 1, 1));
  };

  const handlePreview = async (trackId: string) => {
    if (previewing) return;
    const track = MUSIC_TRACKS.find(t => t.id === trackId);
    if (!track) return;
    setPreviewing(trackId);
    await audioManager.previewTrack(track.url, 5000);
    setPreviewing(null);
  };

  const handleWallpaperUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    Array.from(files).forEach(file => {
      const url = URL.createObjectURL(file);
      setCustomWallpapers(prev => [...prev, url]);
    });
    e.target.value = '';
  };

  const handleComplete = () => {
    onComplete({
      deviceType,
      selectedWallpaper,
      selectedIconPack,
      selectedMusic,
      musicEnabled,
      customWallpapers,
      completedOnboarding: true,
    });
  };

  const stepNames = ['Устройство', 'Фон', 'Иконки', 'Музыка', 'Старт'];

  return (
    <div
      className="fixed inset-0 flex items-center justify-center z-50 wallpaper-bg"
      style={{ backgroundImage: `url(${selectedWallpaper})` }}
    >
      <div className="absolute inset-0 bg-white/40 backdrop-blur-sm" />

      <div className="relative z-10 w-full max-w-lg mx-4 animate-bounce-in">
        <div
          className="glass rounded-3xl shadow-2xl overflow-hidden"
          style={{ border: '1.5px solid rgba(255,255,255,0.7)' }}
        >
          {/* Header */}
          <div className="bg-gradient-to-r from-purple-500/90 to-blue-500/90 px-6 pt-6 pb-4">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <img src={LOGO_URL} alt="Spidi Clicker" className="w-10 h-10 rounded-xl shadow" />
                <div>
                  <h1 className="text-white font-bold text-lg leading-tight">Сброс настроек</h1>
                  <p className="text-purple-100 text-xs">Spidi Clicker v3.1</p>
                </div>
              </div>
              <button
                onClick={onCancel}
                className="text-white/70 hover:text-white text-lg transition-colors"
              >
                ✕
              </button>
            </div>

            <div className="flex gap-1.5 mb-2">
              {Array.from({ length: TOTAL_STEPS }, (_, i) => (
                <div
                  key={i}
                  className="flex-1 h-1.5 rounded-full transition-all duration-500"
                  style={{ background: i < step ? 'rgba(255,255,255,0.9)' : 'rgba(255,255,255,0.3)' }}
                />
              ))}
            </div>
            <div className="flex justify-between">
              {stepNames.map((name, i) => (
                <span
                  key={i}
                  className="text-xs transition-all"
                  style={{
                    color: i + 1 === step ? 'rgba(255,255,255,1)' : 'rgba(255,255,255,0.5)',
                    fontWeight: i + 1 === step ? '700' : '400',
                  }}
                >
                  {name}
                </span>
              ))}
            </div>
          </div>

          <div className="px-6 py-5" key={step}>
            {step === 1 && (
              <ResetStepDevice deviceType={deviceType} onSelect={setDeviceType} />
            )}
            {step === 2 && (
              <ResetStepWallpaper
                allWallpapers={allWallpapers}
                selected={selectedWallpaper}
                onSelect={setSelectedWallpaper}
                onUpload={() => wallpaperInputRef.current?.click()}
              />
            )}
            {step === 3 && (
              <ResetStepIconPack
                selected={localIconPack}
                onSelect={(p) => { setLocalIconPack(p); setIconPackError(''); }}
                error={iconPackError}
              />
            )}
            {step === 4 && (
              <ResetStepMusic
                selectedMusic={selectedMusic}
                musicEnabled={musicEnabled}
                previewing={previewing}
                onSelectMusic={setSelectedMusic}
                onToggleMusic={setMusicEnabled}
                onPreview={handlePreview}
              />
            )}
            {step === 5 && (
              <ResetStepFinal onComplete={handleComplete} onCancel={onCancel} />
            )}
          </div>

          {step < 5 && (
            <div className="px-6 pb-6 flex justify-between items-center">
              <button
                onClick={goPrev}
                disabled={step === 1}
                className="px-5 py-2.5 rounded-2xl text-sm font-semibold transition-all"
                style={{
                  background: step === 1 ? 'rgba(0,0,0,0.05)' : 'rgba(139,92,246,0.1)',
                  color: step === 1 ? '#999' : '#8b5cf6',
                }}
              >
                ← Назад
              </button>
              <span className="text-xs text-gray-400">Шаг {step} из {TOTAL_STEPS}</span>
              <button
                onClick={goNext}
                className="px-6 py-2.5 rounded-2xl text-sm font-bold text-white btn-press shadow-lg"
                style={{ background: 'linear-gradient(135deg, #8b5cf6, #3b82f6)' }}
              >
                Далее →
              </button>
            </div>
          )}
        </div>
      </div>

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

const ResetStepDevice: React.FC<{
  deviceType: 'phone' | 'tablet' | 'pc';
  onSelect: (d: 'phone' | 'tablet' | 'pc') => void;
}> = ({ deviceType, onSelect }) => (
  <div className="animate-fade-in">
    <h2 className="text-xl font-bold text-gray-800 mb-1">Тип устройства</h2>
    <p className="text-gray-500 text-sm mb-5">Перенастрой интерфейс под своё устройство</p>
    <div className="grid grid-cols-3 gap-3">
      {[
        { type: 'phone' as const, label: 'Телефон', icon: ICON_URLS.phone },
        { type: 'tablet' as const, label: 'Планшет', icon: ICON_URLS.tablet },
        { type: 'pc' as const, label: 'Компьютер', icon: ICON_URLS.pc },
      ].map(({ type, label, icon }) => (
        <button
          key={type}
          onClick={() => onSelect(type)}
          className="flex flex-col items-center gap-2 p-4 rounded-2xl transition-all duration-300 border-2"
          style={{
            background: deviceType === type
              ? 'linear-gradient(135deg, rgba(139,92,246,0.15), rgba(59,130,246,0.05))'
              : 'rgba(255,255,255,0.6)',
            borderColor: deviceType === type ? '#8b5cf6' : 'rgba(0,0,0,0.08)',
            transform: deviceType === type ? 'scale(1.04)' : 'scale(1)',
          }}
        >
          <img src={icon} alt={label} className="w-14 h-14 object-contain" />
          <span className="text-sm font-semibold" style={{ color: deviceType === type ? '#8b5cf6' : '#555' }}>
            {label}
          </span>
        </button>
      ))}
    </div>
  </div>
);

const ResetStepWallpaper: React.FC<{
  allWallpapers: string[];
  selected: string;
  onSelect: (w: string) => void;
  onUpload: () => void;
}> = ({ allWallpapers, selected, onSelect, onUpload }) => (
  <div className="animate-fade-in">
    <h2 className="text-xl font-bold text-gray-800 mb-1">Сменить фон</h2>
    <p className="text-gray-500 text-sm mb-4">Выбери новые обои</p>
    <div
      className="grid gap-2 overflow-y-auto pr-1"
      style={{ gridTemplateColumns: 'repeat(3, 1fr)', maxHeight: '240px' }}
    >
      {allWallpapers.map((url, i) => (
        <button
          key={i}
          onClick={() => onSelect(url)}
          className="wallpaper-item relative rounded-xl overflow-hidden aspect-video"
          style={{
            border: selected === url ? '3px solid #8b5cf6' : '2px solid rgba(255,255,255,0.5)',
            boxShadow: selected === url ? '0 0 0 2px #8b5cf6' : 'none',
          }}
        >
          <img src={url} alt={`Обои ${i + 1}`} className="w-full h-full object-cover" />
          {selected === url && (
            <div className="absolute inset-0 flex items-center justify-center bg-purple-500/20">
              <span className="text-white text-xl">✓</span>
            </div>
          )}
        </button>
      ))}
    </div>
    <button
      onClick={onUpload}
      className="mt-3 w-full py-2.5 rounded-2xl text-sm font-semibold border-2 border-dashed transition-all btn-press"
      style={{ borderColor: '#8b5cf6', color: '#8b5cf6', background: 'rgba(139,92,246,0.05)' }}
    >
      + Добавить свои обои
    </button>
  </div>
);

const ResetStepIconPack: React.FC<{
  selected: 'new' | 'old';
  onSelect: (p: 'new' | 'old') => void;
  error: string;
}> = ({ selected, onSelect, error }) => (
  <div className="animate-fade-in">
    <h2 className="text-xl font-bold text-gray-800 mb-1">Пак иконок</h2>
    <p className="text-gray-500 text-sm mb-5">Выбери стиль иконок</p>
    <div className="flex flex-col gap-3">
      <button
        onClick={() => onSelect('new')}
        className="flex items-center gap-4 p-4 rounded-2xl border-2 transition-all"
        style={{
          background: selected === 'new' ? 'rgba(139,92,246,0.1)' : 'rgba(255,255,255,0.6)',
          borderColor: selected === 'new' ? '#8b5cf6' : 'rgba(0,0,0,0.08)',
        }}
      >
        <div className="flex gap-2">
          <img src={ICON_URLS.clickBtn} alt="" className="w-10 h-10 rounded-xl object-contain" />
          <img src={ICON_URLS.coin} alt="" className="w-10 h-10 rounded-xl object-contain" />
        </div>
        <div className="text-left">
          <div className="font-bold text-gray-800">NEW PACK</div>
          <div className="text-xs text-gray-500">Современные иконки v3.1</div>
        </div>
        {selected === 'new' && (
          <div className="ml-auto w-6 h-6 rounded-full bg-purple-500 flex items-center justify-center text-white text-xs">✓</div>
        )}
      </button>
      <button
        onClick={() => onSelect('old')}
        className="flex items-center gap-4 p-4 rounded-2xl border-2 opacity-50 transition-all"
        style={{ background: 'rgba(255,255,255,0.4)', borderColor: 'rgba(0,0,0,0.08)' }}
      >
        <div className="w-10 h-10 rounded-xl bg-gray-200 flex items-center justify-center text-xl">🗑️</div>
        <div className="text-left">
          <div className="font-bold text-gray-500">OLD PACK</div>
          <div className="text-xs text-gray-400">Устаревшие иконки</div>
        </div>
      </button>
      {error && (
        <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-red-600 text-sm font-medium animate-fade-in">
          ⚠️ {error}
        </div>
      )}
    </div>
  </div>
);

const ResetStepMusic: React.FC<{
  selectedMusic: string;
  musicEnabled: boolean;
  previewing: string | null;
  onSelectMusic: (id: string) => void;
  onToggleMusic: (e: boolean) => void;
  onPreview: (id: string) => void;
}> = ({ selectedMusic, musicEnabled, previewing, onSelectMusic, onToggleMusic, onPreview }) => (
  <div className="animate-fade-in">
    <h2 className="text-xl font-bold text-gray-800 mb-1">Музыка</h2>
    <p className="text-gray-500 text-sm mb-4">Выбери фоновую мелодию</p>
    <div className="flex flex-col gap-3 mb-4">
      {MUSIC_TRACKS.map((track) => (
        <div
          key={track.id}
          className="p-3 rounded-2xl border-2 transition-all"
          style={{
            background: selectedMusic === track.id ? 'rgba(139,92,246,0.1)' : 'rgba(255,255,255,0.6)',
            borderColor: selectedMusic === track.id ? '#8b5cf6' : 'rgba(0,0,0,0.08)',
          }}
        >
          <div className="flex items-center gap-3">
            <button onClick={() => onSelectMusic(track.id)} className="flex-1 flex items-center gap-3 text-left">
              <div
                className="w-9 h-9 rounded-xl flex items-center justify-center text-white text-sm"
                style={{ background: selectedMusic === track.id ? '#8b5cf6' : '#94a3b8' }}
              >
                🎵
              </div>
              <div>
                <div className="font-semibold text-gray-800 text-sm">{track.name}</div>
                <div className="text-xs text-gray-500">MP3</div>
              </div>
            </button>
            <button
              onClick={() => onPreview(track.id)}
              disabled={!!previewing}
              className="px-3 py-1.5 rounded-xl text-xs font-semibold"
              style={{ background: 'rgba(139,92,246,0.1)', color: '#8b5cf6' }}
            >
              {previewing === track.id ? '▶ 5s...' : '▶ Слушать'}
            </button>
          </div>
        </div>
      ))}
    </div>
    <div
      className="flex items-center justify-between p-3 rounded-2xl"
      style={{ background: 'rgba(255,255,255,0.6)', border: '1.5px solid rgba(0,0,0,0.08)' }}
    >
      <span className="font-semibold text-gray-700 text-sm">Музыка включена</span>
      <button
        onClick={() => onToggleMusic(!musicEnabled)}
        className="relative w-12 h-6 rounded-full transition-all duration-300"
        style={{ background: musicEnabled ? '#8b5cf6' : '#d1d5db' }}
      >
        <div
          className="absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-all duration-300"
          style={{ left: musicEnabled ? '26px' : '2px' }}
        />
      </button>
    </div>
  </div>
);

const ResetStepFinal: React.FC<{ onComplete: () => void; onCancel: () => void }> = ({ onComplete, onCancel }) => (
  <div className="flex flex-col items-center py-4 animate-fade-in">
    <img
      src={LOGO_URL}
      alt="Spidi Clicker"
      className="w-24 h-24 rounded-3xl shadow-xl mb-5 animate-float"
    />
    <h2 className="text-2xl font-black text-gray-800 mb-2">Настройка обновлена!</h2>
    <p className="text-gray-500 text-sm text-center mb-6">
      Новые настройки применены.<br />
      <span className="text-red-400 font-semibold">Игровой прогресс сохранён.</span>
    </p>
    <button
      onClick={onComplete}
      className="w-full py-4 rounded-2xl text-white font-black text-lg btn-press shadow-xl mb-3"
      style={{ background: 'linear-gradient(135deg, #8b5cf6, #3b82f6)' }}
    >
      ✅ Применить настройки
    </button>
    <button
      onClick={onCancel}
      className="w-full py-2.5 rounded-2xl font-semibold text-sm transition-all"
      style={{ background: 'rgba(0,0,0,0.05)', color: '#666' }}
    >
      Отмена
    </button>
  </div>
);

export default OOBEReset;
