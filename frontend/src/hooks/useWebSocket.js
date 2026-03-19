import { useEffect, useRef, useCallback, useState } from 'react';

/**
 * useWebSocket — Custom React Hook
 *
 * Manages a WebSocket connection lifecycle with:
 *  - Auto-reconnect on disconnect (Exponential Backoff, max 30s)
 *  - Cleanup on component unmount (prevents memory leaks)
 *  - Connection status tracking for UI feedback
 *
 * @param {string} url            - WebSocket server URL (e.g., "ws://localhost:8080/ws/telemetry")
 * @param {function} onMessage    - Callback invoked with the parsed JSON payload on each message
 * @param {object}  [options]     - Optional configuration overrides
 * @param {number}  [options.maxRetryDelayMs=30000]  - Maximum backoff ceiling in milliseconds
 * @param {number}  [options.initialDelayMs=1000]    - Base reconnect delay in milliseconds
 *
 * @returns {{ status: string, disconnect: function }}
 *   - status     : 'CONNECTING' | 'OPEN' | 'CLOSED' | 'RECONNECTING'
 *   - disconnect : Manually close the connection and stop all reconnect attempts
 */
const useWebSocket = (url, onMessage, options = {}) => {
    const {
        maxRetryDelayMs = 30_000,
        initialDelayMs = 1_000,
    } = options;

    const [status, setStatus] = useState('CLOSED');

    const wsRef = useRef(null);                // Holds the active WebSocket instance
    const retryCountRef = useRef(0);           // Tracks consecutive reconnect attempts
    const retryTimeoutRef = useRef(null);      // Holds the pending reconnect timer
    const isManuallyClosed = useRef(false);    // Flag: prevents reconnect on intentional close
    const onMessageRef = useRef(onMessage);    // Stable ref to avoid re-triggering the effect

    // Keep the onMessage callback ref up-to-date without triggering reconnects
    useEffect(() => {
        onMessageRef.current = onMessage;
    }, [onMessage]);

    const connect = useCallback(() => {
        if (!url) return;

        // Compute exponential backoff delay: 1s, 2s, 4s, 8s, 16s, 30s (capped)
        const delay = Math.min(
            initialDelayMs * Math.pow(2, retryCountRef.current),
            maxRetryDelayMs
        );

        const scheduleReconnect = () => {
            if (isManuallyClosed.current) return;

            retryCountRef.current += 1;
            const nextDelay = Math.min(
                initialDelayMs * Math.pow(2, retryCountRef.current),
                maxRetryDelayMs
            );

            console.warn(`[useWebSocket] Reconnecting in ${nextDelay}ms (attempt #${retryCountRef.current})...`);
            setStatus('RECONNECTING');

            retryTimeoutRef.current = setTimeout(() => {
                connect();
            }, nextDelay);
        };

        setStatus('CONNECTING');
        console.log(`[useWebSocket] Connecting to: ${url}`);

        const ws = new WebSocket(url);
        wsRef.current = ws;

        ws.onopen = () => {
            console.log('[useWebSocket] Connection established.');
            retryCountRef.current = 0; // Reset backoff counter on successful connection
            setStatus('OPEN');
        };

        ws.onmessage = (event) => {
            try {
                const payload = JSON.parse(event.data);
                onMessageRef.current?.(payload);
            } catch (error) {
                console.error('[useWebSocket] Failed to parse incoming message:', error, '| Raw data:', event.data);
            }
        };

        ws.onerror = (error) => {
            // onerror is always followed by onclose, so we only log here
            console.error('[useWebSocket] WebSocket error:', error);
        };

        ws.onclose = (event) => {
            console.log(`[useWebSocket] Connection closed. Code: ${event.code}, Reason: "${event.reason}"`);
            setStatus('CLOSED');

            if (!isManuallyClosed.current) {
                scheduleReconnect();
            }
        };
    }, [url, initialDelayMs, maxRetryDelayMs]);

    // Initiates connection and registers cleanup on unmount
    useEffect(() => {
        if (!url) return;

        isManuallyClosed.current = false;
        connect();

        return () => {
            // CLEANUP: Close connection and cancel any pending reconnect timers
            isManuallyClosed.current = true;
            clearTimeout(retryTimeoutRef.current);
            if (wsRef.current) {
                wsRef.current.close(1000, 'Component unmounted');
                wsRef.current = null;
            }
            console.log('[useWebSocket] Cleaned up connection on unmount.');
        };
    }, [connect, url]);

    /**
     * Manually disconnects and permanently stops all reconnect attempts.
     * Useful for logout flows or intentional disconnects.
     */
    const disconnect = useCallback(() => {
        isManuallyClosed.current = true;
        clearTimeout(retryTimeoutRef.current);
        if (wsRef.current) {
            wsRef.current.close(1000, 'Manual disconnect');
            wsRef.current = null;
        }
        setStatus('CLOSED');
    }, []);

    return { status, disconnect };
};

export default useWebSocket;
