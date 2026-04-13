/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';

type Screen = 'home' | 'obstacle' | 'recognition' | 'device' | 'profile' | 'history';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');

  const renderScreen = () => {
    switch (currentScreen) {
      case 'home':
        return <HomeScreen onNavigate={setCurrentScreen} />;
      case 'obstacle':
        return <ObstacleScreen />;
      case 'recognition':
        return <RecognitionScreen onNavigate={setCurrentScreen} />;
      case 'device':
        return <DeviceScreen />;
      case 'profile':
        return <ProfileScreen onNavigate={setCurrentScreen} />;
      case 'history':
        return <HistoryScreen onNavigate={setCurrentScreen} />;
      default:
        return <HomeScreen onNavigate={setCurrentScreen} />;
    }
  };

  return (
    <div className="flex flex-col h-screen max-w-[1080px] mx-auto bg-background overflow-hidden relative">
      <AnimatePresence mode="wait">
        <motion.div
          key={currentScreen}
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: -20 }}
          transition={{ duration: 0.3 }}
          className="flex-1 overflow-y-auto pb-[240px]"
        >
          {renderScreen()}
        </motion.div>
      </AnimatePresence>

      {/* Bottom Navigation Bar */}
      <nav className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[1080px] z-50 flex justify-around items-center px-8 pb-12 pt-6 bg-white border-t-8 border-surface-container-low h-[220px]">
        <NavItem 
          active={currentScreen === 'home'} 
          icon="home" 
          label="首页" 
          onClick={() => setCurrentScreen('home')} 
        />
        <NavItem 
          active={currentScreen === 'obstacle'} 
          icon="visibility" 
          label="避障" 
          onClick={() => setCurrentScreen('obstacle')} 
        />
        <NavItem 
          active={currentScreen === 'recognition' || currentScreen === 'history'} 
          icon="center_focus_strong" 
          label="识别" 
          onClick={() => setCurrentScreen('recognition')} 
        />
        <NavItem 
          active={currentScreen === 'device'} 
          icon="memory" 
          label="设备" 
          onClick={() => setCurrentScreen('device')} 
        />
        <NavItem 
          active={currentScreen === 'profile'} 
          icon="person" 
          label="我的" 
          onClick={() => setCurrentScreen('profile')} 
        />
      </nav>
    </div>
  );
}

function NavItem({ active, icon, label, onClick }: { active: boolean; icon: string; label: string; onClick: () => void }) {
  return (
    <button 
      onClick={onClick}
      className={`flex flex-col items-center justify-center transition-all duration-200 ${
        active 
          ? 'bg-primary-container text-on-primary-fixed rounded-full px-12 py-4 scale-110 shadow-lg' 
          : 'text-on-background opacity-70'
      }`}
    >
      <span className={`material-symbols-outlined text-5xl ${active ? 'fill-1' : ''}`} data-icon={icon}>{icon}</span>
      <span className="text-xl font-bold mt-1">{label}</span>
    </button>
  );
}

function HomeScreen({ onNavigate }: { onNavigate: (s: Screen) => void }) {
  return (
    <div className="flex flex-col px-12 pt-10">
      <header className="flex items-center gap-6 mb-16">
        <span className="material-symbols-outlined text-primary text-7xl" data-icon="record_voice_over">record_voice_over</span>
        <h1 className="text-5xl font-bold text-on-background">智视胸牌</h1>
      </header>
      
      <section className="mb-20">
        <h2 className="text-7xl leading-[1.2] font-bold text-on-background">
          你好，有什么可以帮到您？请对我发出指令~
        </h2>
      </section>

      <div className="flex flex-col gap-12 mb-12">
        <button 
          onClick={() => onNavigate('obstacle')}
          className="w-full text-left bg-primary-container rounded-lg p-16 flex flex-col justify-between active:scale-[0.98] transition-transform h-[600px]"
        >
          <div className="flex justify-between items-start w-full">
            <span className="material-symbols-outlined text-on-primary-fixed text-[180px] fill-1" data-icon="visibility">visibility</span>
            <span className="material-symbols-outlined text-on-primary-fixed text-6xl" data-icon="arrow_forward">arrow_forward</span>
          </div>
          <div>
            <h3 className="text-[90px] font-black text-on-primary-fixed leading-tight">实时避障</h3>
            <p className="text-4xl font-bold text-on-primary-fixed/70 mt-4">点击开启环境感知</p>
          </div>
        </button>

        <button 
          onClick={() => onNavigate('recognition')}
          className="w-full text-left bg-primary-container rounded-lg p-16 flex flex-col justify-between active:scale-[0.98] transition-transform h-[600px]"
        >
          <div className="flex justify-between items-start w-full">
            <span className="material-symbols-outlined text-on-primary-fixed text-[180px] fill-1" data-icon="center_focus_strong">center_focus_strong</span>
            <span className="material-symbols-outlined text-on-primary-fixed text-6xl" data-icon="arrow_forward">arrow_forward</span>
          </div>
          <div>
            <h3 className="text-[90px] font-black text-on-primary-fixed leading-tight">药品识别</h3>
            <p className="text-4xl font-bold text-on-primary-fixed/70 mt-4">智能扫描药盒信息</p>
          </div>
        </button>
      </div>

      <section className="p-12 bg-secondary-container rounded-lg flex items-center gap-8">
        <span className="material-symbols-outlined text-on-secondary-container text-8xl fill-1" data-icon="check_circle">check_circle</span>
        <div>
          <p className="text-on-background font-bold text-4xl">设备已连接</p>
          <p className="text-on-background/60 text-2xl mt-2">视觉引导系统运行中</p>
        </div>
      </section>
    </div>
  );
}

function ObstacleScreen() {
  return (
    <div className="flex flex-col">
      <header className="flex items-center w-full px-12 py-12 mb-4">
        <div className="flex items-center gap-6">
          <span className="material-symbols-outlined text-primary text-7xl" data-icon="record_voice_over">record_voice_over</span>
          <div>
            <h1 className="text-[5rem] leading-tight font-bold text-on-background">实时避障</h1>
            <p className="text-on-surface-variant font-medium text-3xl">暖阳语音助手已就绪</p>
          </div>
        </div>
      </header>

      <main className="px-12 flex flex-col gap-12">
        <div className="bg-error-container text-on-error-container p-12 rounded-[2rem] flex items-center gap-8 shadow-sm">
          <span className="material-symbols-outlined text-8xl fill-1" data-icon="warning">warning</span>
          <div className="flex flex-col">
            <span className="text-[4.5rem] font-black leading-none">前方 0.8米 危险</span>
            <span className="text-4xl font-bold mt-4">请向左避让</span>
          </div>
        </div>

        <div className="relative aspect-square w-full rounded-[3rem] bg-surface-container-low flex items-center justify-center overflow-hidden border border-outline-variant/20 shadow-inner">
          <div className="absolute inset-0 border-[32px] border-surface-container rounded-full scale-90"></div>
          <div className="absolute inset-0 border-[32px] border-surface-container rounded-full scale-75"></div>
          <div className="absolute inset-0 border-[32px] border-surface-container rounded-full scale-50"></div>
          <div className="absolute inset-0 radar-scan rounded-full opacity-50"></div>
          
          <div className="absolute top-24 left-1/2 -translate-x-1/2 w-32 h-32 bg-error rounded-full blur-3xl animate-pulse"></div>
          <div className="absolute top-32 left-1/2 -translate-x-1/2 w-8 h-8 bg-error rounded-full ring-[16px] ring-error-container"></div>
          <div className="absolute right-24 top-1/2 -translate-y-1/2 w-8 h-8 bg-primary rounded-full opacity-60"></div>
          <div className="absolute bottom-32 left-1/4 w-6 h-6 bg-secondary rounded-full opacity-40"></div>
          
          <div className="relative z-10 bg-on-background w-24 h-24 rounded-full flex items-center justify-center shadow-lg">
            <span className="material-symbols-outlined text-surface text-5xl fill-1" data-icon="person_pin_circle">person_pin_circle</span>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-8">
          <StatCard label="延迟" value="12ms" color="text-secondary" />
          <StatCard label="算力" value="8.5" unit="TFLOPS" color="text-primary" />
          <StatCard label="电量" value="18%" color="text-error" />
        </div>

        <div className="flex flex-col gap-10 mt-4">
          <div className="grid grid-cols-2 gap-8">
            <button className="h-[140px] bg-primary text-on-primary-fixed rounded-full flex items-center justify-center gap-6 font-bold text-3xl active:scale-95 transition-transform shadow-md">
              <span className="material-symbols-outlined text-4xl" data-icon="sensors_off">sensors_off</span>
              避障开关
            </button>
            <div className="h-[140px] bg-surface-container-highest rounded-full px-10 flex items-center gap-8 shadow-sm">
              <span className="material-symbols-outlined text-on-surface-variant text-4xl" data-icon="volume_up">volume_up</span>
              <div className="flex-1 bg-outline-variant h-4 rounded-full relative">
                <div className="absolute top-0 left-0 h-full w-2/3 bg-primary rounded-full"></div>
                <div className="absolute top-1/2 left-2/3 -translate-y-1/2 -translate-x-1/2 w-10 h-10 bg-primary rounded-full border-[6px] border-surface-container-lowest shadow-md"></div>
              </div>
            </div>
          </div>
          <button className="h-[140px] bg-surface-container-high text-on-surface rounded-full flex items-center justify-between px-12 font-bold text-3xl active:scale-95 transition-transform shadow-sm">
            <div className="flex items-center gap-6">
              <span className="material-symbols-outlined text-4xl" data-icon="tune">tune</span>
              灵敏度设置
            </div>
            <span className="text-primary font-black">高 (Ultra)</span>
          </button>
          <button className="h-[180px] bg-error text-on-error rounded-full flex items-center justify-center gap-8 font-black text-5xl active:scale-90 transition-all shadow-xl">
            <span className="material-symbols-outlined text-6xl fill-1" data-icon="emergency">emergency</span>
            紧急求助
          </button>
        </div>
      </main>
    </div>
  );
}

function StatCard({ label, value, unit, color }: { label: string; value: string; unit?: string; color: string }) {
  return (
    <div className="bg-surface-container-lowest p-8 rounded-[2rem] flex flex-col items-center justify-center shadow-sm">
      <span className="text-on-surface-variant text-xl font-bold uppercase tracking-widest mb-2">{label}</span>
      <span className={`text-5xl font-black ${color}`}>
        {value} {unit && <span className="text-lg">{unit}</span>}
      </span>
    </div>
  );
}

function RecognitionScreen({ onNavigate }: { onNavigate: (s: Screen) => void }) {
  return (
    <div className="flex flex-col px-12 pt-10">
      <header className="flex items-center gap-6 mb-6">
        <span className="material-symbols-outlined text-primary !text-[72px]" data-icon="record_voice_over">record_voice_over</span>
        <h1 className="text-[64px] leading-tight font-bold text-on-background">暖阳语音助手已就绪</h1>
      </header>

      <div className="flex flex-col gap-6">
        <div className="bg-secondary-container rounded-lg p-8 flex items-center gap-6">
          <span className="material-symbols-outlined text-on-secondary-container !text-[64px] fill-1" data-icon="check_circle">check_circle</span>
          <span className="text-on-secondary-container text-[40px] font-bold">系统状态正常，随时可以识别</span>
        </div>

        <div>
          <p className="text-primary text-[42px] font-bold mb-2">当前模式</p>
          <h2 className="text-[100px] font-black leading-tight tracking-tighter">药品识别</h2>
        </div>

        <button className="w-full aspect-[4/3.2] bg-primary-container rounded-xl flex flex-col items-center justify-center gap-8 active:scale-95 transition-transform shadow-xl overflow-hidden relative">
          <div className="w-56 h-56 bg-white/40 rounded-full flex items-center justify-center">
            <span className="material-symbols-outlined !text-[140px] text-on-primary-fixed fill-1" data-icon="center_focus_strong">center_focus_strong</span>
          </div>
          <span className="text-on-primary-fixed text-[72px] font-black">点击拍照识别</span>
        </button>
      </div>

      <div className="bg-surface-container-lowest rounded-xl p-10 border-[6px] border-primary-container shadow-sm mt-6">
        <div className="flex flex-col gap-6">
          <div className="flex items-center gap-6">
            <span className="material-symbols-outlined text-primary !text-[64px]" data-icon="pill">pill</span>
            <h3 className="text-primary text-[54px] font-bold">最新识别结果</h3>
          </div>
          <div className="flex flex-col gap-6">
            <div>
              <p className="text-on-surface-variant text-[38px] font-medium mb-1">药品名称</p>
              <p className="text-[88px] font-black leading-tight text-on-surface">阿莫西林胶囊</p>
            </div>
            <div className="h-2 bg-surface-container-low rounded-full"></div>
            <div>
              <p className="text-on-surface-variant text-[38px] font-medium mb-1">用法用量</p>
              <p className="text-[68px] font-bold text-on-surface">1粒/次，一日三次</p>
            </div>
          </div>
          <div className="mt-8 flex gap-6">
            <button className="flex-1 bg-secondary text-on-secondary py-8 rounded-full text-[42px] font-bold flex items-center justify-center gap-4">
              <span className="material-symbols-outlined !text-[50px]" data-icon="volume_up">volume_up</span>
              语音播报
            </button>
            <button 
              onClick={() => onNavigate('history')}
              className="flex-1 bg-surface-container-high text-on-surface py-8 rounded-full text-[42px] font-bold flex items-center justify-center gap-4"
            >
              <span className="material-symbols-outlined !text-[50px]" data-icon="history">history</span>
              历史
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function DeviceScreen() {
  return (
    <div className="flex flex-col px-12 pt-10">
      <header className="flex flex-col gap-6 mb-8">
        <div className="flex items-center gap-6">
          <span className="material-symbols-outlined text-primary text-7xl" data-icon="record_voice_over">record_voice_over</span>
          <h1 className="text-[5rem] leading-tight font-bold text-on-background">设备管理</h1>
        </div>
        <p className="text-on-surface-variant text-[2.5rem] font-bold pl-2">暖阳语音助手已就绪</p>
      </header>

      <div className="space-y-12">
        <div className="bg-secondary-container p-12 rounded-lg flex items-center gap-10">
          <span className="material-symbols-outlined text-on-secondary-container text-7xl fill-1" data-icon="shield">shield</span>
          <div>
            <h2 className="text-on-secondary-container font-black text-[3.25rem]">局域网运行状态良好</h2>
            <p className="text-on-secondary-container/80 font-bold text-[2rem]">所有核心通信模块正在稳定运行中</p>
          </div>
        </div>

        <section className="bg-surface-container-lowest p-12 rounded-lg flex flex-col gap-14 border border-outline-variant/30 shadow-sm">
          <div className="flex justify-between items-start">
            <div className="space-y-6">
              <span className="text-primary font-black tracking-[0.2em] text-[1.75rem] uppercase">主控制器</span>
              <h3 className="text-[5rem] font-black leading-none">ESP32-S3 核心</h3>
            </div>
            <div className="bg-secondary-fixed text-on-secondary-fixed px-10 py-6 rounded-full font-black text-3xl flex items-center gap-4">
              <span className="w-6 h-6 bg-secondary rounded-full animate-pulse"></span>
              已连接
            </div>
          </div>
          <div className="grid grid-cols-2 gap-10">
            <div className="bg-surface-container-low p-12 rounded-lg flex flex-col gap-6">
              <span className="material-symbols-outlined text-primary text-6xl" data-icon="speed">speed</span>
              <span className="text-[4.5rem] font-black">24ms</span>
              <span className="text-on-surface-variant text-[2rem] font-bold">响应延迟</span>
            </div>
            <div className="bg-surface-container-low p-12 rounded-lg flex flex-col gap-6">
              <span className="material-symbols-outlined text-primary text-6xl" data-icon="battery_horiz_075">battery_horiz_075</span>
              <span className="text-[4.5rem] font-black">92%</span>
              <span className="text-on-surface-variant text-[2rem] font-bold">剩余电量</span>
            </div>
          </div>
        </section>

        <section className="bg-primary-container p-12 rounded-lg space-y-10 shadow-lg">
          <div className="flex items-center gap-8">
            <div className="w-24 h-24 bg-white/40 rounded-full flex items-center justify-center">
              <span className="material-symbols-outlined text-on-primary-container text-6xl fill-1" data-icon="location_home">location_home</span>
            </div>
            <h4 className="text-on-primary-container font-black text-[3.5rem]">监护人定位</h4>
          </div>
          <div className="bg-white/60 p-8 rounded-lg">
            <p className="text-on-primary-container font-black text-[2.5rem] leading-tight">当前位置：北京市海淀区北三环西路</p>
            <p className="text-on-primary-container/70 font-bold text-[1.75rem] mt-2">最后更新：1分钟前</p>
          </div>
          <div className="flex gap-6">
            <button className="flex-1 bg-on-primary-container text-white h-32 rounded-full flex items-center justify-center gap-6 font-black text-[2.75rem] active:scale-95 transition-transform shadow-xl">
              <span className="material-symbols-outlined text-5xl" data-icon="map">map</span>
              地图查看
            </button>
            <button className="flex-1 bg-white text-on-primary-container h-32 rounded-full border-4 border-on-primary-container flex items-center justify-center gap-6 font-black text-[2.75rem] active:scale-95 transition-transform">
              <span className="material-symbols-outlined text-5xl" data-icon="share_location">share_location</span>
              发送位置
            </button>
          </div>
        </section>

        <section className="bg-surface-container p-12 rounded-lg space-y-10">
          <h4 className="text-[3rem] font-black">一键找寻</h4>
          <div className="grid grid-cols-2 gap-10">
            <button className="bg-white p-12 rounded-lg flex flex-col items-center gap-8 border-2 border-outline/10 shadow-md active:bg-primary/10 transition-colors">
              <span className="material-symbols-outlined text-primary text-[5rem]" data-icon="volume_up">volume_up</span>
              <span className="font-black text-[2.5rem] text-on-background">蜂鸣器</span>
            </button>
            <button className="bg-white p-12 rounded-lg flex flex-col items-center gap-8 border-2 border-outline/10 shadow-md active:bg-primary/10 transition-colors">
              <span className="material-symbols-outlined text-primary text-[5rem]" data-icon="flashlight_on">flashlight_on</span>
              <span className="font-black text-[2.5rem] text-on-background">灯光闪烁</span>
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}

function ProfileScreen({ onNavigate }: { onNavigate: (s: Screen) => void }) {
  return (
    <div className="flex flex-col">
      <nav className="bg-surface flex items-center w-full px-12 py-10 mb-12">
        <div className="flex items-center gap-6">
          <span className="material-symbols-outlined text-primary text-6xl" data-icon="record_voice_over">record_voice_over</span>
          <h1 className="text-[4rem] leading-tight font-bold text-on-background">语音助手已就绪</h1>
        </div>
      </nav>

      <main className="px-12 space-y-12">
        <section className="bg-surface-container-lowest rounded-lg p-12 flex items-center gap-10 shadow-sm">
          <div className="w-48 h-48 rounded-full overflow-hidden bg-primary-container flex-shrink-0">
            <img 
              alt="User Profile" 
              referrerPolicy="no-referrer"
              className="w-full h-full object-cover" 
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuDPfAXqgCAxVGavxeL1_rek4b2-fVcy7sWRKdpu6foi-7O3UxJxRrwujRVJ9DlQmCzc3beW5zgKIR3ODoERJNhAkFjFwFUwbOT_KAzfq8WiIjk0QuaGzs2gtNnEbkfJOriFvpDOcOhHTxzaG3UliJCI6rYPXM-kZNzyzK8yCF6SlXlrtB51QXJwb3VeP3_aoP-ehhkiQAwsTznkyoVvgXhiHnNexPz8Qm0WD6HROrzUECOEU69GGVozyqJv0daaXgx5Wd-DUvGCybFG" 
            />
          </div>
          <div className="flex flex-col">
            <h2 className="text-[4.5rem] font-black text-on-surface">林奶奶</h2>
            <p className="text-on-surface-variant text-[2rem] font-medium tracking-wide">ID: WS-2024-8892</p>
          </div>
        </section>

        <section className="grid grid-cols-1 gap-8">
          <button 
            onClick={() => onNavigate('history')}
            className="bg-primary-container text-on-primary-container rounded-lg p-12 flex items-center justify-between group active:scale-95 transition-transform shadow-md"
          >
            <div className="flex items-center gap-8">
              <span className="material-symbols-outlined text-7xl" data-icon="history">history</span>
              <span className="text-[3rem] font-bold">查看历史记录</span>
            </div>
            <span className="material-symbols-outlined text-6xl" data-icon="arrow_forward_ios">arrow_forward_ios</span>
          </button>
        </section>

        <section className="space-y-8">
          <div className="bg-surface-container-low rounded-lg p-4 space-y-4 shadow-sm">
            <ProfileMenuItem icon="settings_voice" label="语音引擎设置" />
            <ProfileMenuItem icon="sensors" label="避障灵敏度" />
            <ProfileMenuItem icon="psychology" label="大模型配置" />
            <ProfileMenuItem icon="info" label="软件版本" value="v2.4.0" />
          </div>
        </section>

        <section className="pt-8">
          <button className="w-full bg-error text-on-error rounded-full py-10 text-[2.5rem] font-black flex items-center justify-center gap-6 active:scale-95 transition-transform shadow-lg">
            <span className="material-symbols-outlined text-6xl" data-icon="logout">logout</span>
            退出登录
          </button>
        </section>
      </main>
    </div>
  );
}

function ProfileMenuItem({ icon, label, value }: { icon: string; label: string; value?: string }) {
  return (
    <button className="w-full bg-surface-container-lowest text-on-surface rounded-lg p-10 flex items-center justify-between hover:bg-zinc-100 active:scale-95 transition-all">
      <div className="flex items-center gap-8">
        <span className="material-symbols-outlined text-5xl text-on-surface-variant" data-icon={icon}>{icon}</span>
        <span className="text-[2.25rem] font-bold">{label}</span>
      </div>
      {value ? (
        <span className="text-[2.25rem] text-on-surface-variant font-bold">{value}</span>
      ) : (
        <span className="material-symbols-outlined text-5xl text-on-surface-variant" data-icon="chevron_right">chevron_right</span>
      )}
    </button>
  );
}

function HistoryScreen({ onNavigate }: { onNavigate: (s: Screen) => void }) {
  const historyItems = [
    { name: '阿莫西林', time: '今天 10:30', img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBnLc5Dy2mSlV3DQCv5dF9UuGSEDZolq3y3C63-fqGpoRyeUAnjIk5wUedtuCnlJwsXuItWVaUHHdY9fT4IiafGXp-NuipxkYSXe3yQZzHG8HznulXyJnP1xefXSOwv4qiFuWQqj_uhzSG_Lm215saRqJ3EmfKpQm5nl3KHkEMhL2VZh5IeRu9e7zvG_RAiOoGd0ztLMpuw2vcil0n252ZnG9NbHsyBiS-MCwF75JR7hFGSYqkAdBO_unamylRSzvAGQNqaWmVwteGD' },
    { name: '布洛芬口服液', time: '昨天 15:45', img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuA9dzuGerVESq0KIF1pFGZhGlnOBPQuhmCmDXnpPWCF1rP7o3o0gvN6Mr2SivjdK67VUGk1vLjOamfUY33CLlerFnf8ldFmY9cxsotWVpTwjlSQRECxqFtZ5YzVTcXu50O3Sn_htoBsAkCEl6kiwZSFUtXMWCYhQX8y52BFgy3qPfHoOSdOV9mty4Wy5MWO6cc49rwcaaqtLYOtiUmb0yBAXZS0dE_4kEvnDal4HRTCdDg-UhEtslS3yP6jII_qAEfw6RF00FVaOguD' },
    { name: '二甲双胍', time: '10月24日 08:20', img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAMpI3Dw-S4WROfXyvl01_FErkvppGMM3W9HnAr_vRyYc02_T7iZjZwztzJzHOLkAETJC7J7cDE7Q1s_0Sso-7vinHm6ej6YwT3DTV99onbFHEm_KzytYBuP25WoCiXzoXa3nWVOpnOGGcbaMc-iWPE-UZRkgKG4n4CpzoVt8DHpj9EglycZx76xDX_GDMK6T76MLAz7lXhvkryt6Kjcg-muLgBBVqzfp2X5iBeJJtIYzvWMWvgngaKAVZ1bbMedZk5I3CheizBrva4' },
    { name: '沙丁胺醇气雾剂', time: '10月23日 19:12', img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCVTFyySxC4du2yFQQIpKY5bJjTdL-Sa890Rbax4yul--qhZvzlUTfFA_pNqT5tRNSS3dlhe62XF1l4vC3hUZfCZRXJRXEOxsK9pvFoiNY-HOTdKXj8-PpFZ6MdZeQplLuV1zIn8xb4wA2NdoPj-2zB0edI9rQfQshYsNTsb8EycLAGwAncA915pqdWiZtxdD0Lo-Pioocy5MSb6e6rZ_ezccpjuIDnD4K0dbVaFHxipOoLwAvTtpcWVR1zFm7yu9fU9aMb829MZTug' },
    { name: '复合维生素片', time: '10月22日 09:00', img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBdUmXSdXcNATLh-eMe94e47EOl9-rzp0RvQgpOl8iNlndb5Vr06gq7DWCGE8TCogzU2PNaF6yr-55Q3t8HsiisMElVQ1Gz1Lp70KdY5ZrfIW05GnGIankZ1OKyELfZU5STtfANzDlFIlZKMFXZAYS0wy7_FY3zzsopj6Clp2NPX1S30j8ZfxJbCv08lam3z8NT1jpS0cl2YYda3FbMNAN9l7ZKbQwSDlLCIF4F7Kap5nj6mA7q2k1Avkhu4EOl-Y3ghyf16-XMq24p' },
    { name: '京都念慈菴', time: '10月21日 20:30', img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDAh5bph89_ZXDIgXTIU7HFuGLqYA4uuuHWITAXQ2nvrUbhcO1NO20cENEndqnHk6_DuZtD_F4uMGiYC1-8SFHPrK3KNo7K3N4j7t3q9A2KrCsDK8EmHHIWkQAMeuB4HVz-cFAbscsf77IUo-aNHbRECDSWOIgvH6SIOto0HJwiaBLjkrcs0a_8W-wXt3E4Bah7aNEChoS8GtpH5nsgHHCFdr4MDMwnGosACnuEmLAI3tAyHDg-JouLj64MaeIKESgztSQqzwcBiNvS' },
  ];

  return (
    <div className="flex flex-col">
      <header className="bg-surface flex flex-col w-full px-12 pt-16 pb-8 top-0 shrink-0">
        <div className="flex items-center justify-between mb-12">
          <button onClick={() => onNavigate('obstacle')} className="text-primary transition-transform duration-200 active:scale-95">
            <span className="material-symbols-outlined text-6xl">visibility</span>
          </button>
          <button className="text-primary transition-transform duration-200 active:scale-95">
            <span className="material-symbols-outlined text-6xl">mic</span>
          </button>
        </div>
        <div className="flex items-start space-x-4">
          <span className="material-symbols-outlined text-primary text-6xl mt-1">smart_toy</span>
          <h1 className="text-[2.75rem] leading-tight font-bold tracking-tight text-on-background">
            如果想知道哪天吃了什么药直接问我哦~
          </h1>
        </div>
      </header>

      <main className="flex-grow px-12 pb-8">
        <div className="flex items-center justify-between mb-8 mt-4">
          <h2 className="text-[2.25rem] font-bold text-on-surface">识别历史</h2>
          <div className="text-primary font-bold text-[1.75rem]">共 24 条</div>
        </div>
        <div className="space-y-6">
          {historyItems.map((item, index) => (
            <div key={index} className="bg-surface-container-lowest rounded-lg p-6 flex items-center space-x-6 transition-transform duration-200 active:scale-95 cursor-pointer border border-surface-container-high shadow-sm">
              <div className="w-40 h-40 rounded-default overflow-hidden bg-surface-container-high flex-shrink-0">
                <img alt={item.name} referrerPolicy="no-referrer" className="w-full h-full object-cover" src={item.img} />
              </div>
              <div className="flex-grow">
                <div className="text-[2.25rem] font-black text-on-surface mb-1">{item.name}</div>
                <div className="text-[1.5rem] text-outline font-medium">{item.time}</div>
              </div>
              <span className="material-symbols-outlined text-outline text-6xl">chevron_right</span>
            </div>
          ))}
        </div>
        <button className="mt-16 mb-8 w-full py-10 bg-primary-container rounded-lg flex items-center justify-center space-x-4 transition-transform duration-200 active:scale-[0.98] shadow-md">
          <span className="text-[2rem] font-black text-on-primary-fixed">查看更多历史记录</span>
          <span className="material-symbols-outlined text-5xl">expand_more</span>
        </button>
        <div className="text-center text-outline font-bold text-[1.5rem] mb-12">
          向上滑动查看更多记录
        </div>
      </main>
    </div>
  );
}
