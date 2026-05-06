// =====================================================
// Spidi Clicker v3.1 — Audio Manager (Singleton)
// =====================================================

import { MUSIC_TRACKS } from './gameState';

class AudioManager {
  private audio: HTMLAudioElement | null = null;
  private currentUrl: string = '';
  private volume: number = 0.3;
  private enabled: boolean = true;

  init(trackId: string, enabled: boolean, volume: number, customMusic: { name: string; url: string }[] = []) {
    this.enabled = enabled;
    this.volume = volume;

    const allTracks = [
      ...MUSIC_TRACKS,
      ...customMusic.map((m, i) => ({ id: `custom_${i}`, name: m.name, url: m.url })),
    ];

    const track = allTracks.find(t => t.id === trackId) || allTracks[0];
    if (!track) return;

    if (this.audio && this.currentUrl === track.url) {
      this.audio.volume = volume;
      if (enabled && this.audio.paused) {
        this.audio.play().catch(() => {});
      } else if (!enabled && !this.audio.paused) {
        this.audio.pause();
      }
      return;
    }

    if (this.audio) {
      this.audio.pause();
      this.audio.src = '';
    }

    this.audio = new Audio(track.url);
    this.audio.loop = true;
    this.audio.volume = volume;
    this.currentUrl = track.url;

    if (enabled) {
      this.audio.play().catch(() => {
        // Autoplay blocked - wait for user interaction
        const resume = () => {
          if (this.enabled && this.audio) {
            this.audio.play().catch(() => {});
          }
          document.removeEventListener('click', resume);
          document.removeEventListener('touchstart', resume);
        };
        document.addEventListener('click', resume);
        document.addEventListener('touchstart', resume);
      });
    }
  }

  setTrack(trackId: string, customMusic: { name: string; url: string }[] = []) {
    const allTracks = [
      ...MUSIC_TRACKS,
      ...customMusic.map((m, i) => ({ id: `custom_${i}`, name: m.name, url: m.url })),
    ];
    const track = allTracks.find(t => t.id === trackId);
    if (!track || track.url === this.currentUrl) return;

    if (this.audio) {
      this.audio.pause();
      this.audio.src = '';
    }

    this.audio = new Audio(track.url);
    this.audio.loop = true;
    this.audio.volume = this.volume;
    this.currentUrl = track.url;

    if (this.enabled) {
      this.audio.play().catch(() => {});
    }
  }

  setEnabled(enabled: boolean) {
    this.enabled = enabled;
    if (!this.audio) return;
    if (enabled) {
      this.audio.play().catch(() => {});
    } else {
      this.audio.pause();
    }
  }

  setVolume(volume: number) {
    this.volume = volume;
    if (this.audio) {
      this.audio.volume = volume;
    }
  }

  previewTrack(url: string, duration: number = 5000): Promise<void> {
    return new Promise((resolve) => {
      const preview = new Audio(url);
      preview.volume = this.volume;
      preview.play().catch(() => {});
      setTimeout(() => {
        preview.pause();
        preview.src = '';
        resolve();
      }, duration);
    });
  }

  stop() {
    if (this.audio) {
      this.audio.pause();
      this.audio.src = '';
      this.audio = null;
      this.currentUrl = '';
    }
  }
}

export const audioManager = new AudioManager();
