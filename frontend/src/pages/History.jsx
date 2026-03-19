import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';

function History() {
  const [historyData, setHistoryData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const [selectedDevice, setSelectedDevice] = useState("1"); 
  const [hours, setHours] = useState(24);

  useEffect(() => {
    const fetchHistory = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await axios.get(`/api/v1/monitoring/history/${selectedDevice}?hours=${hours}`);
        if (response.data.status === 200) {
          const records = response.data.data.telemetryData || [];
          
          // Pivot measurement records by timestamp into a format recharts expects:
          // { time: "...", temperature: 25, humidity: 60 }
          const pivoted = {};
          
          records.forEach(r => {
             const dt = new Date(r.timestamp);
             const timeKey = dt.toLocaleString();
             
             if (!pivoted[timeKey]) {
                pivoted[timeKey] = { 
                   time: timeKey, 
                   rawTime: dt.getTime() 
                };
             }
             
             // Normalize keys since Mock DB and Node-RED emit varying string names
             let key = r.measurement;
             if (key === 'temp' || key === 't') key = 'temperature';
             if (key === 'soil' || key === 'soil_moisture') key = 'soilMoisture';
             if (key === 'h') key = 'humidity';
             
             pivoted[timeKey][key] = r.value;
          });
          
          // Sort chronologically
          const chartData = Object.values(pivoted).sort((a,b) => a.rawTime - b.rawTime);
          setHistoryData(chartData);
        } else {
          setError(response.data.message || 'Failed to fetch history');
        }
      } catch (err) {
        setError(err.message || 'An error occurred while fetching data');
      } finally {
        setLoading(false);
      }
    };
    fetchHistory();
  }, [selectedDevice, hours]);

  return (
    <div className="page page--history">
      <h2 className="page__title">Telemetry History</h2>
      
      <div style={{ marginBottom: '1.5rem', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
        <label style={{ display: 'flex', alignItems: 'center', fontWeight: 'bold' }}>
          Device Selection:
          <select 
            value={selectedDevice} 
            onChange={e => setSelectedDevice(e.target.value)} 
            style={{ marginLeft: '0.5rem', background: '#252525', color: '#fff', border: '1px solid #555', padding: '0.4rem', borderRadius: '4px' }}
          >
            <option value="1">Zone 1 / Row 1</option>
          </select>
        </label>
        
        <label style={{ display: 'flex', alignItems: 'center', fontWeight: 'bold' }}>
          Time Range:
          <select 
            value={hours} 
            onChange={e => setHours(e.target.value)} 
            style={{ marginLeft: '0.5rem', background: '#252525', color: '#fff', border: '1px solid #555', padding: '0.4rem', borderRadius: '4px' }}
          >
            <option value="1">Last 1 Hour</option>
            <option value="6">Last 6 Hours</option>
            <option value="24">Last 24 Hours</option>
            <option value="168">Last 7 Days</option>
          </select>
        </label>
      </div>

      {loading && <p style={{ color: '#888' }}>Loading historical data...</p>}
      {error && <p style={{ color: '#ff5252', background: 'rgba(255,82,82,0.1)', padding: '1rem', borderRadius: '8px' }}>{error}</p>}

      {!loading && !error && historyData.length === 0 && (
        <p style={{ color: '#888' }}>No data records available for this period.</p>
      )}
      
      {!loading && !error && historyData.length > 0 && (
        <div style={{ width: '100%', height: 450, background: '#1c1c1c', padding: '1.5rem 1rem 0 0', borderRadius: '12px', boxShadow: '0 4px 6px rgba(0,0,0,0.3)', border: '1px solid #333' }}>
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={historyData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#333" vertical={false} />
              <XAxis 
                dataKey="time" 
                stroke="#888" 
                tick={{fill: '#888', fontSize: 12}}
                tickFormatter={(val) => {
                  // Simplify time string for display (HH:MM or MM/DD)
                  try {
                    const parts = val.split(', ');
                    return parts[1] || val;
                  } catch {
                    return val;
                  }
                }}
              />
              <YAxis stroke="#888" tick={{fill: '#888', fontSize: 12}} />
              <Tooltip 
                contentStyle={{ backgroundColor: '#252525', border: '1px solid #555', color: '#fff', borderRadius: '8px' }} 
                itemStyle={{ fontWeight: 'bold' }}
              />
              <Legend wrapperStyle={{ paddingTop: '20px' }} />
              
              <Line 
                type="monotone" 
                name="Temperature (°C)"
                dataKey="temperature" 
                stroke="#ffb74d" 
                strokeWidth={3} 
                dot={false}
                activeDot={{ r: 6 }} 
              />
              <Line 
                type="monotone" 
                name="Soil Moisture (%)"
                dataKey="soilMoisture" 
                stroke="#81c784" 
                strokeWidth={3} 
                dot={false}
                activeDot={{ r: 6 }} 
              />
              <Line 
                type="monotone" 
                name="Air Humidity (%)"
                dataKey="humidity" 
                stroke="#64b5f6" 
                strokeWidth={3} 
                dot={false}
                activeDot={{ r: 6 }} 
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}

export default History;
