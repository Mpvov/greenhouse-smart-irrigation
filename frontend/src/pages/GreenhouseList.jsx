import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { greenhouseApi } from '../services/apiClient';
import DeleteModal from '../components/DeleteModal';
import GreenhouseForm from './GreenhouseForm';
import { useNotification } from '../components/Notification';
import './GreenhouseList.css';

const GreenhouseList = () => {
  const navigate = useNavigate();
  const { showNotification } = useNotification();
  const [greenhouses, setGreenhouses] = useState([]);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingGreenhouse, setEditingGreenhouse] = useState(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [ghToDelete, setGhToDelete] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchGreenhouses();
  }, []);

  const fetchGreenhouses = async () => {
    try {
      const data = await greenhouseApi.getAll();
      setGreenhouses(data || []);
    } catch (error) {
      console.error('Failed to fetch greenhouses', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingGreenhouse(null);
    setIsFormOpen(true);
  };

  const handleEdit = (gh) => {
    setEditingGreenhouse(gh);
    setIsFormOpen(true);
  };

  const handleDeleteClick = (gh) => {
    setGhToDelete(gh);
    setIsDeleteModalOpen(true);
  };

  const confirmDelete = async () => {
    try {
      await greenhouseApi.delete(ghToDelete.id);
      setIsDeleteModalOpen(false);
      showNotification(`Deleted ${ghToDelete.name} successfully`);
      fetchGreenhouses();
    } catch (error) {
      showNotification('Failed to delete greenhouse: ' + error.message, 'error');
    }
  };

  const onFormSubmit = () => {
    setIsFormOpen(false);
    showNotification(editingGreenhouse ? 'Greenhouse updated' : 'New greenhouse created');
    fetchGreenhouses();
  };

  if (loading) return <div className="loading">Loading Greenhouses...</div>;

  return (
    <div className="gh-manager">
      <div className="gh-manager__header">
        <h2>Greenhouse Management</h2>
        <button className="btn btn--primary" onClick={handleCreate}>+ Add Greenhouse</button>
      </div>

      <div className="gh-grid">
        {greenhouses.length > 0 ? (
          greenhouses.map(gh => (
            <div key={gh.id} className="gh-card">
              <div className="gh-card__info">
                <h3 className="gh-card__name-link" onClick={() => navigate(`/greenhouses/${gh.id}`)}>{gh.name}</h3>
                <p>📍 {gh.location || 'No location set'}</p>
                <small className="gh-id">ID: {gh.id}</small>
              </div>
              <div className="gh-card__actions">
                <button className="btn btn--secondary btn--sm" onClick={() => navigate(`/greenhouses/${gh.id}`)}>Detail</button>
                <button className="btn btn--icon" onClick={() => handleEdit(gh)}>✏️</button>
                <button className="btn btn--icon btn--icon-danger" onClick={() => handleDeleteClick(gh)}>🗑️</button>
              </div>
            </div>
          ))
        ) : (
          <div className="gh-empty">
            <p>No greenhouses found. Create your first one!</p>
          </div>
        )}
      </div>

      <GreenhouseForm 
        isOpen={isFormOpen} 
        onClose={() => setIsFormOpen(false)} 
        onSubmit={onFormSubmit}
        initialData={editingGreenhouse}
      />

      {ghToDelete && (
        <DeleteModal 
          isOpen={isDeleteModalOpen}
          onClose={() => setIsDeleteModalOpen(false)}
          onConfirm={confirmDelete}
          title={`Delete ${ghToDelete.name}?`}
          message={`Are you sure you want to delete "${ghToDelete.name}"? This action is irreversible.`}
        />
      )}
    </div>
  );
};

export default GreenhouseList;
