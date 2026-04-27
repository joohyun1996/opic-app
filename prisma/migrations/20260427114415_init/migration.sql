-- CreateTable
CREATE TABLE "User" (
    "id" SERIAL NOT NULL,
    "username" TEXT NOT NULL,
    "password" TEXT NOT NULL,
    "role" TEXT NOT NULL DEFAULT 'member',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Word" (
    "id" SERIAL NOT NULL,
    "word" TEXT NOT NULL,
    "phonetic" TEXT,
    "meaningKo" TEXT NOT NULL,
    "meaningEn" TEXT,
    "example" TEXT,
    "exampleKo" TEXT,
    "level" INTEGER NOT NULL,
    "category" TEXT NOT NULL,
    "partOfSpeech" TEXT,
    "collocations" TEXT[],
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "Word_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "UserWord" (
    "userId" INTEGER NOT NULL,
    "wordId" INTEGER NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'new',
    "correctCount" INTEGER NOT NULL DEFAULT 0,
    "wrongCount" INTEGER NOT NULL DEFAULT 0,
    "nextReview" TIMESTAMP(3),
    "lastStudied" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "UserWord_pkey" PRIMARY KEY ("userId","wordId")
);

-- CreateTable
CREATE TABLE "GrammarChapter" (
    "id" TEXT NOT NULL,
    "category" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "order" INTEGER NOT NULL,
    "coreConceptKo" TEXT NOT NULL,
    "structureTable" JSONB NOT NULL,
    "goodExamples" JSONB NOT NULL,
    "badExamples" JSONB NOT NULL,
    "opicTip" TEXT,
    "quizBank" JSONB NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "GrammarChapter_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "GrammarProgress" (
    "userId" INTEGER NOT NULL,
    "chapterId" TEXT NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'not_started',
    "lastScore" INTEGER,
    "lastStudied" TIMESTAMP(3),
    "doneAt" TIMESTAMP(3),

    CONSTRAINT "GrammarProgress_pkey" PRIMARY KEY ("userId","chapterId")
);

-- CreateTable
CREATE TABLE "GrammarError" (
    "id" SERIAL NOT NULL,
    "userId" INTEGER NOT NULL,
    "source" TEXT NOT NULL,
    "original" TEXT NOT NULL,
    "corrected" TEXT NOT NULL,
    "errorType" TEXT NOT NULL,
    "errorDetail" TEXT,
    "chapterId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "GrammarError_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "SpeakingSession" (
    "id" SERIAL NOT NULL,
    "userId" INTEGER NOT NULL,
    "questionType" TEXT NOT NULL,
    "question" TEXT NOT NULL,
    "originalText" TEXT NOT NULL,
    "correctedText" TEXT NOT NULL,
    "feedback" JSONB NOT NULL,
    "opicLevel" TEXT,
    "pauseCount" INTEGER NOT NULL DEFAULT 0,
    "wordTimestamps" JSONB,
    "audioPath" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "SpeakingSession_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ShadowingVideo" (
    "id" SERIAL NOT NULL,
    "userId" INTEGER NOT NULL,
    "youtubeUrl" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "thumbnail" TEXT,
    "duration" INTEGER,
    "subtitles" JSONB NOT NULL,
    "isShared" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ShadowingVideo_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "User_username_key" ON "User"("username");

-- CreateIndex
CREATE UNIQUE INDEX "Word_word_key" ON "Word"("word");

-- CreateIndex
CREATE INDEX "Word_level_idx" ON "Word"("level");

-- CreateIndex
CREATE INDEX "Word_category_idx" ON "Word"("category");

-- CreateIndex
CREATE INDEX "Word_level_category_idx" ON "Word"("level", "category");

-- CreateIndex
CREATE INDEX "UserWord_userId_status_nextReview_idx" ON "UserWord"("userId", "status", "nextReview");

-- CreateIndex
CREATE INDEX "UserWord_userId_status_idx" ON "UserWord"("userId", "status");

-- CreateIndex
CREATE INDEX "GrammarProgress_userId_status_idx" ON "GrammarProgress"("userId", "status");

-- CreateIndex
CREATE INDEX "GrammarProgress_userId_lastStudied_idx" ON "GrammarProgress"("userId", "lastStudied");

-- CreateIndex
CREATE INDEX "GrammarError_userId_errorType_idx" ON "GrammarError"("userId", "errorType");

-- CreateIndex
CREATE INDEX "GrammarError_userId_createdAt_idx" ON "GrammarError"("userId", "createdAt");

-- CreateIndex
CREATE INDEX "SpeakingSession_userId_createdAt_idx" ON "SpeakingSession"("userId", "createdAt");

-- CreateIndex
CREATE INDEX "SpeakingSession_userId_opicLevel_idx" ON "SpeakingSession"("userId", "opicLevel");

-- AddForeignKey
ALTER TABLE "UserWord" ADD CONSTRAINT "UserWord_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "UserWord" ADD CONSTRAINT "UserWord_wordId_fkey" FOREIGN KEY ("wordId") REFERENCES "Word"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GrammarProgress" ADD CONSTRAINT "GrammarProgress_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GrammarProgress" ADD CONSTRAINT "GrammarProgress_chapterId_fkey" FOREIGN KEY ("chapterId") REFERENCES "GrammarChapter"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "GrammarError" ADD CONSTRAINT "GrammarError_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "SpeakingSession" ADD CONSTRAINT "SpeakingSession_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ShadowingVideo" ADD CONSTRAINT "ShadowingVideo_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
