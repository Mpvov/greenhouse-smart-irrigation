import React from 'react';

export function RowCard({ id, name, plantType, mode, initialSoilMoisture, initialPumpStatus, data }) {
  // Values are directly available from data (telemetryData[id])
  const soilMoisture = data.soilMoisture !== undefined ? data.soilMoisture : initialSoilMoisture;
  const pumpStatus = data.pumpStatus !== undefined ? data.pumpStatus : initialPumpStatus;
  const lastUpdate = data.lastUpdate;

  // Offline calculation
  const isOffline = lastUpdate && (Date.now() - new Date(lastUpdate).getTime() > 5 * 60 * 1000); // 5 mins
  const isAlert = data.activeAlert;

  const getStatusColor = () => {
    if (isAlert) return '#ff5252';
    if (pumpStatus === 'ON') return '#4CAF50';
    return '#888';
  };

  return (
    <div className={`row-card ${isAlert ? 'alert' : ''} ${isOffline ? 'offline' : ''}`} 
      style={{ 
        padding: '1.2rem', 
        borderRadius: '10px', 
        background: isAlert ? 'rgba(255, 82, 82, 0.1)' : '#252525', 
        border: `1px solid ${isAlert ? '#ff5252' : '#333'}`,
        boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
        transition: 'transform 0.2s',
        opacity: isOffline ? 0.6 : 1,
        position: 'relative',
        overflow: 'hidden'
      }}>
      
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.8rem' }}>
        <h4 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 'bold' }}>{name}</h4>
        {isOffline && <span style={{ fontSize: '0.7rem', color: '#999' }}>Offline</span>}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
        <div style={{ background: '#1a1a1a', padding: '0.6rem', borderRadius: '6px' }}>
          <div style={{ fontSize: '0.75rem', color: '#888', marginBottom: '0.2rem' }}>SOIL MOISTURE</div>
          <div style={{ fontSize: '1.2rem', fontWeight: '800', color: soilMoisture < 30 ? '#ffb74d' : '#81c784' }}>
            {soilMoisture !== undefined ? `${soilMoisture.toFixed(1)}%` : '--'}
          </div>
        </div>
        
        <div style={{ background: '#1a1a1a', padding: '0.6rem', borderRadius: '6px' }}>
          <div style={{ fontSize: '0.75rem', color: '#888', marginBottom: '0.2rem' }}>PUMP STATUS</div>
          <div style={{ fontSize: '1.2rem', fontWeight: '800', color: getStatusColor() }}>
            {isAlert ? 'LOCKED' : (pumpStatus || '--')}
          </div>
        </div>
      </div>

      <div style={{ marginTop: '0.8rem', paddingTop: '0.8rem', borderTop: '1px solid #333', fontSize: '0.8rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ color: '#666' }}>MODE: <span style={{ color: '#aaa' }}>{mode || '--'}</span></span>
        {plantType && <span style={{ color: '#81c784', background: 'rgba(129, 199, 132, 0.1)', padding: '2px 6px', borderRadius: '4px' }}>🌱 {plantType}</span>}
        {lastUpdate && <span style={{ color: '#666' }}>{new Date(lastUpdate).toLocaleTimeString()}</span>}
      </div>
    </div>
  );
}
