import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { usePaymentStream } from '../../hooks/usePaymentStream'

// ── Mock @stomp/stompjs and sockjs-client ─────────────────────────────────────
// We capture the callbacks provided to the Client so tests can trigger them
// without a real WebSocket server.

let capturedOnConnect    = null
let capturedOnDisconnect = null
let capturedSubscription = null

const mockClient = {
  activate:   vi.fn(),
  deactivate: vi.fn(),
  subscribe:  vi.fn((topic, cb) => {
    capturedSubscription = cb
    return { unsubscribe: vi.fn() }
  }),
}

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn((config) => {
    capturedOnConnect    = config.onConnect
    capturedOnDisconnect = config.onDisconnect
    return mockClient
  }),
}))

vi.mock('sockjs-client', () => ({ default: vi.fn() }))

describe('usePaymentStream', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    capturedOnConnect    = null
    capturedOnDisconnect = null
    capturedSubscription = null
  })

  it('starts in disconnected state with empty events', () => {
    const { result } = renderHook(() => usePaymentStream())
    expect(result.current.connected).toBe(false)
    expect(result.current.events).toEqual([])
  })

  it('activates the STOMP client on mount', () => {
    renderHook(() => usePaymentStream())
    expect(mockClient.activate).toHaveBeenCalledOnce()
  })

  it('sets connected=true when onConnect fires', () => {
    const { result } = renderHook(() => usePaymentStream())
    act(() => capturedOnConnect())
    expect(result.current.connected).toBe(true)
  })

  it('sets connected=false when onDisconnect fires', () => {
    const { result } = renderHook(() => usePaymentStream())
    act(() => capturedOnConnect())
    act(() => capturedOnDisconnect())
    expect(result.current.connected).toBe(false)
  })

  it('prepends a received event to the events array', () => {
    const { result } = renderHook(() => usePaymentStream())
    act(() => capturedOnConnect())

    const event = { eventId: 'evt-1', status: 'APPROVED', amount: 100, currency: 'BRL' }
    act(() => capturedSubscription({ body: JSON.stringify(event) }))

    expect(result.current.events).toHaveLength(1)
    expect(result.current.events[0]).toMatchObject({ eventId: 'evt-1' })
  })

  it('caps event list at 100 entries', () => {
    const { result } = renderHook(() => usePaymentStream())
    act(() => capturedOnConnect())

    act(() => {
      for (let i = 0; i < 110; i++) {
        capturedSubscription({ body: JSON.stringify({ eventId: `evt-${i}` }) })
      }
    })

    expect(result.current.events).toHaveLength(100)
    // Newest event is first
    expect(result.current.events[0]).toMatchObject({ eventId: 'evt-109' })
  })

  it('clearEvents empties the events array', () => {
    const { result } = renderHook(() => usePaymentStream())
    act(() => capturedOnConnect())
    act(() => capturedSubscription({ body: JSON.stringify({ eventId: 'e1' }) }))

    act(() => result.current.clearEvents())

    expect(result.current.events).toEqual([])
  })

  it('deactivates the client on unmount', () => {
    const { unmount } = renderHook(() => usePaymentStream())
    unmount()
    expect(mockClient.deactivate).toHaveBeenCalledOnce()
  })

  it('silently ignores malformed JSON messages', () => {
    const { result } = renderHook(() => usePaymentStream())
    act(() => capturedOnConnect())

    // Should not throw
    act(() => capturedSubscription({ body: 'not-json' }))

    expect(result.current.events).toHaveLength(0)
  })
})
