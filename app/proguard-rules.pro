# TASK 40: 릴리스 R8 keep 규칙. 라이브러리(Room·WorkManager·kotlinx.serialization·Compose)는 자체 consumer 규칙을 포함한다.

# whisper.cpp JNI: C 함수 이름이 Java_com_jooh_opic_core_stt_WhisperNative_* 라서 클래스·메서드 이름이 바뀌면 안 된다
-keep class com.jooh.opic.core.stt.WhisperNative { native <methods>; *; }
-keepclasseswithmembernames class * { native <methods>; }

# MediaPipe GenAI (Gemma): 내부에서 JNI·리플렉션으로 자기 클래스를 찾는다
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# kotlinx.serialization: @Serializable 데이터 클래스 (JSON 콘텐츠·백업 파일)
-keepattributes *Annotation*, InnerClasses, Signature
-keep,includedescriptorclasses class com.jooh.opic.**$$serializer { *; }
-keepclassmembers class com.jooh.opic.** { *** Companion; }
-keepclasseswithmembers class com.jooh.opic.** { kotlinx.serialization.KSerializer serializer(...); }

# WorkManager가 이름으로 만드는 Worker
-keep class com.jooh.opic.ReminderWorker { <init>(...); }

# 보안 라이브러리(Tink, EncryptedSharedPreferences)가 참조하는 선택적 의존성
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
