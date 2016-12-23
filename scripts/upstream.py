from __future__ import print_function

import threading
import socket
import struct
import time
from datetime import datetime
import random

TICKERS = ["AAPL", "GOOG", "MSFT", "SPY"]
PORT = 5555

def timestamp_millis(timestamp):
    return int((timestamp - datetime.utcfromtimestamp(0)).total_seconds() * 1000.0)

def send_trade(client_socket, host, port):
    timestamp = datetime.utcnow()
    ticker = random.choice(TICKERS)
    price = 90 + random.randrange(0, 400) * 0.05
    size = random.randrange(100, 10000, 100)
    msg = struct.pack("!QH%dsdI" % len(ticker), timestamp_millis(timestamp), len(ticker), ticker.encode("ascii"), price, size)
    msg_len = struct.pack("!H", len(msg))
    print("[%s:%d] %s: %s %.2f %d" % (host, port, timestamp, ticker, price, size))
    client_socket.send(msg_len + msg)

def emit_trades(client_socket, addr):
    (host, port) = addr
    try:
        while True:
            time.sleep(random.uniform(0.2, 3))
            send_trade(client_socket, host, port)
    except Exception as e:
        print("[%s:%d] Error: %s" % (host, port, e))
    finally:
        client_socket.close()

def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("127.0.0.1", PORT))
    server.listen(5)
    print("Waiting for connections on port %d" % PORT)
    while True:
        (client, addr) = server.accept()
        print("Incoming connection from %s:%d" % addr)
        client_thread = threading.Thread(target=emit_trades, args=(client, addr))
        client_thread.daemon = True
        client_thread.start()

if __name__ == "__main__":
    main()