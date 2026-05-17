import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
} from 'recharts'

const COLORS = {
  APPROVED: '#22c55e',
  DECLINED: '#ef4444',
  PENDING:  '#f59e0b',
  FAILED:   '#f97316',
  REFUNDED: '#8b5cf6',
}

export function StatsChart({ events }) {
  const counts = events.reduce((acc, e) => {
    acc[e.status] = (acc[e.status] ?? 0) + 1
    return acc
  }, {})

  const data = Object.entries(counts).map(([status, count]) => ({ status, count }))

  if (data.length === 0) return null

  return (
    <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
      <h3 className="text-sm text-gray-400 mb-3">Events by Status</h3>
      <ResponsiveContainer width="100%" height={140}>
        <BarChart data={data} margin={{ top: 0, right: 8, left: -20, bottom: 0 }}>
          <XAxis dataKey="status" tick={{ fill: '#9ca3af', fontSize: 11 }} />
          <YAxis tick={{ fill: '#9ca3af', fontSize: 11 }} allowDecimals={false} />
          <Tooltip
            contentStyle={{ backgroundColor: '#111827', border: '1px solid #374151', borderRadius: '6px' }}
            labelStyle={{ color: '#e5e7eb' }}
            itemStyle={{ color: '#9ca3af' }}
          />
          <Bar dataKey="count" radius={[4, 4, 0, 0]}>
            {data.map((entry) => (
              <Cell key={entry.status} fill={COLORS[entry.status] ?? '#6b7280'} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
