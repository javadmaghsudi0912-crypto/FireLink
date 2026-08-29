# راه‌اندازی FireLink — فارسی

## 1) Firebase
1. یک پروژه در Firebase Console بسازید.
2. Authentication > Sign-in method > Email/Password را فعال کنید.
3. Realtime Database را بسازید.
4. در Project settings یک Android app با package زیر اضافه کنید:
   `com.firelink.app`
5. مقادیر API Key، App ID، Project ID و Database URL را بردارید.
6. فایل `local.properties.example` را به `local.properties` کپی و مقادیر را وارد کنید.
7. محتوای `firebase/database.rules.json` را در Realtime Database Rules قرار دهید و Publish کنید.

## 2) ساخت کاربران
در Firebase Authentication برای هر همکار یک حساب Email/Password بسازید.

## 3) عضویت گروه
بعد از ساخت کاربر، UID او را از Authentication بردارید و در Realtime Database این ساختار را بسازید:

teams
  station01
    members
      USER_UID_1: true
      USER_UID_2: true

`station01` همان شناسه گروهی است که داخل برنامه وارد می‌کنید.

## 4) ساخت APK
پروژه را با Android Studio باز کنید و Gradle Sync بزنید.
برای تست:
Build > Build APK(s)

برای نسخه نهایی:
Build > Generate Signed Bundle / APK > APK

حتماً یک keystore مخصوص این برنامه بسازید و چند نسخه پشتیبان امن از آن نگه دارید.
برای تمام آپدیت‌های آینده باید APK با همان کلید امضا شود.

## 5) نصب بدون Google Play
فایل APK امضاشده را با Quick Share، بلوتوث، تلگرام، واتساپ، کابل یا فلش OTG منتقل کنید.
روی گوشی گیرنده فایل APK را باز کنید.
در Android 8 به بعد باید برای همان برنامه‌ای که APK را باز می‌کند، گزینه
"Install unknown apps / Allow from this source"
را موقتاً فعال کنید.

## 6) روند استفاده پیشنهادی
- ابتدای شیفت: ورود + شناسه گروه + نام واحد + «شروع شیفت»
- هنگام حادثه: GPS روشن + «ارسال موقعیت فعلی حادثه»
- گیرندگان: اعلان جدید > «نقشه» > «دریافت شد»
- در ضعف اینترنت: «SMS پشتیبان» و ارسال پیام از اپ SMS گوشی

## 7) نکات عملیاتی
- قبل از هر شیفت، اینترنت، GPS، اعلان‌ها و ورود حساب تست شود.
- Battery Optimization برای FireLink محدود نشود، مخصوصاً روی گوشی‌های Xiaomi/Samsung/Huawei.
- اگر دقت GPS بیش از حدود 50 متر بود، قبل از حرکت مقصد را با آدرس اعلامی تطبیق دهید.
- FireLink جایگزین سامانه رسمی دیسپچ یا فرماندهی نیست؛ تا زمان آزمون میدانی باید ابزار کمکی باشد.
