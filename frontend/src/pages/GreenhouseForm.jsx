import React, { useState, useEffect } from 'react';
import { greenhouseApi } from '../services/apiClient';
import { useNotification } from '../components/Notification';

const GreenhouseForm = ({ isOpen, onClose, onSubmit, initialData }) => {
  const { showNotification } = useNotification();
  const [formData, setFormData] = useState({ name: '', location: '' });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (initialData) {
      setFormData({ name: initialData.name, location: initialData.location || '' });
    } else {
      setFormData({ name: '', location: '' });
    }
  }, [initialData, isOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      if (initialData) {
        await greenhouseApi.update(initialData.id, formData);
      } else {
        await greenhouseApi.create(formData);
      }
      onSubmit();
    } catch (error) {
      showNotification('Error saving greenhouse: ' + (error.response?.data?.message || error.message), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          <h3>{initialData ? 'Edit Greenhouse' : 'Add New Greenhouse'}</h3>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Greenhouse Name</label>
            <input 
              type="text" 
              placeholder="e.g. My North Greenhouse"
              required 
              value={formData.name} 
              onChange={e => setFormData({...formData, name: e.target.value})}
            />
          </div>
          <div className="form-group">
            <label>Location (Optional)</label>
            <input 
              type="text" 
              placeholder="e.g. Zone A, Building 2"
              value={formData.location} 
              onChange={e => setFormData({...formData, location: e.target.value})}
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn--secondary" onClick={onClose} disabled={submitting}>Cancel</button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {submitting ? 'Saving...' : 'Save Greenhouse'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default GreenhouseForm;
