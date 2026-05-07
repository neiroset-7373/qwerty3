// =====================================================
// Spidi Clicker v3.1 — Game State & Logic
// =====================================================

export interface Upgrade {
  id: string;
  name: string;
  description: string;
  cost: number;
  multiplier: number;
  isAutoClicker: boolean;
  icon: string;
  duration: number; // в миллисекундах (24 часа)
  expiresAt?: number; // timestamp когда истечёт
  active: boolean;
}

export interface DailyGift {
  day: number;
  reward: string;
  coins?: number;
  specialReward?: string;
  claimed: boolean;
}

export interface GameState {
  coins: number;
  totalCoins: number;
  totalClicks: number;
  clickPower: number;
  baseClickPower: number;
  autoClickPerSecond: number;
  baseAutoClick: number;
  upgrades: Upgrade[];
  dailyGifts: DailyGift[];
  lastDailyCheck: string;
  currentDay: number;
  medal100k: boolean;
  goldenSpidi: boolean;
  selectedWallpaper: string;
  selectedIconPack: string;
  selectedMusic: string;
  musicEnabled: boolean;
  musicVolume: number;
  completedOnboarding: boolean;
  deviceType: 'phone' | 'tablet' | 'pc';
  customWallpapers: string[];
  customMusic: { name: string; url: string }[];
  lastSaved: string;
}

export const WALLPAPERS = [
  'https://imgfy.ru/ib/Es100C1zaVZdm1Z_1775727486.webp',
  'https://imgfy.ru/ib/hNDLBKPvzB3XRdJ_1775727498.webp',
  'https://imgfy.ru/ib/58mSMpwqgKyc2bv_1775727499.webp',
  'https://imgfy.ru/ib/k1k7x1HIEvj4OZO_1775727498.webp',
  'https://imgfy.ru/ib/vWDp69bLejOVlvE_1775727499.webp',
  'https://imgfy.ru/ib/8tTCTZQjXOq7iZw_1776351253.webp',
  'https://imgfy.ru/ib/dgxAmb468GFFs2E_1776351253.webp',
  'https://imgfy.ru/ib/BZbPPQFZNmv3qrd_1777051453.webp',
];

export const LOGO_URL = 'https://i.ibb.co/x8tjVJBT/9a537f7d-5259-44cd-b818-dff5f504aca3.jpg';

export const ICON_URLS = {
  game: 'https://imgfy.ru/ib/T4OFpSB3pas5OvI_1776415599.webp',
  upgrades: 'https://imgfy.ru/ib/8oDebZgUc1j5TuA_1776415600.webp',
  gifts: 'https://imgfy.ru/ib/WqGTQErBrSV3lF8_1776970865.webp',
  settings: 'https://imgfy.ru/ib/e8UA077eHGqRZ7u_1776415599.webp',
  clickPower: 'https://imgfy.ru/ib/HJcZg8qbls7EbTV_1776351304.webp',
  upgradesSection: 'https://imgfy.ru/ib/rc2NGfsgt97BWyb_1776351596.webp',
  autoClicker: 'https://imgfy.ru/ib/kTYWzhqIhMduTlz_1777054672.webp',
  settingsSection: 'https://imgfy.ru/ib/1dLMmuECgT7T0r9_1776351304.webp',
  medal100k: 'https://imgfy.ru/ib/kI4AgyRv8KaPZAo_1775835017.webp',
  goldenSpidi: 'https://i.ibb.co/gppjW5R/1775898076670.jpg',
  coin: 'https://imgfy.ru/ib/Vm9n8qKCjSoXfQ7_1775834388.webp',
  clickBtn: 'https://imgfy.ru/ib/cFgAkQjlmXFzaGI_1776967380.webp',
  phone: 'https://imgfy.ru/ib/475Jq1LdI26eKyi_1776415547.webp',
  tablet: 'https://imgfy.ru/ib/sXHaQUDSGc305IG_1776415547.webp',
  pc: 'https://imgfy.ru/ib/UsrM5vinHW04SdC_1776415548.webp',
};

export const MUSIC_TRACKS = [
  {
    id: 'track1',
    name: 'Неофициальная мелодия',
    url: 'https://cdn.jsdelivr.net/gh/neiroset-7373/music/spidi_music.mp3',
  },
  {
    id: 'track2',
    name: 'Spidi Original',
    url: 'https://cdn.jsdelivr.net/gh/neiroset-7373/music/click.mp3',
  },
];

const ONE_DAY_MS = 24 * 60 * 60 * 1000;

const DEFAULT_UPGRADES: Upgrade[] = [
  {
    id: 'mult_x2',
    name: 'Множитель x2',
    description: 'Удваивает силу клика на 24 часа',
    cost: 250,
    multiplier: 2,
    isAutoClicker: false,
    icon: '✕2',
    duration: ONE_DAY_MS,
    active: false,
  },
  {
    id: 'mult_x3',
    name: 'Множитель x3',
    description: 'Утраивает силу клика на 24 часа',
    cost: 450,
    multiplier: 3,
    isAutoClicker: false,
    icon: '✕3',
    duration: ONE_DAY_MS,
    active: false,
  },
  {
    id: 'mult_x4',
    name: 'Множитель x4',
    description: 'Учетверяет силу клика на 24 часа',
    cost: 760,
    multiplier: 4,
    isAutoClicker: false,
    icon: '✕4',
    duration: ONE_DAY_MS,
    active: false,
  },
  {
    id: 'mult_x5',
    name: 'Множитель x5',
    description: 'Увеличивает силу клика в 5 раз на 24 часа',
    cost: 1000,
    multiplier: 5,
    isAutoClicker: false,
    icon: '✕5',
    duration: ONE_DAY_MS,
    active: false,
  },
  {
    id: 'mult_x10',
    name: 'Множитель x10',
    description: 'Увеличивает силу клика в 10 раз на 24 часа',
    cost: 10500,
    multiplier: 10,
    isAutoClicker: false,
    icon: '✕10',
    duration: ONE_DAY_MS,
    active: false,
  },
  {
    id: 'mult_x100',
    name: 'Множитель x100',
    description: 'Увеличивает силу клика в 100 раз на 24 часа',
    cost: 11500,
    multiplier: 100,
    isAutoClicker: false,
    icon: '✕100',
    duration: ONE_DAY_MS,
    active: false,
  },
  {
    id: 'mult_x1000',
    name: 'Множитель x1000',
    description: 'Увеличивает силу клика в 1000 раз на 24 часа',
    cost: 13000,
    multiplier: 1000,
    isAutoClicker: false,
    icon: '✕1K',
    duration: ONE_DAY_MS,
    active: false,
  },
  {
    id: 'autoclicker',
    name: 'Автокликер',
    description: 'Автоматически кликает 1 раз в секунду на 24 часа',
    cost: 500,
    multiplier: 1,
    isAutoClicker: true,
    icon: '🤖',
    duration: ONE_DAY_MS,
    active: false,
  },
];

const DEFAULT_DAILY_GIFTS: DailyGift[] = [
  { day: 1, reward: '100 монет', coins: 100, claimed: false },
  { day: 2, reward: '1 000 монет', coins: 1000, claimed: false },
  { day: 3, reward: '5 000 монет', coins: 5000, claimed: false },
  { day: 4, reward: '10 000 монет', coins: 10000, claimed: false },
  { day: 5, reward: 'Золотой Спиди (+50 сила клика навсегда)', specialReward: 'golden_spidi', claimed: false },
];

export const DEFAULT_STATE: GameState = {
  coins: 0,
  totalCoins: 0,
  totalClicks: 0,
  clickPower: 1,
  baseClickPower: 1,
  autoClickPerSecond: 0,
  baseAutoClick: 0,
  upgrades: DEFAULT_UPGRADES,
  dailyGifts: DEFAULT_DAILY_GIFTS,
  lastDailyCheck: '',
  currentDay: 1,
  medal100k: false,
  goldenSpidi: false,
  selectedWallpaper: WALLPAPERS[0],
  selectedIconPack: 'new',
  selectedMusic: MUSIC_TRACKS[0].id,
  musicEnabled: true,
  musicVolume: 0.3,
  completedOnboarding: false,
  deviceType: 'pc',
  customWallpapers: [],
  customMusic: [],
  lastSaved: '',
};

const SAVE_KEY = 'spidi_clicker_v31_save';

export function loadState(): GameState {
  try {
    const saved = localStorage.getItem(SAVE_KEY);
    if (saved) {
      const parsed = JSON.parse(saved) as Partial<GameState>;
      return {
        ...DEFAULT_STATE,
        ...parsed,
        upgrades: parsed.upgrades && parsed.upgrades.length > 0
          ? parsed.upgrades
          : DEFAULT_UPGRADES,
        dailyGifts: parsed.dailyGifts && parsed.dailyGifts.length > 0
          ? parsed.dailyGifts
          : DEFAULT_DAILY_GIFTS,
      };
    }
  } catch (e) {
    console.warn('Failed to load state', e);
  }
  return { ...DEFAULT_STATE };
}

export function saveState(state: GameState): void {
  try {
    localStorage.setItem(SAVE_KEY, JSON.stringify({
      ...state,
      lastSaved: new Date().toISOString(),
    }));
  } catch (e) {
    console.warn('Failed to save state', e);
  }
}

export function resetState(): void {
  try {
    localStorage.removeItem(SAVE_KEY);
  } catch (e) {
    console.warn('Failed to reset state', e);
  }
}

export function recalculateStats(state: GameState): GameState {
  const now = Date.now();
  let clickMultiplier = 1;
  let autoClick = 0;

  // Обновляем статус апгрейдов и считаем множители
  const updatedUpgrades = state.upgrades.map(up => {
    if (up.active && up.expiresAt && now < up.expiresAt) {
      if (up.isAutoClicker) {
        autoClick += up.multiplier;
      } else {
        clickMultiplier *= up.multiplier;
      }
      return up;
    } else if (up.active && up.expiresAt && now >= up.expiresAt) {
      // Истек
      return { ...up, active: false, expiresAt: undefined };
    }
    return up;
  });

  const newClickPower = state.baseClickPower * clickMultiplier;
  const newAutoClick = state.baseAutoClick + autoClick;

  return {
    ...state,
    upgrades: updatedUpgrades,
    clickPower: newClickPower,
    autoClickPerSecond: newAutoClick,
  };
}

export function formatNumber(n: number): string {
  if (n >= 1_000_000_000) return (n / 1_000_000_000).toFixed(2) + 'B';
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(2) + 'M';
  if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K';
  return Math.floor(n).toString();
}

export function getTodayStr(): string {
  return new Date().toDateString();
}

export function formatTimeRemaining(expiresAt: number): string {
  const now = Date.now();
  const diff = expiresAt - now;
  if (diff <= 0) return 'Истёк';
  
  const hours = Math.floor(diff / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  
  if (hours > 0) {
    return `${hours}ч ${minutes}м`;
  }
  return `${minutes}м`;
}
