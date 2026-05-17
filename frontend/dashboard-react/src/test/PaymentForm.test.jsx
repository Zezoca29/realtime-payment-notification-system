import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { PaymentForm } from '../components/PaymentForm'

// Mock the paymentApi module
vi.mock('../services/paymentApi', () => ({
  sendPayment: vi.fn(),
}))

import { sendPayment } from '../services/paymentApi'

describe('PaymentForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders all form fields', () => {
    render(<PaymentForm />)
    expect(screen.getByLabelText(/customer id/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/amount/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/currency/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/status/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /send/i })).toBeInTheDocument()
  })

  it('pre-populates defaults', () => {
    render(<PaymentForm />)
    expect(screen.getByLabelText(/customer id/i)).toHaveValue('customer-001')
    expect(screen.getByLabelText(/amount/i)).toHaveValue(150)
    expect(screen.getByLabelText(/currency/i)).toHaveValue('BRL')
    expect(screen.getByLabelText(/status/i)).toHaveValue('APPROVED')
  })

  it('shows success message after successful submission', async () => {
    sendPayment.mockResolvedValue({ paymentId: 'pay-abc-123' })

    render(<PaymentForm />)
    await userEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() =>
      expect(screen.getByText(/pay-abc-123 accepted/i)).toBeInTheDocument()
    )
  })

  it('shows error message when submission fails', async () => {
    sendPayment.mockRejectedValue(new Error('Service unavailable'))

    render(<PaymentForm />)
    await userEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() =>
      expect(screen.getByText(/service unavailable/i)).toBeInTheDocument()
    )
  })

  it('disables the submit button while loading', async () => {
    let resolve
    sendPayment.mockReturnValue(new Promise((r) => { resolve = r }))

    render(<PaymentForm />)
    await userEvent.click(screen.getByRole('button', { name: /send/i }))

    expect(screen.getByRole('button', { name: /sending/i })).toBeDisabled()
    resolve({ paymentId: 'xyz' })
  })

  it('calls onSent callback after success', async () => {
    sendPayment.mockResolvedValue({ paymentId: 'pay-xyz' })
    const onSent = vi.fn()

    render(<PaymentForm onSent={onSent} />)
    await userEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => expect(onSent).toHaveBeenCalledOnce())
  })

  it('sends correct payload to the API', async () => {
    sendPayment.mockResolvedValue({ paymentId: 'pay-123' })

    render(<PaymentForm />)
    await userEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => expect(sendPayment).toHaveBeenCalledWith({
      customerId: 'customer-001',
      amount: 150,
      currency: 'BRL',
      status: 'APPROVED',
    }))
  })
})
