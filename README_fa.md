# MiCTS

[![Stars](https://img.shields.io/github/stars/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS) [![Downloads](https://img.shields.io/github/downloads/parallelcc/MiCTS/total)](https://github.com/parallelcc/MiCTS/releases) [![Release](https://img.shields.io/github/v/release/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS/releases/latest) [![DeepWiki](https://img.shields.io/badge/DeepWiki-parallelcc%2FMiCTS-blue.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACwAAAAyCAYAAAAnWDnqAAAAAXNSR0IArs4c6QAAA05JREFUaEPtmUtyEzEQhtWTQyQLHNak2AB7ZnyXZMEjXMGeK/AIi+QuHrMnbChYY7MIh8g01fJoopFb0uhhEqqcbWTp06/uv1saEDv4O3n3dV60RfP947Mm9/SQc0ICFQgzfc4CYZoTPAswgSJCCUJUnAAoRHOAUOcATwbmVLWdGoH//PB8mnKqScAhsD0kYP3j/Yt5LPQe2KvcXmGvRHcDnpxfL2zOYJ1mFwrryWTz0advv1Ut4CJgf5uhDuDj5eUcAUoahrdY/56ebRWeraTjMt/00Sh3UDtjgHtQNHwcRGOC98BJEAEymycmYcWwOprTgcB6VZ5JK5TAJ+fXGLBm3FDAmn6oPPjR4rKCAoJCal2eAiQp2x0vxTPB3ALO2CRkwmDy5WohzBDwSEFKRwPbknEggCPB/imwrycgxX2NzoMCHhPkDwqYMr9tRcP5qNrMZHkVnOjRMWwLCcr8ohBVb1OMjxLwGCvjTikrsBOiA6fNyCrm8V1rP93iVPpwaE+gO0SsWmPiXB+jikdf6SizrT5qKasx5j8ABbHpFTx+vFXp9EnYQmLx02h1QTTrl6eDqxLnGjporxl3NL3agEvXdT0WmEost648sQOYAeJS9Q7bfUVoMGnjo4AZdUMQku50McDcMWcBPvr0SzbTAFDfvJqwLzgxwATnCgnp4wDl6Aa+Ax283gghmj+vj7feE2KBBRMW3FzOpLOADl0Isb5587h/U4gGvkt5v60Z1VLG8BhYjbzRwyQZemwAd6cCR5/XFWLYZRIMpX39AR0tjaGGiGzLVyhse5C9RKC6ai42ppWPKiBagOvaYk8lO7DajerabOZP46Lby5wKjw1HCRx7p9sVMOWGzb/vA1hwiWc6jm3MvQDTogQkiqIhJV0nBQBTU+3okKCFDy9WwferkHjtxib7t3xIUQtHxnIwtx4mpg26/HfwVNVDb4oI9RHmx5WGelRVlrtiw43zboCLaxv46AZeB3IlTkwouebTr1y2NjSpHz68WNFjHvupy3q8TFn3Hos2IAk4Ju5dCo8B3wP7VPr/FGaKiG+T+v+TQqIrOqMTL1VdWV1DdmcbO8KXBz6esmYWYKPwDL5b5FA1a0hwapHiom0r/cKaoqr+27/XcrS5UwSMbQAAAABJRU5EBgg==)](https://deepwiki.com/parallelcc/MiCTS)

[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;[English](/README_en.md)&nbsp;&nbsp;|&nbsp;&nbsp;[Русский](/README_ru.md)&nbsp;&nbsp;|&nbsp;&nbsp;[Türkçe](/README_tr.md)&nbsp;&nbsp;|&nbsp;&nbsp;فارسی

قابلیت «جستجو با دایره» (Circle to Search) را در هر دستگاه اندروید ۹ تا ۱۵ فعال کنید.

*این برنامه فقط وظیفه فعال‌سازی «جستجو با دایره» را بر عهده دارد و نمی‌تواند مشکلاتی که ممکن است پس از فعال‌سازی موفق رخ دهد را برطرف کند.*

## نحوه استفاده

1.  آخرین نسخه اپلیکیشن [گوگل](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox) را نصب کنید، شروع خودکار را فعال کرده، محدودیت‌های پس‌زمینه را غیرفعال کنید و گوگل را به عنوان برنامه دستیار پیش‌فرض تنظیم نمایید.

2.  MiCTS را نصب و اجرا کنید.
    *   اگر خوش‌شانس باشید، «جستجو با دایره» بدون نیاز به روت و تنها با اجرای MiCTS فعال خواهد شد.
    *   اگر اتفاقی نیفتاد، به احتمال زیاد گوگل قابلیت «جستجو با دایره» را برای دستگاه شما غیرفعال کرده است (می‌توانید با بررسی پیام `Omni invocation failed: not enabled` در Logcat این موضوع را تأیید کنید). در این صورت، روش‌های زیر را **با دسترسی روت** امتحان کنید:
        *   ماژول را در LSPosed فعال کنید، گزینه `جعل هویت دستگاه برای گوگل` را در [تنظیمات MiCTS](#نحوه-ورود-به-تنظیمات) فعال کرده و اپلیکیشن گوگل را به اجبار متوقف (Force Stop) و دوباره راه‌اندازی کنید.
        *   اگر همچنان کار نکرد، با استفاده از [GMS-Flags](https://github.com/polodarb/GMS-Flags)، فلگ `45631784` مربوط به `com.google.android.apps.search.omnient.device` را به true تغییر دهید.

3.  روش فعال‌سازی را تنظیم کنید.
    *   اجرای MiCTS قابلیت را فعال می‌کند، بنابراین می‌توانید از برنامه‌های دیگر مانند Quick Ball، Xposed Edge، ShortX و غیره استفاده کرده و اجرای MiCTS را به عنوان عمل مورد نظر خود برای سفارشی‌سازی روش فعال‌سازی تنظیم کنید.
    *   MiCTS یک کاشی فعال‌سازی ارائه می‌دهد که می‌توانید آن را به پنل تنظیمات سریع اضافه کرده و با کلیک روی آن، قابلیت را فعال کنید.
    *   برای دستگاه‌های شیائومی، MiCTS از `فعال‌سازی با فشردن طولانی نوار اشاره` و `فعال‌سازی با فشردن طولانی دکمه خانه` پشتیبانی می‌کند که می‌توانید آن‌ها را در تنظیمات MiCTS فعال کنید (پس از نصب MiCTS، باید ماژول را فعال و گوشی را مجدداً راه‌اندازی کنید).
    *   برای دستگاه‌های سامسونگ با اندروید ۱۳ و بالاتر، می‌توانید "Routines+" را از [گلکسی استور](https://galaxystore.samsung.com/detail/com.samsung.android.app.routineplus) یا [Good Lock](https://galaxystore.samsung.com/detail/com.samsung.android.goodlock) دانلود و نصب کنید. سپس، به تنظیمات > حالت‌ها و روتین‌ها (Modes and Routines) بروید تا روتین‌هایی برای اجرای MiCTS از طریق عمل دکمه (Button action) مانند فشردن طولانی دکمه پاور ایجاد کنید.

## تنظیمات

### نحوه ورود به تنظیمات
*   آیکون برنامه MiCTS را به مدت طولانی فشار دهید تا گزینه تنظیمات نمایش داده شود، سپس برای ورود کلیک کنید.
*   از صفحه ماژول‌ها در LSPosed، روی MiCTS کلیک کرده و سپس روی آیکون تنظیمات کلیک کنید.
*   کاشی موجود در پنل تنظیمات سریع را به مدت طولانی فشار دهید.

### تنظیمات برنامه
*   **تأخیر پیش‌فرض فعال‌سازی:** تأخیر هنگام فعال‌سازی از طریق اجرای برنامه MiCTS.
*   **تأخیر فعال‌سازی از طریق کاشی:** تأخیر هنگام فعال‌سازی از طریق کاشی پنل تنظیمات سریع.

### تنظیمات ماژول
نیاز به فعال‌سازی ماژول در LSPosed دارد.
*   **سرویس فعال‌سازی سیستم:** سرویس سیستمی که برای فعال‌سازی استفاده می‌شود. فقط سرویس‌های پشتیبانی‌شده نمایش داده می‌شوند. باید System Framework را به محدوده (scope) در LSPosed اضافه کنید.
    *   **VIS:** در اندروید ۹ تا ۱۵ پشتیبانی می‌شود. باید گوگل به عنوان دستیار پیش‌فرض تنظیم شود و در برخی دستگاه‌ها هنگام فعال‌سازی، لبه‌های صفحه نمایش چشمک می‌زند. اگر ماژول فعال نباشد، فقط از این سرویس استفاده خواهد شد.
    *   **CSHelper:** در اندروید ۱۴ QPR3 و بالاتر پشتیبانی می‌شود. نیازی به تنظیم گوگل به عنوان دستیار پیش‌فرض نیست و لبه‌های صفحه نمایش هنگام فعال‌سازی چشمک نمی‌زند.
    *   **CSService:** در اندروید ۱۵ و بالاتر پشتیبانی می‌شود. یک سرویس اختصاصی برای «جستجو با دایره» با اثری مشابه CSHelper.

*   **فعال‌سازی با فشردن طولانی نوار اشاره:** فقط در دستگاه‌های شیائومی پشتیبانی می‌شود. باید System Launcher/POCO Launcher را به محدوده در LSPosed اضافه کنید.

*   **فعال‌سازی با فشردن طولانی دکمه خانه:** فقط در دستگاه‌های شیائومی پشتیبانی می‌شود. باید System Framework را به محدوده در LSPosed اضافه کنید.

*   **جعل هویت دستگاه برای گوگل:** باید گوگل را به محدوده در LSPosed اضافه کنید.
    *   **شرکت سازنده:** مقدار `ro.product.manufacturer` را که توسط گوگل خوانده می‌شود، تغییر می‌دهد.
    *   **برند:** مقدار `ro.product.brand` را که توسط گوگل خوانده می‌شود، تغییر می‌دهد.
    *   **مدل:** مقدار `ro.product.model` را که توسط گوگل خوانده می‌شود، تغییر می‌دهد.
    *   **دستگاه:** مقدار `ro.product.device` را که توسط گوگل خوانده می‌شود، تغییر می‌دهد.

## پرسش‌های متداول

### پیام «فعال‌سازی ناموفق بود!» نمایش داده می‌شود.

به احتمال زیاد گوگل به عنوان دستیار پیش‌فرض تنظیم نشده است، آن را بررسی کنید.

### به جای «جستجو با دایره»، دستیار گوگل ظاهر می‌شود.

اطمینان حاصل کنید که اپلیکیشن گوگل شما آخرین نسخه است.

### گاهی اوقات فعال‌سازی با موفقیت انجام نمی‌شود و رابط کاربری تنها پس از باز کردن گوگل ظاهر می‌شود.

این مشکل احتمالاً به دلیل مکانیسم "tombstone" (متوقف کردن فرآیندهای پس‌زمینه) است. بررسی کنید که آیا دستگاه شما تنظیمات مرتبطی دارد و گوگل را به لیست سفید اضافه کنید، مانند انتخاب «بدون محدودیت» در بخش بهینه‌سازی باتری.

این مشکل در صورتی که `سرویس فعال‌سازی سیستم` در تنظیمات ماژول روی `CSHelper` تنظیم شده باشد، نباید رخ دهد.

## مشارکت در ترجمه

شما می‌توانید از طریق [Crowdin](https://crowdin.com/project/micts) در ترجمه مشارکت کنید.

اگر می‌خواهید زبان جدیدی اضافه کنید، لطفاً ابتدا یک issue ثبت کنید.

## تاریخچه ستاره‌ها

<a href="https://star-history.com/#parallelcc/micts&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />
 </picture>
</a>