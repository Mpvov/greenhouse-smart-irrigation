import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { greenhouseApi, zoneApi } from '../services/apiClient';
import DeleteModal from '../components/DeleteModal';
import ZoneForm from './ZoneForm';
import { useNotification } from '../components/Notification';
import ZoneRows from './ZoneRows';
import './GreenhouseDetail.css';

const GreenhouseDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showNotification } = useNotification();
  const [greenhouse, setGreenhouse] = useState(null);
  const [zones, setZones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isZoneFormOpen, setIsZoneFormOpen] = useState(false);
  const [editingZone, setEditingZone] = useState(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [zoneToDelete, setZoneToDelete] = useState(null);

  useEffect(() => {
    fetchData();
  }, [id]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [ghData, zonesData] = await Promise.all([
        greenhouseApi.getById(id),
        zoneApi.getByGreenhouse(id)
      ]);
      setGreenhouse(ghData);
      setZones(zonesData || []);
    } catch (error) {
      console.error('Failed to fetch greenhouse details', error);
      if (error.response?.status === 404) {
        navigate('/greenhouses');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCreateZone = () => {
    setEditingZone(null);
    setIsZoneFormOpen(true);
  };

  const handleEditZone = (zone) => {
    setEditingZone(zone);
    setIsZoneFormOpen(true);
  };

  const handleDeleteZoneClick = (zone) => {
    setZoneToDelete(zone);
    setIsDeleteModalOpen(true);
  };

  const confirmDeleteZone = async () => {
    try {
      await zoneApi.delete(zoneToDelete.id);
      setIsDeleteModalOpen(false);
      showNotification(`Zone "${zoneToDelete.name}" deleted`);
      fetchData();
    } catch (error) {
      showNotification('Failed to delete zone: ' + error.message, 'error');
    }
  };

  if (loading) return <div className="loading">Loading details...</div>;
  if (!greenhouse) return <div className="error">Greenhouse not found.</div>;

  return (
    <div className="gh-detail">
      <div className="gh-detail__header">
        <button className="btn-back" onClick={() => navigate('/greenhouses')}>← Back</button>
        <div className="gh-info-main">
          <h2>{greenhouse.name}</h2>
          <p>📍 {greenhouse.location}</p>
        </div>
      </div>

      <div className="zones-section">
        <div className="zones-section__header">
          <h3>Phân khu (Zones)</h3>
          <button className="btn btn--primary" onClick={handleCreateZone}>+ Add Zone</button>
        </div>

        <div className="zones-grid">
          {zones.length > 0 ? (
            zones.map(zone => (
              <div key={zone.id} className="zone-card">
                <div className="zone-card__content">
                  <h4>{zone.name}</h4>
                  <div className="zone-stats">
                    <div className="stat-item">
                      <span className="stat-label">Temp</span>
                      <span className="stat-value">{zone.lastTemperature ? `${zone.lastTemperature}°C` : '--'}</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-label">Humidity</span>
                      <span className="stat-value">{zone.lastHumidity ? `${zone.lastHumidity}%` : '--'}</span>
                    </div>
                  </div>
                  <ZoneRows zoneId={zone.id} greenhouseId={id} />
                </div>
                <div className="zone-card__actions">
                  <button className="btn btn--icon" onClick={() => handleEditZone(zone)}>✏️</button>
                  <button className="btn btn--icon btn--icon-danger" onClick={() => handleDeleteZoneClick(zone)}>🗑️</button>
                </div>
              </div>
            ))
          ) : (
            <div className="empty-state">No zones defined in this greenhouse.</div>
          )}
        </div>
      </div>

      <ZoneForm
        isOpen={isZoneFormOpen}
        onClose={() => setIsZoneFormOpen(false)}
        onSubmit={() => {
          setIsZoneFormOpen(false);
          showNotification(editingZone ? 'Zone updated' : 'New zone added');
          fetchData();
        }}
        initialData={editingZone}
        greenhouseId={id}
      />

      {zoneToDelete && (
        <DeleteModal
          isOpen={isDeleteModalOpen}
          onClose={() => setIsDeleteModalOpen(false)}
          onConfirm={confirmDeleteZone}
          title={`Delete ${zoneToDelete.name}?`}
          message={`Are you sure you want to delete zone "${zoneToDelete.name}"? This will delete all rows and devices within it.`}
        />
      )}
    </div>
  );
};

export default GreenhouseDetail;
