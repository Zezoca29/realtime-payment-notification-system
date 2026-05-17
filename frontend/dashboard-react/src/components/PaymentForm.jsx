import { useState } from 'react'
import { sendPayment } from '../services/paymentApi'

const STATUSES = ['APPROVED', 'DECLINED', 'PENDING', 'FAILED', 'REFUNDED']
const CURRENCIES = ['BRL', 'USD', 'EUR']

export function PaymentForm({ onSent }) {
  const [form, setForm] = useState({
    customerId: 'customer-001',
    amount: '150.00',
    currency: 'BRL',
    status: 'APPROVED',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)

  const handleChange = (e) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setSuccess(null)
    try {
      const result = await sendPayment({
        ...form,
        amount: parseFloat(form.amount),
      })
      setSuccess(`Payment ${result.paymentId} accepted`)
      onSent?.()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="bg-gray-900 rounded-lg border border-gray-800 p-5 space-y-4">
      <h3 className="text-sm font-semibold text-gray-300 uppercase tracking-wider">
        Send Payment Event
      </h3>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-xs text-gray-400 mb-1">Customer ID</label>
          <input
            name="customerId"
            value={form.customerId}
            onChange={handleChange}
            className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm text-gray-100 focus:outline-none focus:border-blue-500"
            required
          />
        </div>
        <div>
          <label className="block text-xs text-gray-400 mb-1">Amount</label>
          <input
            name="amount"
            type="number"
            step="0.01"
            min="0.01"
            value={form.amount}
            onChange={handleChange}
            className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm text-gray-100 focus:outline-none focus:border-blue-500"
            required
          />
        </div>
        <div>
          <label className="block text-xs text-gray-400 mb-1">Currency</label>
          <select
            name="currency"
            value={form.currency}
            onChange={handleChange}
            className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm text-gray-100 focus:outline-none focus:border-blue-500"
          >
            {CURRENCIES.map((c) => <option key={c}>{c}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-400 mb-1">Status</label>
          <select
            name="status"
            value={form.status}
            onChange={handleChange}
            className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-sm text-gray-100 focus:outline-none focus:border-blue-500"
          >
            {STATUSES.map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
      </div>

      {error && (
        <p className="text-xs text-red-400 bg-red-900/30 border border-red-800 rounded px-3 py-2">
          {error}
        </p>
      )}
      {success && (
        <p className="text-xs text-green-400 bg-green-900/30 border border-green-800 rounded px-3 py-2">
          {success}
        </p>
      )}

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-gray-700 disabled:text-gray-500 text-white font-semibold py-2 rounded text-sm transition-colors"
      >
        {loading ? 'Sending...' : 'Send Payment →'}
      </button>
    </form>
  )
}
