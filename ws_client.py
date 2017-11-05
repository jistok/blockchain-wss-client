#!/usr/bin/env python

import thread
import time
import websocket
import sys, signal

WS_URL = 'ws://localhost:18080'


def on_message(ws, message):
    print(message)


def on_error(ws, error):
    print(error)


def on_close(ws):
    print("### closed ###")


def on_open(ws):
    print 'Opening connection ...'

    def run(*args):
        ws.send("Hi, I'm here")

    thread.start_new_thread(run, ())


def signal_handler(signal, frame):
    print('Shutting down ...')
    ws.close()
    sys.exit(0)


if __name__ == "__main__":
    signal.signal(signal.SIGINT, signal_handler)
    websocket.enableTrace(True)
    ws_url = WS_URL
    if len(sys.argv) > 1:
      ws_url = sys.argv[1]
      print 'Setting ws_url to %s' % ws_url
    ws = websocket.WebSocketApp(ws_url,
                                on_message=on_message,
                                on_error=on_error,
                                on_close=on_close)
    ws.on_open = on_open
    ws.run_forever()

