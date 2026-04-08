import React, { useState, useEffect } from 'react';
import RowForm from './RowForm';
import DeviceList from './DeviceList';
import ScheduleList from './ScheduleList';
import { rowApi } from '../services/apiClient';
import { useNotification } from '../components/Notification';

const ZoneRows = ({ zoneId, greenhouseId }) => {
  const { showNotification } = useNotification();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isRowFormOpen, setIsRowFormOpen] = useState(false);
  const [editingRow, setEditingRow] = useState(null);
  const [rowFormMode, setRowFormMode] = useState('create');
  const [manageDevicesRowId, setManageDevicesRowId] = useState(null);
  const [manageSchedulesRowId, setManageSchedulesRowId] = useState(null);

  useEffect(() => {
    fetchRows();
  }, [zoneId]);

  const fetchRows = async () => {
    try {
      setLoading(true);
      const data = await rowApi.getByZone(zoneId);
      setRows(data || []);
    } catch (error) {
      console.error('Failed to fetch rows', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRow = (e) => {
    e.stopPropagation();
    setEditingRow(null);
    setRowFormMode('create');
    setIsRowFormOpen(true);
  };

  const handleEditRow = (e, row) => {
    e.stopPropagation();
    setEditingRow(row);
    setRowFormMode('edit');
    setIsRowFormOpen(true);
  };

  const handleThresholdRow = (e, row) => {
    e.stopPropagation();
    setEditingRow(row);
    setRowFormMode('threshold');
    setIsRowFormOpen(true);
  };

  const toggleManageDevices = (e, id) => {
    e.stopPropagation();
    setManageSchedulesRowId(null);
    setManageDevicesRowId(manageDevicesRowId === id ? null : id);
  };

  const toggleManageSchedules = (e, id) => {
    e.stopPropagation();
    setManageDevicesRowId(null);
    setManageSchedulesRowId(manageSchedulesRowId === id ? null : id);
  };

  const handleDeleteRow = async (e, id) => {
    e.stopPropagation();
    if (window.confirm('Are you sure you want to delete this row?')) {
      try {
        await rowApi.delete(id);
        showNotification('Row deleted');
        fetchRows();
      } catch (error) {
        showNotification('Failed to delete row: ' + error.message, 'error');
      }
    }
  };

  if (loading) return <div className="loading-small">Loading rows...</div>;

  return (
    <div className="zone-rows">
      <div className="zone-rows__header">
        <h5>Hàng cây (Rows)</h5>
        <button className="btn-add-mini" onClick={handleCreateRow}>+ Add</button>
      </div>

      <div className="rows-list">
        {rows.length > 0 ? (
          rows.map(row => (
            <div key={row.id} className="row-item-container">
              <div className="row-item">
                <div className="row-item__info">
                  <span className="row-name">{row.name}</span>
                  <span className="row-plant">{row.plantType || 'Unknown'}</span>
                  <span className={`status-tag status-tag--${row.currentMode?.toLowerCase() || 'auto'}`}>{row.currentMode}</span>
                </div>
                <div className="row-item__data">
                  <span className="moisture-val">💧 {row.lastSoilMoisture ? `${row.lastSoilMoisture.toFixed(1)}%` : '--'}</span>
                  <span className={`threshold-val threshold-val--${row.thresholdEnabled ? 'on' : 'off'}`}>
                    Threshold: {row.thresholdEnabled ? `${row.thresholdMin ?? '--'} - ${row.thresholdMax ?? '--'}%` : 'OFF'}
                  </span>
                  <span className={`pump-val pump-val--${row.pumpStatus === 'ON' ? 'on' : 'off'}`}>
                    Pump: {row.pumpStatus}
                  </span>
                </div>
                <div className="row-item__actions">
                  <button className="btn-mini" onClick={(e) => handleThresholdRow(e, row)} title="Set Threshold">🎯</button>
                  <button className="btn-mini" onClick={(e) => toggleManageSchedules(e, row.id)} title="Irrigation Schedules">🗓️</button>
                  <button className="btn-mini" onClick={(e) => toggleManageDevices(e, row.id)} title="Devices">🛠️</button>
                  <button className="btn-mini" onClick={(e) => handleEditRow(e, row)} title="Edit Row">✏️</button>
                  <button className="btn-mini btn-mini--danger" onClick={(e) => handleDeleteRow(e, row.id)} title="Delete Row">🗑️</button>
                </div>
              </div>
              
              {manageDevicesRowId === row.id && (
                <div className="row-expanded-panel">
                  <h6>Manage Devices (Sensors/Actuators)</h6>
                  <DeviceList 
                    greenhouseId={greenhouseId} 
                    zoneId={zoneId} 
                    rowId={row.id} 
                    type="sensor" 
                  />
                </div>
              )}

              {manageSchedulesRowId === row.id && (
                <div className="row-expanded-panel">
                  <h6>Irrigation Schedules (Lịch tưới)</h6>
                  <ScheduleList rowId={row.id} />
                </div>
              )}
            </div>
          ))
        ) : (
          <div className="empty-mini">No rows found</div>
        )}
      </div>

      <RowForm 
        isOpen={isRowFormOpen}
        onClose={() => setIsRowFormOpen(false)}
        onSubmit={() => { 
          setIsRowFormOpen(false); 
          showNotification(editingRow ? 'Row updated' : 'New row added');
          fetchRows(); 
        }}
        initialData={editingRow}
        mode={rowFormMode}
        zoneId={zoneId}
      />
    </div>
  );
};

export default ZoneRows;
