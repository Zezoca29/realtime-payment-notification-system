import { useEffect, useRef, useState, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const WS_URL = 'http://localhost:8083/ws'
const TOPIC = '/topic/payments'
const MAX_EVENTS = 100

export function usePaymentStream() {
  const [events, setEvents] = useState([])
  const [connected, setConnected] = useState(false)
  const clientRef = useRef(null)

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)
        client.subscribe(TOPIC, (message) => {
          try {
            const event = JSON.parse(message.body)
            setEvents((prev) => [event, ...prev].slice(0, MAX_EVENTS))
          } catch {
            console.error('Failed to parse event:', message.body)
          }
        })
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
        setConnected(false)
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
    }
  }, [])

  const clearEvents = useCallback(() => setEvents([]), [])

  return { events, connected, clearEvents }
}
