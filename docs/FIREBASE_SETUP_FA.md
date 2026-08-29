# راه‌اندازی Firebase برای FireLink

FireLink از Firebase Authentication و Realtime Database استفاده می‌کند.

## مشخصات Android

- Package name: `com.firelink.app`
- Authentication provider: Email/Password
- Database: Firebase Realtime Database

## GitHub Actions Secrets

در Repository Settings > Secrets and variables > Actions این چهار Secret را بسازید:

- `FIREBASE_API_KEY`
- `FIREBASE_APP_ID`
- `FIREBASE_DATABASE_URL`
- `FIREBASE_PROJECT_ID`

Workflow این مقادیر را فقط هنگام Build به `local.properties` می‌نویسد. فایل `local.properties` در Git ذخیره نمی‌شود.

## Database Rules

قواعد پیشنهادی در `firebase/database.rules.json` قرار دارد. پس از ساخت Realtime Database، محتوای این فایل باید در Rules دیتابیس Deploy شود.

## ساختار عضویت

برای هر تیم، UID اعضای مجاز باید به شکل زیر در Realtime Database ثبت شود:

```json
{
  "teams": {
    "TEAM-01": {
      "members": {
        "FIREBASE_USER_UID": true
      }
    }
  }
}
```

کاربرانی که UID آنها زیر `members` نباشد، طبق Rules اجازه خواندن یا ارسال موقعیت آن تیم را ندارند.
