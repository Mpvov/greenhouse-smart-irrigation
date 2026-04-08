import React, { useState, useEffect } from 'react';
import { scheduleApi } from '../services/apiClient';
import { useNotification } from '../components/Notification';

const MINUTES_IN_DAY = 24 * 60;

const toMinutes = (hhmm) => {
  const [hour, minute] = String(hhmm || '').split(':').map(Number);
  if (!Number.isInteger(hour) || !Number.isInteger(minute)) return NaN;
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return NaN;
  return hour * 60 + minute;
};

const hasOverlap = (candidate, existingList) => {
  const candidateStart = toMinutes(candidate.startTime);
  const candidateEnd = candidateStart + Number(candidate.duration);
  return existingList.some((s) => {
    if (s?.isActive === false) return false;
    const existingStart = toMinutes(s.startTime);
    const existingEnd = existingStart + Number(s.duration);
    return candidateStart < existingEnd && existingStart < candidateEnd;
  });
};

const ScheduleList = ({ rowId }) => {
  const { showNotification } = useNotification();
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newSchedule, setNewSchedule] = useState({ startTime: '08:00', duration: '15' });

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

    const startMinutes = toMinutes(newSchedule.startTime);
    const durationValue = Number(newSchedule.duration);

    if (Number.isNaN(startMinutes)) {
      showNotification('Giờ bắt đầu không hợp lệ (định dạng HH:mm).', 'error');
      return;
    }
    if (!Number.isInteger(durationValue) || durationValue <= 0 || durationValue > MINUTES_IN_DAY) {
      showNotification('Thời lượng tưới phải là số nguyên từ 1 đến 1440 phút.', 'error');
      return;
    }
    if (startMinutes + durationValue > MINUTES_IN_DAY) {
      showNotification('Lịch tưới phải kết thúc trong cùng một ngày.', 'error');
      return;
    }
    if (hasOverlap({ startTime: newSchedule.startTime, duration: durationValue }, schedules)) {
      showNotification('Lịch mới bị trùng với một lịch đang active.', 'error');
      return;
    }

    try {
      await scheduleApi.create({
        rowId,
        startTime: newSchedule.startTime,
        duration: durationValue,
      });
      showNotification('Đã thêm lịch tưới mới.');
      fetchSchedules();
      setNewSchedule({ startTime: '08:00', duration: '15' });
      setShowCreateForm(false);
    } catch (error) {
      const backendMessage = error?.response?.data?.message;
      showNotification('Không thể thêm lịch: ' + (backendMessage || error.message), 'error');
    }
  };

  const handleDelete = async (id) => {
    try {
      await scheduleApi.delete(id);
      showNotification('Đã xoá lịch tưới.');
      fetchSchedules();
    } catch (error) {
      showNotification('Không thể xoá lịch: ' + error.message, 'error');
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

      {!showCreateForm && (
        <button type="button" className="btn-mini" onClick={() => setShowCreateForm(true)}>
          Thêm lịch tưới mới
        </button>
      )}

      {showCreateForm && (
        <form className="schedule-form-mini" onSubmit={handleAddSchedule}>
          <div className="input-group-mini">
            <label>
              Giờ bắt đầu
              <input
                type="time"
                value={newSchedule.startTime}
                onChange={e => setNewSchedule({ ...newSchedule, startTime: e.target.value })}
                required
              />
            </label>
            <label>
              Thời lượng tưới (phút)
              <input
                type="number"
                placeholder="Phút"
                value={newSchedule.duration}
                onChange={e => setNewSchedule({ ...newSchedule, duration: e.target.value })}
                min="1"
                max="1440"
                step="1"
                required
                className="input-duration"
              />
            </label>
            <button type="submit" className="btn-mini">Áp dụng cấu hình</button>
            <button
              type="button"
              className="btn-mini-close"
              onClick={() => setShowCreateForm(false)}
              title="Đóng biểu mẫu"
            >
              ×
            </button>
          </div>
        </form>
      )}
    </div>
  );
};

export default ScheduleList;
