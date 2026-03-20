import React, { useState, useEffect } from 'react';
import { scheduleApi } from '../services/apiClient';
import { useNotification } from '../components/Notification';

const ScheduleList = ({ rowId }) => {
  const { showNotification } = useNotification();
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newSchedule, setNewSchedule] = useState({ startTime: '08:00', duration: 15 });

  useEffect(() => {
    fetchSchedules();
  }, [rowId]);

  const fetchSchedules = async () => {
    try {
      setLoading(true);
      const data = await scheduleApi.getByRow(rowId);
      setSchedules(data || []);
    } catch (error) {
      console.error('Failed to fetch schedules', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddSchedule = async (e) => {
    e.preventDefault();
    try {
      await scheduleApi.create({ ...newSchedule, rowId });
      showNotification('Schedule added');
      fetchSchedules();
      setNewSchedule({ startTime: '08:00', duration: 15 });
    } catch (error) {
      showNotification('Failed to add schedule: ' + error.message, 'error');
    }
  };

  const handleDelete = async (id) => {
    try {
      await scheduleApi.delete(id);
      showNotification('Schedule removed');
      fetchSchedules();
    } catch (error) {
      showNotification('Failed to delete schedule: ' + error.message, 'error');
    }
  };

  if (loading) return <div className="loading-mini">...</div>;

  return (
    <div className="schedule-section">
      <div className="schedule-list">
        {schedules.length > 0 ? schedules.map(s => (
          <div key={s.id} className="schedule-item">
            <span className="schedule-time">⏰ {s.startTime}</span>
            <span className="schedule-duration">{s.duration} min</span>
            <button className="btn-mini-close" onClick={() => handleDelete(s.id)} title="Delete schedule">×</button>
          </div>
        )) : <span className="no-schedules">No schedules set</span>}
      </div>
      <form className="schedule-form-mini" onSubmit={handleAddSchedule}>
        <div className="input-group-mini">
          <input 
            type="time" 
            value={newSchedule.startTime} 
            onChange={e => setNewSchedule({...newSchedule, startTime: e.target.value})}
            required
          />
          <input 
            type="number" 
            placeholder="Min" 
            value={newSchedule.duration} 
            onChange={e => setNewSchedule({...newSchedule, duration: parseInt(e.target.value)})}
            min="1" 
            max="120"
            required
            className="input-duration"
          />
          <button type="submit" className="btn-add-circle">+</button>
        </div>
      </form>
    </div>
  );
};

export default ScheduleList;
