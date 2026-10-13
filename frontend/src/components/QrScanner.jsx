import { useEffect, useRef } from 'react';
import { Html5Qrcode } from 'html5-qrcode';

/**
 * Thin wrapper around html5-qrcode's camera scanner. Calls onScan(text)
 * with the raw decoded string the first time something is successfully
 * read, then stops the camera automatically.
 */
export default function QrScanner({ onScan, onError }) {
  const containerId = 'qr-scanner-region';
  const scannerRef = useRef(null);
  const stoppedRef = useRef(false);

  useEffect(() => {
    const scanner = new Html5Qrcode(containerId);
    scannerRef.current = scanner;
    stoppedRef.current = false;

    scanner
      .start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 220, height: 220 } },
        (decodedText) => {
          if (stoppedRef.current) return;
          stoppedRef.current = true;
          scanner.stop().catch(() => {});
          onScan(decodedText);
        },
        () => { /* per-frame decode failures are normal, ignore */ }
      )
      .catch((err) => {
        onError?.('Could not access camera: ' + err);
      });

    return () => {
      if (!stoppedRef.current) {
        scanner.stop().catch(() => {});
      }
    };
  }, [onScan, onError]);

  return <div id={containerId} className="qr-scanner-region" />;
}
