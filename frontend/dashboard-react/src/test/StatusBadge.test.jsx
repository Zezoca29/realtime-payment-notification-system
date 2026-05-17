import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatusBadge } from '../../components/StatusBadge'

describe('StatusBadge', () => {
  it('renders the status label', () => {
    render(<StatusBadge status="APPROVED" />)
    expect(screen.getByText('APPROVED')).toBeInTheDocument()
  })

  it.each([
    ['APPROVED', 'bg-green-900'],
    ['DECLINED', 'bg-red-900'],
    ['PENDING',  'bg-yellow-900'],
    ['FAILED',   'bg-orange-900'],
    ['REFUNDED', 'bg-purple-900'],
  ])('applies correct colour class for %s', (status, expectedClass) => {
    render(<StatusBadge status={status} />)
    expect(screen.getByText(status)).toHaveClass(expectedClass)
  })

  it('falls back to gray for unknown status', () => {
    render(<StatusBadge status="UNKNOWN" />)
    const badge = screen.getByText('UNKNOWN')
    expect(badge).toHaveClass('bg-gray-800')
  })

  it('renders with bold font', () => {
    render(<StatusBadge status="PENDING" />)
    expect(screen.getByText('PENDING')).toHaveClass('font-bold')
  })
})
