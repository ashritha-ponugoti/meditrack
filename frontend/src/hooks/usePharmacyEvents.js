import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Subscribes to /topic/pharmacy/{pharmacyId} and calls onEvent whenever
 * the backend signals something changed (a dispense happened, a transfer
 * was suggested/updated). The caller decides what to do - typically
 * re-fetch its own data - this hook only handles the connection.
 */
export default function usePharmacyEvents(pharmacyId, onEvent) {
  const clientRef = useRef(null);

  useEffect(() => {
    if (!pharmacyId) return;

    const stompClient = new Client({
      webSocketFactory: () => new SockJS(`${baseURL}/ws`),
      reconnectDelay: 4000,
      onConnect: () => {
        stompClient.subscribe(`/topic/pharmacy/${pharmacyId}`, (message) => {
          try {
            const payload = JSON.parse(message.body);
            onEvent?.(payload);
          } catch {
            onEvent?.({});
          }
        });
      },
    });

    stompClient.activate();
    clientRef.current = stompClient;

    return () => {
      stompClient.deactivate();
    };
  }, [pharmacyId, onEvent]);
}
