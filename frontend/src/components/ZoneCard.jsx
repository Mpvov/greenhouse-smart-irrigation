import React from 'react';
import { RowCard } from './RowCard';

export function ZoneCard({ zoneId, name, rows, telemetryData, initialTemp, initialHumidity }) {
  // telemetryData[zoneId] is populated by useTelemetrySocket.js
  const zoneData = telemetryData[zoneId] || {};
  const temp = zoneData.temperature !== undefined ? zoneData.temperature : initialTemp;
  const humidity = zoneData.humidity !== undefined ? zoneData.humidity : initialHumidity;

  return (
    <div className="card zone-card" style={{ border: '1px solid #555', padding: '1rem', margin: '1rem 0', borderRadius: '12px', background: '#1e1e1e', color: '#fff', boxShadow: '0 4px 6px rgba(0,0,0,0.3)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h3 style={{ margin: 0, fontSize: '1.4rem', color: '#fff' }}>{name}</h3>
        <div style={{ display: 'flex', gap: '1.5rem', color: '#4CAF50', fontWeight: 'bold' }}>
          <div>🌡 {temp ? `${temp.toFixed(1)}°C` : '--'}</div>
          <div>💧 {humidity ? `${humidity.toFixed(1)}%` : '--'}</div>
        </div>
      </div>
      
      <div className="rows-container" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1rem' }}>
        {rows.map(row => (
          <RowCard 
            key={row.id} 
            id={row.id} 
            name={row.name} 
            plantType={row.plantType}
            mode={row.currentMode}
            initialSoilMoisture={row.lastSoilMoisture}
            initialPumpStatus={row.pumpStatus}
            data={telemetryData[row.id] || {}} 
          />
        ))}
      </div>
    </div>
  );
}
