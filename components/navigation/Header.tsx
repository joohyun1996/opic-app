'use client'

import { useRouter } from 'next/navigation'
import Link from 'next/link'

interface HeaderProps {
  title: string
  back?: string | boolean
  action?: React.ReactNode
}

export default function Header({ title, back, action }: HeaderProps) {
  const router = useRouter()

  function handleBack() {
    if (typeof back === 'string') {
      router.push(back)
    } else {
      router.back()
    }
  }

  return (
    <header
      className="flex items-center justify-between px-4 border-b"
      style={{ height: 56, background: '#fff', borderColor: '#e8e6e0' }}
    >
      <div className="w-10 flex items-center">
        {back !== undefined && (
          <button
            onClick={handleBack}
            className="flex items-center justify-center w-9 h-9 -ml-1 rounded-full active:bg-gray-100"
            aria-label="뒤로가기"
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M12.5 15L7.5 10L12.5 5" stroke="#1a1a18" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
        )}
      </div>

      <span className="text-base font-semibold" style={{ color: '#1a1a18' }}>
        {title}
      </span>

      <div className="w-10 flex items-center justify-end">
        {action}
      </div>
    </header>
  )
}
