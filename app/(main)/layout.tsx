export default function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex justify-center" style={{ background: '#f8f7f4' }}>
      <div className="w-full max-w-[430px] min-h-screen bg-white relative">
        {children}
      </div>
    </div>
  )
}
