interface DayCardProps {
  day: number
  total: number
  mastered: number
  learning: number
  wrong: number
  onClick: () => void
}

export default function DayCard({ day, total, mastered, learning, wrong, onClick }: DayCardProps) {
  const done = mastered + learning
  const percent = total > 0 ? Math.round((mastered / total) * 100) : 0
  const isComplete = mastered === total && total > 0

  return (
    <button
      onClick={onClick}
      className="flex flex-col gap-2 p-3 rounded-xl border text-left active:scale-95 transition-transform"
      style={{
        borderColor: isComplete ? '#16a34a' : '#e8e6e0',
        background: isComplete ? '#eaf3de' : '#fff', // CLAUDE.md 지정 습득 색상
      }}
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium" style={{ color: '#6b7280' }}>
          Day {day}
        </span>
        {wrong > 0 && (
          <span
            className="text-[10px] font-bold px-1.5 py-0.5 rounded-full"
            style={{ background: '#fcebeb', color: '#dc2626' }}
          >
            {wrong}
          </span>
        )}
      </div>

      <div className="text-sm font-semibold" style={{ color: '#1a1a18' }}>
        {mastered}
        <span className="font-normal text-xs" style={{ color: '#9ca3af' }}>
          /{total}
        </span>
      </div>

      <div className="w-full rounded-full overflow-hidden" style={{ height: 4, background: '#e8e6e0' }}>
        <div
          className="h-full rounded-full transition-all"
          style={{
            width: `${percent}%`,
            background: isComplete ? '#16a34a' : '#f59e0b',
          }}
        />
      </div>
    </button>
  )
}
