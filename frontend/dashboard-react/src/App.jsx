import { usePaymentStream } from './hooks/usePaymentStream'
import { EventTable } from './components/EventTable'
import { StatsChart } from './components/StatsChart'
import { PaymentForm } from './components/PaymentForm'

const STATUS_COUNTS = (events) =>
  events.reduce(
    (acc, e) => {
      if (e.status === 'APPROVED') acc.approved++
      else if (e.status === 'DECLINED') acc.declined++
      else acc.other++
      return acc
    },
    { approved: 0, declined: 0, other: 0 }
  )

function ConnectionIndicator({ connected }) {
  return (
    <div className="flex items-center gap-2">
      <span
        className={`w-2 h-2 rounded-full ${
          connected ? 'bg-green-400 animate-pulse' : 'bg-red-500'
        }`}
      />
      <span className={`text-xs ${connected ? 'text-green-400' : 'text-red-400'}`}>
        {connected ? 'Live' : 'Disconnected'}
      </span>
    </div>
  )
}

export default function App() {
  const { events, connected, clearEvents } = usePaymentStream()
  const { approved, declined, other } = STATUS_COUNTS(events)

  return (
    <div className="min-h-screen p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-bold text-white">
            Payment Notification Dashboard
          </h1>
          <p className="text-xs text-gray-500 mt-0.5">
            Real-time event stream via Kafka → WebSocket
          </p>
        </div>
        <ConnectionIndicator connected={connected} />
      </div>

      {/* Stats */}
      <div className="grid grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Total Events', value: events.length, color: 'text-blue-400' },
          { label: 'Approved', value: approved, color: 'text-green-400' },
          { label: 'Declined', value: declined, color: 'text-red-400' },
          { label: 'Other', value: other, color: 'text-yellow-400' },
        ].map(({ label, value, color }) => (
          <div key={label} className="bg-gray-900 border border-gray-800 rounded-lg p-4">
            <p className="text-xs text-gray-500">{label}</p>
            <p className={`text-2xl font-bold mt-1 ${color}`}>{value}</p>
          </div>
        ))}
      </div>

      {/* Main grid */}
      <div className="grid grid-cols-3 gap-6">
        {/* Left: form + chart */}
        <div className="col-span-1 space-y-4">
          <PaymentForm />
          <StatsChart events={events} />
        </div>

        {/* Right: event feed */}
        <div className="col-span-2">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-gray-300">
              Live Event Feed
            </h2>
            {events.length > 0 && (
              <button
                onClick={clearEvents}
                className="text-xs text-gray-500 hover:text-gray-300 transition-colors"
              >
                Clear
              </button>
            )}
          </div>
          <EventTable events={events} />
        </div>
      </div>
    </div>
  )
}
