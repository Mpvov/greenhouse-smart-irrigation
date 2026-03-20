import React from 'react';
import { useNavigate } from 'react-router-dom';
import './LandingPage.css';

const features = [
  {
    icon: '🌡️',
    title: 'Real-time Monitoring',
    desc: 'Track soil moisture, temperature, and humidity across every row and zone — live, in your browser.',
  },
  {
    icon: '🤖',
    title: 'Intelligent Automation',
    desc: 'AUTO mode activates pumps automatically when soil moisture drops below your threshold. No manual guesswork.',
  },
  {
    icon: '🏡',
    title: 'Hierarchical Management',
    desc: 'Organize your farm into Greenhouses → Zones → Rows. Each level is independently configurable.',
  },
  {
    icon: '📅',
    title: 'Irrigation Scheduling',
    desc: 'Set time-based irrigation schedules per row. The system triggers pumps precisely on time, every time.',
  },
  {
    icon: '📊',
    title: 'Historical Analytics',
    desc: 'Visualize time-series charts of past telemetry data to identify trends and optimize irrigation cycles.',
  },
  {
    icon: '🔌',
    title: 'Edge Device Integration',
    desc: 'Connect IoT sensors and actuators via MQTT. The platform handles data ingestion, parsing, and alerts.',
  },
];

const steps = [
  { num: '01', title: 'Create your Greenhouse', desc: 'Define your physical space — name it, set a location, and you\'re ready to add zones.' },
  { num: '02', title: 'Add Zones & Rows', desc: 'Divide your greenhouse into climate zones and individual crop rows with custom thresholds.' },
  { num: '03', title: 'Connect Devices', desc: 'Register your edge sensors and actuators. They\'ll automatically start streaming data.' },
  { num: '04', title: 'Sit back & monitor', desc: 'Watch live telemetry, get alerts, and let automation handle the irrigation.' },
];

export default function LandingPage() {
  const navigate = useNavigate();

  return (
    <div className="landing">
      {/* ── NAV ───────────────────────────────────────── */}
      <nav className="landing__nav">
        <div className="landing__nav-brand">
          <span className="landing__nav-logo">🌿</span>
          <span className="landing__nav-title">IrriSmart</span>
        </div>
        <div className="landing__nav-actions">
          <button className="landing__btn-ghost" onClick={() => navigate('/login')}>Log In</button>
          <button className="landing__btn-cta" onClick={() => navigate('/register')}>Get Started Free</button>
        </div>
      </nav>

      {/* ── HERO ──────────────────────────────────────── */}
      <section className="landing__hero">
        <div className="landing__hero-bg" aria-hidden="true">
          <div className="landing__orb landing__orb--1" />
          <div className="landing__orb landing__orb--2" />
          <div className="landing__orb landing__orb--3" />
        </div>
        <div className="landing__hero-content">
          <div className="landing__badge">🚀 Smart IoT Irrigation Platform</div>
          <h1 className="landing__hero-headline">
            Your greenhouse,<br />
            <span className="landing__gradient-text">always thriving.</span>
          </h1>
          <p className="landing__hero-sub">
            IrriSmart connects your IoT sensors and actuators to a real-time cloud dashboard.
            Automate irrigation, monitor every plant row, and never over- or under-water again.
          </p>
          <div className="landing__hero-ctas">
            <button className="landing__btn-primary" onClick={() => navigate('/register')}>
              Start for free →
            </button>
            <button className="landing__btn-secondary" onClick={() => navigate('/login')}>
              I already have an account
            </button>
          </div>
          <div className="landing__hero-stats">
            <div className="landing__stat">
              <span className="landing__stat-num">Real-time</span>
              <span className="landing__stat-label">Telemetry</span>
            </div>
            <div className="landing__stat-divider" />
            <div className="landing__stat">
              <span className="landing__stat-num">Auto</span>
              <span className="landing__stat-label">Irrigation Mode</span>
            </div>
            <div className="landing__stat-divider" />
            <div className="landing__stat">
              <span className="landing__stat-num">MQTT</span>
              <span className="landing__stat-label">IoT Protocol</span>
            </div>
          </div>
        </div>
      </section>

      {/* ── FEATURES ──────────────────────────────────── */}
      <section className="landing__section landing__features">
        <div className="landing__section-header">
          <p className="landing__section-eyebrow">Everything you need</p>
          <h2 className="landing__section-title">Built for modern precision farming</h2>
          <p className="landing__section-sub">
            From raw sensor data to intelligent automation — IrriSmart handles the full pipeline.
          </p>
        </div>
        <div className="landing__features-grid">
          {features.map((f) => (
            <div className="landing__feature-card" key={f.title}>
              <div className="landing__feature-icon">{f.icon}</div>
              <h3 className="landing__feature-title">{f.title}</h3>
              <p className="landing__feature-desc">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── ARCHITECTURE ──────────────────────────────── */}
      <section className="landing__section landing__arch">
        <div className="landing__section-header">
          <p className="landing__section-eyebrow">System Architecture</p>
          <h2 className="landing__section-title">Edge → Fog → Cloud</h2>
          <p className="landing__section-sub">
            A three-tier IoT architecture ensures low-latency control and scalable data storage.
          </p>
        </div>
        <div className="landing__arch-flow">
          <div className="landing__arch-node">
            <div className="landing__arch-icon">🔌</div>
            <h4>Edge Devices</h4>
            <p>Soil sensors, humidity sensors, pump actuators deployed per row</p>
          </div>
          <div className="landing__arch-arrow">→</div>
          <div className="landing__arch-node">
            <div className="landing__arch-icon">⚡</div>
            <h4>Fog Node</h4>
            <p>Local MQTT broker + Node-RED for pre-processing and command relay</p>
          </div>
          <div className="landing__arch-arrow">→</div>
          <div className="landing__arch-node landing__arch-node--highlight">
            <div className="landing__arch-icon">☁️</div>
            <h4>Cloud Platform</h4>
            <p>Spring WebFlux backend + Redis hot path + MongoDB cold path + React dashboard</p>
          </div>
        </div>
      </section>

      {/* ── HOW IT WORKS ──────────────────────────────── */}
      <section className="landing__section landing__how">
        <div className="landing__section-header">
          <p className="landing__section-eyebrow">Getting Started</p>
          <h2 className="landing__section-title">Up and running in minutes</h2>
        </div>
        <div className="landing__steps">
          {steps.map((s) => (
            <div className="landing__step" key={s.num}>
              <div className="landing__step-num">{s.num}</div>
              <div className="landing__step-body">
                <h4 className="landing__step-title">{s.title}</h4>
                <p className="landing__step-desc">{s.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA BANNER ────────────────────────────────── */}
      <section className="landing__cta-banner">
        <div className="landing__cta-orb" aria-hidden="true" />
        <h2 className="landing__cta-title">Ready to automate your greenhouse?</h2>
        <p className="landing__cta-sub">Create your free account and connect your first sensor in under 5 minutes.</p>
        <div className="landing__cta-actions">
          <button className="landing__btn-primary landing__btn-large" onClick={() => navigate('/register')}>
            Create Free Account
          </button>
          <button className="landing__btn-ghost landing__btn-large" onClick={() => navigate('/login')}>
            Sign In
          </button>
        </div>
      </section>

      {/* ── FOOTER ────────────────────────────────────── */}
      <footer className="landing__footer">
        <div className="landing__footer-brand">
          <span>🌿</span> IrriSmart — Smart IoT Irrigation System
        </div>
        <p className="landing__footer-copy">© 2026 Thesis Project · Spring WebFlux + React + MQTT</p>
      </footer>
    </div>
  );
}
