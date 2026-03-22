// =============================================================================
// init-db.js — MongoDB Initialization Script for Smart Irrigation System
//
// This script runs automatically on first startup of the MongoDB container
// (mounted via Docker Compose into /docker-entrypoint-initdb.d/).
//
// Responsibilities:
//   1. Switch to the application database: irrigation_db
//   2. Create the Time-Series collection for telemetry data (data_records)
//   3. Create standard collections for all other entities
//   4. Create indexes as defined in the database schema
// =============================================================================

// --- Step 1: Switch to the application database ---
db = db.getSiblingDB('irrigation_db');

print('✅ [init-db] Switched to database: irrigation_db');

// =============================================================================
// --- Step 2: Time-Series Collection (CRITICAL) ---
// data_records MUST be created as a Time-Series collection BEFORE any data
// is inserted, otherwise MongoDB will create it as a standard collection.
// timeField  = "timestamp" : primary time dimension
// metaField  = "deviceId"  : groups series by device for efficient lookups
// =============================================================================
db.createCollection('data_records', {
    timeseries: {
        timeField: 'timestamp',
        metaField: 'deviceId',
        granularity: 'seconds'  // IoT data is high-frequency, seconds granularity is optimal
    },
    expireAfterSeconds: 60 * 60 * 24 * 90  // TTL: Auto-purge data older than 90 days
});

print('✅ [init-db] Time-Series collection created: data_records (timeField: timestamp, metaField: deviceId)');

// =============================================================================
// --- Step 3: Standard Collections ---
// =============================================================================
const standardCollections = [
    'users',
    'greenhouses',
    'zones',
    'rows',
    'devices',
    'schedules',
    'control_logs',
    'alerts'
];

standardCollections.forEach(function(collectionName) {
    db.createCollection(collectionName);
    print('✅ [init-db] Standard collection created: ' + collectionName);
});

// =============================================================================
// --- Step 4: Indexes ---
// Based on schema access patterns for optimal query performance.
// =============================================================================

// users: Unique index on email (login lookup + uniqueness enforcement)
db.users.createIndex({ email: 1 }, { unique: true, name: 'idx_users_email_unique' });

// greenhouses: Index on userId (load all greenhouses for a user)
db.greenhouses.createIndex({ userId: 1 }, { name: 'idx_greenhouses_userId' });

// zones: Index on greenhouseId (load tree: greenhouse -> zones)
db.zones.createIndex({ greenhouseId: 1 }, { name: 'idx_zones_greenhouseId' });

// rows: Index on zoneId (load tree: zone -> rows)
db.rows.createIndex({ zoneId: 1 }, { name: 'idx_rows_zoneId' });

// devices: Separate indexes on zoneId and rowId (devices can belong to either)
db.devices.createIndex({ zoneId: 1 }, { name: 'idx_devices_zoneId', sparse: true });
db.devices.createIndex({ rowId: 1 }, { name: 'idx_devices_rowId', sparse: true });

// schedules: Compound index to quickly fetch active schedules for a row
db.schedules.createIndex({ rowId: 1, isActive: 1 }, { name: 'idx_schedules_rowId_isActive' });

// control_logs: Index on rowId + timestamp for history queries
db.control_logs.createIndex({ rowId: 1, timestamp: -1 }, { name: 'idx_control_logs_rowId_timestamp' });

// alerts: Index on userId + timestamp for user-specific alert feed
db.alerts.createIndex({ userId: 1, timestamp: -1 }, { name: 'idx_alerts_userId_timestamp' });

// NOTE: data_records (Time-Series) — MongoDB automatically creates optimized
// compound indexes on (metaField, timeField). No manual index needed.

print('');
print('🎉 [init-db] Database initialization complete!');
print('   Database  : irrigation_db');
print('   TimeSeries: data_records');
print('   Standard  : ' + standardCollections.join(', '));
