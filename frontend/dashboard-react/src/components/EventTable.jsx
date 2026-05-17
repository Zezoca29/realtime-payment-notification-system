import { StatusBadge } from './StatusBadge'

export function EventTable({ events }) {
  if (events.length === 0) {
    return (
      <div className="text-center text-gray-500 py-16">
        Waiting for payment events...
      </div>
    )
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-800">
      <table className="w-full text-sm">
        <thead className="bg-gray-900 text-gray-400 text-xs uppercase">
          <tr>
            <th className="px-4 py-3 text-left">Time</th>
            <th className="px-4 py-3 text-left">Payment ID</th>
            <th className="px-4 py-3 text-left">Customer</th>
            <th className="px-4 py-3 text-right">Amount</th>
            <th className="px-4 py-3 text-center">Status</th>
            <th className="px-4 py-3 text-left">Event ID</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-800">
          {events.map((e, idx) => (
            <tr
              key={e.eventId ?? idx}
              className={`transition-all duration-300 hover:bg-gray-800 ${
                idx === 0 ? 'animate-pulse-once bg-gray-850' : ''
              }`}
            >
              <td className="px-4 py-3 text-gray-400 whitespace-nowrap">
                {e.timestamp
                  ? new Date(e.timestamp).toLocaleTimeString()
                  : '—'}
              </td>
              <td className="px-4 py-3 font-mono text-xs text-blue-400 truncate max-w-[120px]">
                {e.paymentId}
              </td>
              <td className="px-4 py-3 text-gray-300">{e.customerId}</td>
              <td className="px-4 py-3 text-right font-semibold">
                {e.amount?.toLocaleString('pt-BR', {
                  style: 'currency',
                  currency: e.currency ?? 'BRL',
                })}
              </td>
              <td className="px-4 py-3 text-center">
                <StatusBadge status={e.status} />
              </td>
              <td className="px-4 py-3 font-mono text-xs text-gray-500 truncate max-w-[120px]">
                {e.eventId}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
