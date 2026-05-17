const STATUS_STYLES = {
  APPROVED: 'bg-green-900 text-green-300 border border-green-700',
  DECLINED: 'bg-red-900 text-red-300 border border-red-700',
  PENDING:  'bg-yellow-900 text-yellow-300 border border-yellow-700',
  FAILED:   'bg-orange-900 text-orange-300 border border-orange-700',
  REFUNDED: 'bg-purple-900 text-purple-300 border border-purple-700',
}

export function StatusBadge({ status }) {
  const cls = STATUS_STYLES[status] ?? 'bg-gray-800 text-gray-300'
  return (
    <span className={`px-2 py-0.5 rounded text-xs font-bold ${cls}`}>
      {status}
    </span>
  )
}
