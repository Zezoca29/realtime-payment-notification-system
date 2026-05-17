import { describe, it, expect, vi, beforeEach } from 'vitest'
import { sendPayment } from '../services/paymentApi'

const mockFetch = vi.fn()
global.fetch = mockFetch

describe('paymentApi.sendPayment', () => {
  beforeEach(() => vi.clearAllMocks())

  it('POSTs to the correct URL with JSON content-type', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => ({ paymentId: 'pay-1', status: 'APPROVED' }),
    })

    await sendPayment({ customerId: 'c1', amount: 50, currency: 'USD', status: 'APPROVED' })

    expect(mockFetch).toHaveBeenCalledWith(
      'http://localhost:8081/api/v1/payments',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ customerId: 'c1', amount: 50, currency: 'USD', status: 'APPROVED' }),
      })
    )
  })

  it('returns parsed response on success', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => ({ paymentId: 'pay-abc', status: 'APPROVED', correlationId: 'cid-1' }),
    })

    const result = await sendPayment({ customerId: 'c1', amount: 10, currency: 'BRL', status: 'APPROVED' })
    expect(result.paymentId).toBe('pay-abc')
  })

  it('throws an error when the server returns non-2xx', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      json: async () => ({ message: 'Validation failed' }),
    })

    await expect(sendPayment({})).rejects.toThrow('Validation failed')
  })

  it('throws a generic HTTP error when response body has no message', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      json: async () => ({}),
      status: 500,
    })

    await expect(sendPayment({})).rejects.toThrow('HTTP 500')
  })
})
