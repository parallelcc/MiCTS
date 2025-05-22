# MiCTS



[![Звёзды](https://img.shields.io/github/stars/parallelcc/MiCTS?label=%D0%97%D0%B2%D1%91%D0%B7%D0%B4%D1%8B)](https://github.com/parallelcc/MiCTS) [![Скачивания](https://img.shields.io/github/downloads/parallelcc/MiCTS/total?label=%D0%A1%D0%BA%D0%B0%D1%87%D0%B8%D0%B2%D0%B0%D0%BD%D0%B8%D1%8F)](https://github.com/parallelcc/MiCTS/releases) [![Релиз](https://img.shields.io/github/v/release/parallelcc/MiCTS?label=%D0%A0%D0%B5%D0%BB%D0%B8%D0%B7)](https://github.com/parallelcc/MiCTS/releases/latest)  [![DeepWiki](https://img.shields.io/badge/DeepWiki-parallelcc%2FMiCTS-blue.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACwAAAAyCAYAAAAnWDnqAAAAAXNSR0IArs4c6QAAA05JREFUaEPtmUtyEzEQhtWTQyQLHNak2AB7ZnyXZMEjXMGeK/AIi+QuHrMnbChYY7MIh8g01fJoopFb0uhhEqqcbWTp06/uv1saEDv4O3n3dV60RfP947Mm9/SQc0ICFQgzfc4CYZoTPAswgSJCCUJUnAAoRHOAUOcATwbmVLWdGoH//PB8mnKqScAhsD0kYP3j/Yt5LPQe2KvcXmGvRHcDnpxfL2zOYJ1mFwrryWTz0advv1Ut4CJgf5uhDuDj5eUcAUoahrdY/56ebRWeraTjMt/00Sh3UDtjgHtQNHwcRGOC98BJEAEymycmYcWwOprTgcB6VZ5JK5TAJ+fXGLBm3FDAmn6oPPjR4rKCAoJCal2eAiQp2x0vxTPB3ALO2CRkwmDy5WohzBDwSEFKRwPbknEggCPB/imwrycgxX2NzoMCHhPkDwqYMr9tRcP5qNrMZHkVnOjRMWwLCcr8ohBVb1OMjxLwGCvjTikrsBOiA6fNyCrm8V1rP93iVPpwaE+gO0SsWmPiXB+jikdf6SizrT5qKasx5j8ABbHpFTx+vFXp9EnYQmLx02h1QTTrl6eDqxLnGjporxl3NL3agEvXdT0WmEost648sQOYAeJS9Q7bfUVoMGnjo4AZdUMQku50McDcMWcBPvr0SzbTAFDfvJqwLzgxwATnCgnp4wDl6Aa+Ax283gghmj+vj7feE2KBBRMW3FzOpLOADl0Isb5587h/U4gGvkt5v60Z1VLG8BhYjbzRwyQZemwAd6cCR5/XFWLYZRIMpX39AR0tjaGGiGzLVyhse5C9RKC6ai42ppWPKiBagOvaYk8lO7DajerabOZP46Lby5wKjw1HCRx7p9sVMOWGzb/vA1hwiWc6jm3MvQDTogQkiqIhJV0nBQBTU+3okKCFDy9WwferkHjtxib7t3xIUQtHxnIwtx4mpg26/HfwVNVDb4oI9RHmx5WGelRVlrtiw43zboCLaxv46AZeB3IlTkwouebTr1y2NjSpHz68WNFjHvupy3q8TFn3Hos2IAk4Ju5dCo8B3wP7VPr/FGaKiG+T+v+TQqIrOqMTL1VdWV1DdmcbO8KXBz6esmYWYKPwDL5b5FA1a0hwapHiom0r/cKaoqr+27/XcrS5UwSMbQAAAABJRU5ErkJggg==)](https://deepwiki.com/parallelcc/MiCTS)



[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;[English](/README_en.md)&nbsp;&nbsp;|&nbsp;&nbsp;Русский



Триггер для Circle to Search на любом устройстве Android 9–15



*Это приложение предназначено только для запуска Circle to Search и не может решать проблемы, которые могут возникнуть после запуска*



## Как использовать



1. Установите последнюю версию [Google](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox), включите автозапуск, выключите ограничения на работу в фоновом режиме, и установите Google ассистентом по умолчанию




2. Установите и запустите MiCTS

   - Если вам повезет, то Circle to Search будет запускаться без root при запуске MiCTS

   - Если ничего не произошло, скорее всего, Google отключил функцию Circle to Search для вашего устройства (вы можете убедиться в этом, проверив сообщение `Omni invocation failed: not enabled` в Logcat). Попробуйте выполнить следующие действия **с root**:

     - Активируйте модуль в LSPosed, включите `Подмену устройства для Google` в [настройках MiCTS](#как-открыть-настройки) и принудительно перезапустите Google

     - Если он все еще не работает, измените `com.google.android.apps.search.omnient.device` флаг `45631784` на true, используя [GMS-Flags](https://github.com/polodarb/GMS-Flags).





3. Настройка метода триггера

   - Триггер срабатывает при запуске MiCTS, поэтому вы можете использовать другие приложения, такие как Quick Ball, Xposed Edge, ShortX и т. д., установите запуск MiCTS в качестве действия, чтобы настроить метод срабатывания

   - MiCTS предоставляет кнопку в шторке в качестве триггера, поэтому вы можете добавить ее в шторку и запускать триггеры, нажимая на неё

   - Для устройств Xiaomi в MiCTS встроена поддержка `Триггера по зажатию полосы управления с помощью жестов` и `Триггера по зажатию кнопки домой`, которые можно включить в настройках MiCTS (необходимо активировать модуль и перезагрузить телефон после установки MiCTS)

   - Для устройств Samsung с Android 13 и выше можно загрузить и установить "Routines+" из [Galaxy Store](https://galaxystore.samsung.com/detail/com.samung.android.app.routineplus) или [Good Lock](https://galaxystore.samsung.com/detail/com.samsung.android.goodlock). Затем перейдите в раздел "Настройки" > "Режимы и рутина", чтобы создать конфигурации, запускающие MiCTS по действию, например долгому нажати кнопки питания

   



## Настройки



### Как открыть Настройки

- Зажмите значок приложения MiCTS, отобразится опция "Настройки"

- На странице Модули в LSPosed выберите MiCTS, затем нажмите на значок настроек

- Зажмите кнопку в шторке



### Настройки Приложения

- Задержка обычного триггера: Задержка при срабатывании при запуске MiCTS

- Задержка триггера кнопки в шторке: Задержка триггера при нажатии кнопки в шторке



### Настройки Модуля

Необходимо активировать модуль в LSPosed

- Сервис системных триггеров: Системная служба, используемая при срабатывании триггера. Будут показаны только поддерживаемые службы. Необходимо добавить Системный Framework в область действия в LSPosed

   - VIS: Поддерживается на Android 9-15. Необходимо установить Google как ассистент по умолчанию. Если модуль не активирован, будет использоваться только этот сервис

   - CSHelper: Поддерживает Android 14 QPR3 и выше. Не нужно устанавливать Google как ассистент по умолчанию

   - CSService: Поддерживает Android 15 и выше. Специальный сервис для Circle To Search, действие которого аналогично CSHelper





- Триггер по зажатию полосы управления с помощью жестов: Поддерживается только на устройствах Xiaomi. Необходимо добавить Системный Launcher/POCO Launcher в область действия в LSPosed





- Триггер по зажатию кнопки домой: Поддерживается только на устройствах Xiaomi. Необходимо добавить Системный Framework в область видимости в LSPosed





- Подмена устройства для Google: Необходимо добавить Google в область действия в LSPosed

   - Производитель: Модифицировать значение `ro.product.manufacturer` которое читает Google

   - Бренд: Модифицировать значение `ro.product.brand` которое читает Google

   - Модель: Модифицировать значение `ro.product.model` которое читает Google
     
   - Устройство: Модифицировать значение `ro.product.device` которое читает Google



## Частозадаваемые вопросы



### Пишет "Триггер не сработал!"



Скорее всего, Google не установлен как ассистент по умолчанию


### Вместо Circle to Search открывается Google ассистент



Убедитесь что Google обновлён до последней версии



### Иногда триггер не срабатывает успешно, и интерфейс появляется только после открытия Google



Скорее всего, это связано с механизмом закрытия фоновых процессов. Проверьте, есть ли на вашем устройстве соответствующие настройки и добавьте Google в белый список, например, выберите "Без ограничений" в экономии заряда батареи.



Эта проблема не должна возникать когда `Сервис системных триггеров` установлен в настройках модуля `CSHelper`



## История звёзд



<a href="https://star-history.com/#parallelcc/micts&Date">

 <picture>

   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date&theme=dark" />

   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />

   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />

 </picture>

</a>
