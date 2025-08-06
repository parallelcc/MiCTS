# MiCTS

[![Stars](https://img.shields.io/github/stars/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS) [![Downloads](https://img.shields.io/github/downloads/parallelcc/MiCTS/total)](https://github.com/parallelcc/MiCTS/releases) [![Release](https://img.shields.io/github/v/release/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS/releases/latest)  [![DeepWiki](https://img.shields.io/badge/DeepWiki-parallelcc%2FMiCTS-blue.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACwAAAAyCAYAAAAnWDnqAAAAAXNSR0IArs4c6QAAA05JREFUaEPtmUtyEzEQhtWTQyQLHNak2AB7ZnyXZMEjXMGeK/AIi+QuHrMnbChYY7MIh8g01fJoopFb0uhhEqqcbWTp06/uv1saEDv4O3n3dV60RfP947Mm9/SQc0ICFQgzfc4CYZoTPAswgSJCCUJUnAAoRHOAUOcATwbmVLWdGoH//PB8mnKqScAhsD0kYP3j/Yt5LPQe2KvcXmGvRHcDnpxfL2zOYJ1mFwrryWTz0advv1Ut4CJgf5uhDuDj5eUcAUoahrdY/56ebRWeraTjMt/00Sh3UDtjgHtQNHwcRGOC98BJEAEymycmYcWwOprTgcB6VZ5JK5TAJ+fXGLBm3FDAmn6oPPjR4rKCAoJCal2eAiQp2x0vxTPB3ALO2CRkwmDy5WohzBDwSEFKRwPbknEggCPB/imwrycgxX2NzoMCHhPkDwqYMr9tRcP5qNrMZHkVnOjRMWwLCcr8ohBVb1OMjxLwGCvjTikrsBOiA6fNyCrm8V1rP93iVPpwaE+gO0SsWmPiXB+jikdf6SizrT5qKasx5j8ABbHpFTx+vFXp9EnYQmLx02h1QTTrl6eDqxLnGjporxl3NL3agEvXdT0WmEost648sQOYAeJS9Q7bfUVoMGnjo4AZdUMQku50McDcMWcBPvr0SzbTAFDfvJqwLzgxwATnCgnp4wDl6Aa+Ax283gghmj+vj7feE2KBBRMW3FzOpLOADl0Isb5587h/U4gGvkt5v60Z1VLG8BhYjbzRwyQZemwAd6cCR5/XFWLYZRIMpX39AR0tjaGGiGzLVyhse5C9RKC6ai42ppWPKiBagOvaYk8lO7DajerabOZP46Lby5wKjw1HCRx7p9sVMOWGzb/vA1hwiWc6jm3MvQDTogQkiqIhJV0nBQBTU+3okKCFDy9WwferkHjtxib7t3xIUQtHxnIwtx4mpg26/HfwVNVDb4oI9RHmx5WGelRVlrtiw43zboCLaxv46AZeB3IlTkwouebTr1y2NjSpHz68WNFjHvupy3q8TFn3Hos2IAk4Ju5dCo8B3wP7VPr/FGaKiG+T+v+TQqIrOqMTL1VdWV1DdmcbO8KXBz6esmYWYKPwDL5b5FA1a0hwapHiom0r/cKaoqr+27/XcrS5UwSMbQAAAABJRU5ErkJggg==)](https://deepwiki.com/parallelcc/MiCTS)

[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;[English](/README_en.md)&nbsp;&nbsp;|&nbsp;&nbsp;[Русский](/README_ru.md)&nbsp;&nbsp;|&nbsp;&nbsp;Türkçe

Circle to Search özelliğini herhangi bir Android 9-15 cihazda tetikleyin

*Bu uygulama yalnızca Circle to Search'ü tetiklemeyi amaçlar ve başarıyla tetiklendikten sonra oluşabilecek sorunları ele alamaz*

## Nasıl Kullanılır

1. [Google](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox) uygulamasının en son sürümünü yükleyin, otomatik başlamasına izin verin, arka plan kısıtlamalarını devre dışı bırakın, ve Google uygulamasını varsayılan asistan olarak ayarlayın


2. MiCTS uygulamasını yükleyin ve açın
   - Şanslıysanız, MiCTS başlatıldığında Circle to Search root olmadan doğrudan tetiklenecektir
   - Hiçbir şey olmadıysa, büyük olasılıkla Google cihazınız için Circle to Search'ü devre dışı bırakmıştır (Logcat'te `Omni invocation failed: not enabled` mesajını kontrol ederek onaylayabilirsiniz). Aşağıdakileri **root ile** deneyin:
     - LSPosed'de modülü etkinleştirin, [MiCTS ayarlarında](#how-to-enter-settings) `Google için cihaz kimliği taklidi` özelliğini etkinleştirin ve Google'ı yeniden başlatmaya zorlayın
     - Hala işe yaramıyorsa, [GMS-Flags](https://github.com/polodarb/GMS-Flags) kullanarak `com.google.android.apps.search.omnient.device` flag'ını `45631784` true olarak değiştirin


3. Tetikleme metodunu ayarlayın
   - MiCTS'yi başlatmak tetikleyecektir, böylece Quick Ball, Xposed Edge, ShortX, vb. gibi diğer uygulamaları kullanabilirsiniz, tetikleme yöntemini özelleştirmek için MiCTS'yi başlatmayı eylem olarak ayarlayın
   - MiCTS bir tetikleyici kutucuk sağlar, böylece bunu Hızlı Ayarlar paneline ekleyebilir ve tıklayarak tetikleyebilirsiniz
   - Xiaomi cihazları için MiCTS, MiCTS ayarlarından etkinleştirilebilen `Uzun basma hareketiyle tetikle` ve `Ana ekran butonuna basılı tutarak tetikleme` için yerleşik desteğe sahiptir (MiCTS'yi yükledikten sonra modülü etkinleştirmeniz ve telefonu yeniden başlatmanız gerekir)
   - Android 13 ve üzeri sürümleri çalıştıran Samsung cihazları için, [Galaxy Store](https://galaxystore.samsung.com/detail/com.samsung.android.app.routineplus) veya [Good Lock](https://galaxystore.samsung.com/detail/com.samsung.android.goodlock) adresinden "Routines+" uygulamasını indirip yükleyebilirsiniz. Ardından, güç düğmesine uzun basma gibi Düğme eylemiyle MiCTS'yi başlatan rutinler oluşturmak için Ayarlar > Modlar ve Rutinler'e gidin.
   

## Ayarlar

### Ayarlara Nasıl Girilir
- Ayarlar seçeneğini göstermek için MiCTS uygulama simgesine uzun basın, ardından erişmek için üzerine basın
- LSPosed'deki Modüller sayfasından MiCTS'ye tıklayın, ardından ayarlar simgesine tıklayarak girin
- Erişmek için Hızlı Ayarlar paneli kutucuğuna uzun basın

### Uygulama Ayarları
- Varsayılan tetikleme gecikmesi: MiCTS başlatılarak tetiklendiğinde gecikme
- Bildirim kutucuğu tetikleme gecikmesi: Hızlı Ayarlar paneli kutucuğu tarafından tetiklendiğinde gecikme

### Modül Ayarları
LSPosed'de modülü etkinleştirmeniz gerekir
- Sistem tetikleme servisi: Tetikleme tarafından kullanılan sistem hizmeti. Yalnızca desteklenen hizmetler gösterilecektir. LSPosed'de kapsama Sistem Çerçevesi eklemeniz gerekir
   - VIS: Android 9-15'i destekler. Google'ı varsayılan asistan uygulaması olarak ayarlamanız gerekir ve bazı cihazlar için tetikleme sırasında ekran kenarı yanıp söner. Modül etkinleştirilmezse, yalnızca bu hizmet kullanılacaktır.
   - CSHelper: Android 14 QPR3 ve üzeri sürümleri destekler. Google'ı varsayılan asistan uygulaması olarak ayarlamanıza gerek yoktur ve tetikleme sırasında ekran kenarı yanıp sönmez.
   - CSService: Android 15 ve üzeri sürümleri destekler. Circle to Search için özel bir hizmet, CSHelper ile aynı etkiyi verir


- Uzun basma hareketiyle ile tetikleyin: Yalnızca Xiaomi cihazlarda desteklenir. LSPosed'de kapsama Sistem Başlatıcı/POCO Başlatıcı eklemeniz gerekiyor


- Ana ekran düğmesine uzun basarak tetikleyin: Yalnızca Xiaomi cihazlarda desteklenir. LSPosed'de kapsama Sistem Çerçevesi eklemeniz gerekir


- Google için cihaz kimliği taklidi: LSPosed'de Google'ı kapsama eklemek gerekir
   - Üretici: Google'ın okuduğu `ro.product.manufacturer` değerini değiştirin
   - Marka: Google'ın okuduğu `ro.product.brand` değerini değiştirin
   - Model: Google'ın okuduğu `ro.product.model` değerini değiştirin
   - Cihaz: Google'ın okuduğu `ro.product.device` değerini değiştirin

## SSS

### "Trigger failed!" istemi

Büyük olasılıkla Google varsayılan asistan olarak ayarlanmadığı içindir, kontrol edin

### Circle to Search yerine Google asistanı görünüyor

Google'ın en son sürüm olduğundan emin olun

### Bazen başarılı bir şekilde tetiklenmiyor ve arayüz yalnızca Google açıldıktan sonra görünüyor

Bu muhtemelen tombstone mekanizmasından kaynaklanmaktadır. Cihazınızda ilgili ayarların olup olmadığını kontrol edin ve Google'ı beyaz listeye ekleyin, örneğin pil tasarrufunda "Kısıtlama yok" seçeneğini belirleyin

Bu sorun, `Sistem tetikleyici hizmeti` Modül Ayarlarında `CSHelper` olarak ayarlandığında ortaya çıkmamalıdır

## Yıldız Geçmişi

<a href="https://star-history.com/#parallelcc/micts&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />
 </picture>
</a>
