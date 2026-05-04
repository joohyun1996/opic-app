import bcrypt from 'bcryptjs'
import { PrismaClient } from '../app/generated/prisma'
import { PrismaPg } from '@prisma/adapter-pg'
import 'dotenv/config'

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL! })
const prisma = new PrismaClient({ adapter } as any)

async function main() {
  const hash = await bcrypt.hash('x2h6d3$5^', 12)
  await prisma.user.update({
    where: { username: 'hbk9093' },
    data: { password: hash }
  })
  console.log('✅ 비밀번호 변경 완료')
  await prisma.$disconnect()
}

main()
