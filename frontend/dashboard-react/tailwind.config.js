/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        approved: '#22c55e',
        declined: '#ef4444',
        pending: '#f59e0b',
        failed: '#f97316',
        refunded: '#8b5cf6',
      },
    },
  },
  plugins: [],
}
