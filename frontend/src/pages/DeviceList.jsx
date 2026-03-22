import React, { useState, useEffect } from 'react';
import { deviceApi } from '../services/apiClient';
import { useNotification } from '../components/Notification';

const DeviceList = ({ greenhouseId, zoneId, rowId, type }) => {
  const { showNotification } = useNotification();
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newDeviceId, setNewDeviceId] = useState('');
  const [newDeviceType, setNewDeviceType] = useState(type === 'sensor' ? 'DHT20' : 'PUMP');

  useEffect(() => {
    fetchDevices();
  }, [greenhouseId, zoneId, rowId]);

  const fetchDevices = async () => {
    try {
      setLoading(true);
      // For now, only get by greenhouse and filter locally or we can use more specific API
      const data = await deviceApi.getByGreenhouse(greenhouseId);
      let filtered = data || [];
      if (rowId) {
        filtered = filtered.filter(d => d.rowId === rowId);
      } else if (zoneId) {
        filtered = filtered.filter(d => d.zoneId === zoneId && !d.rowId);
      }
      setDevices(filtered);
    } catch (error) {
      console.error('Failed to fetch devices', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddDevice = async (e) => {
    e.preventDefault();
    if (!newDeviceId) return;
    try {
      await deviceApi.create({
        id: newDeviceId,
        type: newDeviceType,
        greenhouseId,
        zoneId,
        rowId
      });
      showNotification('Device added: ' + newDeviceId);
      setNewDeviceId('');
      fetchDevices();
    } catch (error) {
      showNotification('Failed to add device: ' + error.message, 'error');
    }
  };

  const handleDeleteDevice = async (id) => {
    if (window.confirm('Delete device?')) {
      try {
        await deviceApi.delete(id);
        showNotification('Device removed');
        fetchDevices();
      } catch (error) {
        showNotification('Failed to delete device: ' + error.message, 'error');
      }
    }
  };

  if (loading) return <div className="loading-small">Loading devices...</div>;

  return (
    <div className="device-manager">
      <div className="device-list">
        {devices.map(d => (
          <div key={d.id} className="device-tag">
            <span>{d.type}: {d.id}</span>
            <button className="btn-close-mini" onClick={() => handleDeleteDevice(d.id)}>×</button>
          </div>
        ))}
      </div>
      <form className="device-add-form" onSubmit={handleAddDevice}>
        <input 
          type="text" 
          value={newDeviceId} 
          onChange={(e) => setNewDeviceId(e.target.value)} 
          placeholder="Device ID (MAC)"
          required
        />
        <select value={newDeviceType} onChange={(e) => setNewDeviceType(e.target.value)}>
          <option value="DHT20">DHT20 (Temp/Hum)</option>
          <option value="SOIL_MOISTURE">Soil Moisture</option>
          <option value="PUMP">Pump</option>
        </select>
        <button type="submit" className="btn-mini">+</button>
      </form>
    </div>
  );
};

export default DeviceList;
