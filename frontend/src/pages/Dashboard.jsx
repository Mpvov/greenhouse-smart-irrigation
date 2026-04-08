import React, { useState, useEffect } from 'react';
import { useAuth } from '../store/AuthContext';
import { useTelemetrySocket } from '../hooks/useTelemetrySocket';
import { ZoneCard } from '../components/ZoneCard';
import { rowApi } from '../services/apiClient';

function Dashboard() {
  const { token } = useAuth();
  const [monitoringTree, setMonitoringTree] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [togglingRowIds, setTogglingRowIds] = useState({});

  // 1. Fetch the hierarchy (Greenhouses -> Zones -> Rows)
  useEffect(() => {
    const fetchTree = async () => {
      try {
        const response = await fetch('/api/v1/monitoring/tree', {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        const result = await response.json();
        if (result.status === 200) {
          setMonitoringTree(result.data);
        } else {
          setFetchError(result.message || 'Failed to fetch monitoring tree');
        }
      } catch (err) {
        setFetchError('Network error while fetching monitoring tree');
      } finally {
        setIsLoading(false);
      }
    };

    if (token) fetchTree();
  }, [token]);

  // 2. Initialize WebSocket for real-time updates
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${wsProtocol}//${window.location.host}/ws/telemetry`;
  const { telemetryData, isConnected, error: wsError } = useTelemetrySocket(wsUrl, token);

  const handleTogglePump = async (rowId, currentStatus) => {
    if (!rowId || togglingRowIds[rowId]) return;

    const action = String(currentStatus).toUpperCase() === 'ON' ? 'OFF' : 'ON';

    setTogglingRowIds(prev => ({ ...prev, [rowId]: true }));
    try {
      await rowApi.controlPump(rowId, action);
    } catch (err) {
      console.error('[Dashboard] Failed to control pump', err);
    } finally {
      setTogglingRowIds(prev => {
        const cloned = { ...prev };
        delete cloned[rowId];
        return cloned;
      });
    }
  };

  if (isLoading) {
    return <div style={{ padding: '2rem', textAlign: 'center', color: '#888' }}>Loading monitoring system...</div>;
  }

  if (fetchError) {
    return <div style={{ padding: '2rem', color: '#ff5252' }}>Error: {fetchError}</div>;
  }

  return (
    <div className="page page--dashboard" style={{ padding: '1.5rem', background: '#121212', minHeight: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h2 className="page__title" style={{ margin: 0, fontSize: '1.8rem', fontWeight: '800' }}>Live Monitoring</h2>
        <div style={{ 
          display: 'flex',
          alignItems: 'center',
          gap: '0.8rem',
          padding: '0.6rem 1.2rem', 
          borderRadius: '30px', 
          background: isConnected ? 'rgba(76, 175, 80, 0.1)' : 'rgba(244, 67, 54, 0.1)',
          border: `1px solid ${isConnected ? '#4CAF50' : '#f44336'}`,
          color: isConnected ? '#4CAF50' : '#f44336',
          fontSize: '0.9rem',
          fontWeight: 'bold'
        }}>
          <span style={{ 
            width: '10px', 
            height: '10px', 
            borderRadius: '50%', 
            background: isConnected ? '#4CAF50' : '#f44336',
            boxShadow: isConnected ? '0 0 8px #4CAF50' : 'none'
          }}></span>
          {isConnected ? 'LIVE' : 'DISCONNECTED'}
        </div>
      </div>

      {wsError && <div style={{ color: '#ff5252', marginBottom: '1rem', background: 'rgba(255, 82, 82, 0.1)', padding: '0.8rem', borderRadius: '8px' }}>{wsError}</div>}

      <div className="greenhouses-wrapper">
        {monitoringTree.map(gh => (
          <div key={gh.info.id} className="greenhouse-section" style={{ marginBottom: '3rem' }}>
            <h3 style={{ borderBottom: '1px solid #333', paddingBottom: '0.5rem', marginBottom: '1.5rem', color: '#aaa', textTransform: 'uppercase', letterSpacing: '1px', fontSize: '1rem' }}>
              📍 {gh.info.name} - {gh.info.location}
            </h3>
            <div className="zones-container">
              {gh.zones.map(zoneTree => (
                <ZoneCard 
                  key={zoneTree.info.id} 
                  zoneId={zoneTree.info.id} 
                  name={zoneTree.info.name} 
                  rows={zoneTree.rows} 
                  telemetryData={telemetryData} 
                  initialTemp={zoneTree.info.lastTemperature}
                  initialHumidity={zoneTree.info.lastHumidity}
                  onTogglePump={handleTogglePump}
                  togglingRowIds={togglingRowIds}
                />
              ))}
            </div>
          </div>
        ))}
        {monitoringTree.length === 0 && <div style={{ color: '#666', textAlign: 'center' }}>No greenhouses found in current configuration.</div>}
      </div>
    </div>
  );
}

export default Dashboard;
