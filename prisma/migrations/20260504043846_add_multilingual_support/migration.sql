/*
  Warnings:

  - You are about to drop the column `opicTip` on the `GrammarChapter` table. All the data in the column will be lost.
  - You are about to drop the column `opicLevel` on the `SpeakingSession` table. All the data in the column will be lost.
  - A unique constraint covering the columns `[language,word]` on the table `Word` will be added. If there are existing duplicate values, this will fail.
  - Added the required column `language` to the `GrammarChapter` table without a default value. This is not possible if the table is not empty.
  - Added the required column `language` to the `GrammarError` table without a default value. This is not possible if the table is not empty.
  - Added the required column `language` to the `ShadowingVideo` table without a default value. This is not possible if the table is not empty.
  - Added the required column `language` to the `SpeakingSession` table without a default value. This is not possible if the table is not empty.
  - Added the required column `language` to the `Word` table without a default value. This is not possible if the table is not empty.

*/
-- DropIndex
DROP INDEX "GrammarError_userId_createdAt_idx";

-- DropIndex
DROP INDEX "GrammarError_userId_errorType_idx";

-- DropIndex
DROP INDEX "SpeakingSession_userId_createdAt_idx";

-- DropIndex
DROP INDEX "SpeakingSession_userId_opicLevel_idx";

-- DropIndex
DROP INDEX "Word_category_idx";

-- DropIndex
DROP INDEX "Word_level_category_idx";

-- DropIndex
DROP INDEX "Word_level_idx";

-- DropIndex
DROP INDEX "Word_word_key";

-- AlterTable
ALTER TABLE "GrammarChapter" DROP COLUMN "opicTip",
ADD COLUMN     "examTip" TEXT,
ADD COLUMN     "language" TEXT NOT NULL;

-- AlterTable
ALTER TABLE "GrammarError" ADD COLUMN     "language" TEXT NOT NULL;

-- AlterTable
ALTER TABLE "ShadowingVideo" ADD COLUMN     "language" TEXT NOT NULL;

-- AlterTable
ALTER TABLE "SpeakingSession" DROP COLUMN "opicLevel",
ADD COLUMN     "examLevel" TEXT,
ADD COLUMN     "language" TEXT NOT NULL;

-- AlterTable
ALTER TABLE "Word" ADD COLUMN     "language" TEXT NOT NULL;

-- CreateIndex
CREATE INDEX "GrammarChapter_language_category_idx" ON "GrammarChapter"("language", "category");

-- CreateIndex
CREATE INDEX "GrammarChapter_language_order_idx" ON "GrammarChapter"("language", "order");

-- CreateIndex
CREATE INDEX "GrammarError_userId_language_errorType_idx" ON "GrammarError"("userId", "language", "errorType");

-- CreateIndex
CREATE INDEX "GrammarError_userId_language_createdAt_idx" ON "GrammarError"("userId", "language", "createdAt");

-- CreateIndex
CREATE INDEX "ShadowingVideo_language_idx" ON "ShadowingVideo"("language");

-- CreateIndex
CREATE INDEX "ShadowingVideo_userId_language_idx" ON "ShadowingVideo"("userId", "language");

-- CreateIndex
CREATE INDEX "SpeakingSession_userId_language_createdAt_idx" ON "SpeakingSession"("userId", "language", "createdAt");

-- CreateIndex
CREATE INDEX "SpeakingSession_userId_language_examLevel_idx" ON "SpeakingSession"("userId", "language", "examLevel");

-- CreateIndex
CREATE INDEX "Word_language_level_idx" ON "Word"("language", "level");

-- CreateIndex
CREATE INDEX "Word_language_category_idx" ON "Word"("language", "category");

-- CreateIndex
CREATE INDEX "Word_language_level_category_idx" ON "Word"("language", "level", "category");

-- CreateIndex
CREATE UNIQUE INDEX "Word_language_word_key" ON "Word"("language", "word");
