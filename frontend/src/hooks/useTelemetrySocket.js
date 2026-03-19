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
        const parsed = JSON.parse(event.data);
        const records = Array.isArray(parsed) ? parsed : [parsed];

        setTelemetryData((prevData) => {
          const newData = { ...prevData };

          records.forEach((record) => {
            // Support SenML array format or object with 'e' array
            const baseName = record.bn || '';
            const events = record.e ? record.e : [record];

            events.forEach((e) => {
              // Standardize topic name to match backend ID extraction: e.g. "userId/ghId/zId/rId/"
              const rawName = e.n || '';
              const parts = baseName.split('/').filter(Boolean);
              // if length is > 4, we assume [userid, ghid, zId, rId, metricName]
              const rowId = parts.length > 4 ? parts[3] : null;
              const zoneId = parts.length > 2 ? parts[2] : null;
              const deviceId = rowId || zoneId || 'unknown';
              
              // Key the state by deviceId for easy lookups in cards
              if (deviceId) {
                if (!newData[deviceId]) newData[deviceId] = {};
                
                // Map sensor names (t, h, soil) to readable keys
                const key = (rawName === 't' || rawName === 'temp') ? 'temperature' : 
                            (rawName === 'h' || rawName === 'humidity') ? 'humidity' : 
                            (rawName === 'soil' || rawName === 'soil_moisture') ? 'soilMoisture' : rawName;
                
                let timestamp = Date.now();
                if (e.t) {
                  // If timestamp is in seconds (length <= 10), convert to ms
                  timestamp = String(e.t).length <= 10 ? Number(e.t) * 1000 : Number(e.t);
                }
                
                newData[deviceId][key] = e.v;
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
