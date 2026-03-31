import { useState, useEffect, useRef } from 'react';

export function useTelemetrySocket(url, token, fallbackUserId = "1") {
  const [telemetryData, setTelemetryData] = useState({});
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState(null);
  const ws = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const reconnectAttempts = useRef(0);

  const connect = () => {
    if (!token) return;

    // Clear any existing reconnect timer
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }

    const wsUrl = token ? `${url}?token=${token}` : `${url}?userId=${fallbackUserId}`;
    console.log(`[WebSocket] Attempting connection to ${url}...`);
    
    const socket = new WebSocket(wsUrl);
    ws.current = socket;

    socket.onopen = () => {
      console.log('[WebSocket] Connected successfully');
      setIsConnected(true);
      setError(null);
      reconnectAttempts.current = 0; // Reset attempts on success
    };

    socket.onclose = (event) => {
      setIsConnected(false);
      console.log(`[WebSocket] Closed. Code: ${event.code}. Reconnecting...`);
      
      // Don't reconnect if it was a clean close (intentional)
      if (event.code !== 1000 && event.code !== 1001) {
        const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 30000);
        reconnectTimeoutRef.current = setTimeout(() => {
          reconnectAttempts.current += 1;
          connect();
        }, delay);
      }
    };

    socket.onerror = (err) => {
      console.error('[WebSocket] Error occurred:', err);
      setError('WebSocket connection error');
    };

    socket.onmessage = (event) => {
      try {
        const parsedWrapper = JSON.parse(event.data);
        // The backend now wraps SenML with physical IDs: 
        // { userId, ghId, zId, rId, data: { bn: ..., e: [...] } }
        
        let physicalGhId = null;
        let physicalZoneId = null;
        let physicalRowId = null;
        let records = [];

        if (parsedWrapper.data) {
          // New enriched format
          physicalGhId = parsedWrapper.ghId;
          physicalZoneId = parsedWrapper.zId;
          physicalRowId = parsedWrapper.rId;
          records = Array.isArray(parsedWrapper.data) ? parsedWrapper.data : [parsedWrapper.data];
        } else {
          // Fallback legacy format
          records = Array.isArray(parsedWrapper) ? parsedWrapper : [parsedWrapper];
        }

        setTelemetryData((prevData) => {
          const newData = { ...prevData };

          records.forEach((record) => {
            // Support SenML array format or object with 'e' array
            const baseName = record.bn || '';
            const events = record.e ? record.e : [record];

            events.forEach((e) => {
              // Priority 1: Use enriched physical IDs from backend if available
              let deviceId = physicalRowId || physicalZoneId;

              // Priority 2: Fallback logic extracting from topic (bn)
              if (!deviceId) {
                const rawParts = baseName.split('/').filter(Boolean);
                const parts = rawParts.map(part => {
                  if (part.startsWith('user_')) return part.substring(5);
                  if (part.startsWith('gh_')) return part.substring(3);
                  if (part.startsWith('z_')) return part.substring(2);
                  if (part.startsWith('r_')) return part.substring(2);
                  return part;
                });
                const rowId = rawParts.length > 4 ? parts[3] : null;
                const zoneId = rawParts.length > 2 ? parts[2] : null;
                deviceId = rowId || zoneId || 'unknown';
              }
              
              // Key the state by deviceId for easy lookups in cards
              if (deviceId) {
                if (!newData[deviceId]) newData[deviceId] = {};
                
                // Map sensor names (t, h, soil) to readable keys
                const rawName = e.n || '';
                const key = (rawName === 't' || rawName === 'temp') ? 'temperature' :
                            (rawName === 'h' || rawName === 'humidity') ? 'humidity' :
                            (rawName === 'soil' || rawName === 'soil_moisture') ? 'soilMoisture' :
                            (rawName === 'pump' || rawName === 'pump_status') ? 'pumpStatus' : rawName;
                
                let timestamp = Date.now();
                if (e.t) {
                  // If timestamp is in seconds (length <= 10), convert to ms
                  timestamp = String(e.t).length <= 10 ? Number(e.t) * 1000 : Number(e.t);
                }
                
                const normalizedValue = key === 'pumpStatus'
                  ? (Number(e.v) > 0 || String(e.v).toUpperCase() === 'ON' ? 'ON' : 'OFF')
                  : e.v;

                newData[deviceId][key] = normalizedValue;
                newData[deviceId].lastUpdate = timestamp;
                
                // Alert logic
                if (String(e.v).includes('FAULT') || String(e.v).includes('LOCKED')) {
                  newData[deviceId].activeAlert = true;
                }
              }
            });
          });

          return newData;
        });
      } catch (err) {
        console.error('[WebSocket] Parse error:', err);
      }
    };
  };

  useEffect(() => {
    connect();

    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      if (ws.current) {
        console.log('[WebSocket] Cleaning up connection...');
        ws.current.close(1000); // 1000: Normal Closure
      }
    };
  }, [url, token]);

  return { telemetryData, isConnected, error };
}
