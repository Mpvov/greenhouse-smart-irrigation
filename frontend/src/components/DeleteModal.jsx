import React from 'react';
import './DeleteModal.css';

const DeleteModal = ({ isOpen, onClose, onConfirm, title, message }) => {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content modal-content--danger">
        <div className="modal-header">
          <h2 className="modal-title">⚠️ {title || 'Confirm Deletion'}</h2>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>
        <div className="modal-body">
          <p className="danger-text">{message || 'This action cannot be undone.'}</p>
          <div className="alert-box">
            <strong>WARNING:</strong> This will permanently delete all associated zones, rows, devices, and historical data.
          </div>
        </div>
        <div className="modal-footer">
          <button className="btn btn--secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn--danger" onClick={onConfirm}>Delete Everything</button>
        </div>
      </div>
    </div>
  );
};

export default DeleteModal;
