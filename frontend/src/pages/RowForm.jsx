import React, { useState, useEffect } from 'react';
import { rowApi } from '../services/apiClient';
import { useNotification } from '../components/Notification';

const RowForm = ({ isOpen, onClose, onSubmit, initialData, zoneId }) => {
  const { showNotification } = useNotification();
  const [formData, setFormData] = useState({
    name: '',
    plantType: '',
    currentMode: 'AUTO',
    thresholdEnabled: false,
    thresholdMin: 30,
    thresholdMax: 70
  });

  useEffect(() => {
    if (initialData) {
      setFormData({
        name: initialData.name || '',
        plantType: initialData.plantType || '',
        currentMode: initialData.currentMode || 'AUTO',
        thresholdEnabled: Boolean(initialData.thresholdEnabled),
        thresholdMin: initialData.thresholdMin || 30,
        thresholdMax: initialData.thresholdMax || 70
      });
    } else {
      setFormData({
        name: '',
        plantType: '',
        currentMode: 'AUTO',
        thresholdEnabled: false,
        thresholdMin: 30,
        thresholdMax: 70
      });
    }
  }, [initialData, isOpen]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox'
        ? checked
        : (name === 'thresholdMin' || name === 'thresholdMax') ? parseFloat(value) : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (initialData) {
        await rowApi.update(initialData.id, formData);
      } else {
        await rowApi.create({ ...formData, zoneId });
      }
      onSubmit();
    } catch (error) {
      showNotification('Failed to save row: ' + error.message, 'error');
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <h3>{initialData ? 'Edit Row' : 'Add New Row'}</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Row Name</label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="e.g. Row 1"
              required
            />
          </div>
          <div className="form-group">
            <label>Plant Type</label>
            <input
              type="text"
              name="plantType"
              value={formData.plantType}
              onChange={handleChange}
              placeholder="e.g. Tomato"
            />
          </div>
          <div className="form-group">
            <label>Mode</label>
            <select name="currentMode" value={formData.currentMode} onChange={handleChange}>
              <option value="AUTO">Automatic</option>
              <option value="MANUAL">Manual</option>
            </select>
          </div>
          <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <input
              type="checkbox"
              id="thresholdEnabled"
              name="thresholdEnabled"
              checked={formData.thresholdEnabled}
              onChange={handleChange}
            />
            <label htmlFor="thresholdEnabled" style={{ margin: 0 }}>Enable Moisture Threshold Control</label>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Min Moisture (%)</label>
              <input
                type="number"
                name="thresholdMin"
                value={formData.thresholdMin}
                onChange={handleChange}
                min="0"
                max="100"
                disabled={!formData.thresholdEnabled}
              />
            </div>
            <div className="form-group">
              <label>Max Moisture (%)</label>
              <input
                type="number"
                name="thresholdMax"
                value={formData.thresholdMax}
                onChange={handleChange}
                min="0"
                max="100"
                disabled={!formData.thresholdEnabled}
              />
            </div>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn--secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn--primary">Save</button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RowForm;
