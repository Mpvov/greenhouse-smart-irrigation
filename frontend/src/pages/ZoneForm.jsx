import React, { useState, useEffect } from 'react';
import { zoneApi } from '../services/apiClient';
import { useNotification } from '../components/Notification';

const ZoneForm = ({ isOpen, onClose, onSubmit, initialData, greenhouseId }) => {
  const { showNotification } = useNotification();
  const [formData, setFormData] = useState({ name: '' });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (initialData) {
      setFormData({ name: initialData.name });
    } else {
      setFormData({ name: '' });
    }
  }, [initialData, isOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      if (initialData) {
        await zoneApi.update(initialData.id, formData);
      } else {
        await zoneApi.create({ ...formData, greenhouseId });
      }
      onSubmit();
    } catch (error) {
      showNotification('Error saving zone: ' + (error.response?.data?.message || error.message), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          <h3>{initialData ? 'Edit Zone' : 'Add New Zone'}</h3>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Zone Name</label>
            <input 
              type="text" 
              placeholder="e.g. Zone A - Vegetative"
              required 
              value={formData.name} 
              onChange={e => setFormData({ name: e.target.value })}
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn--secondary" onClick={onClose} disabled={submitting}>Cancel</button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {submitting ? 'Saving...' : 'Save Zone'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ZoneForm;
