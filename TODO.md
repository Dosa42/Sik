# DTS-TEAM.v1 — Kurulum, kayıtlı URL ve Room TODO planı

## Dayanak ve mevcut durum

Bu plan, `DTS-TEAM.v1(2).zip` içindeki proje ile paylaşılan ekran görüntüsündeki işlerin karşılaştırmasına dayanır. ZIP üzerinde değişiklik yapılmadı.

- Projede tek Android modülü `app_user` var. `MainActivity.kt` bir URL giriş alanı ve WebView oluşturuyor; girilen HTTP(S) adresini açıyor. `ws://` ve `wss://` adresleri `UserCore.nativeConnect` üzerinden yerel koda gidiyor.
- `MainActivity.kt` hata olduğunda adresi ve hata açıklamasını WebView içinde metin olarak gösteriyor. Geri tuşuyla WebView geçmişinde gezinme mevcut.
- `gradle/libs.versions.toml` yalnızca AGP sürümünü tanımlıyor. `app_user/build.gradle.kts` içinde Room/KSP bağımlılığı yok; dosyanın sonunda bunların sonra ekleneceği yazıyor. Room veritabanı, entity ve DAO dosyaları yok.
- Girilen URL kalıcı depoya yazılmıyor; uygulama açılışında kayıtlı URL okunmuyor. Kurulum ekranı ile URL sıfırlama akışı yok.
- Yerel `ClientSession` sınıfında `disconnect()` var; Kotlin/JNI katmanında bu işlevi çağıran bir giriş noktası yok.

## Hedef davranış

1. Kayıtlı URL yoksa kurulum ekranı açılır; kullanıcı geçerli bir sunucu adresi girip kaydeder.
2. URL Room'a başarıyla kaydedildikten sonra mevcut HTTP(S) WebView veya WS(S) bağlantı yolu açılır.
3. Sonraki açılışlarda kayıtlı URL okunur ve kullanıcıdan tekrar istenmeden aynı açma yolu çalışır.
4. Kullanıcı kayıtlı adresi değiştirebilir veya sıfırlayabilir. Sıfırlama hem veritabanındaki adresi hem açık ekran/bağlantı durumunu temizleyip kurulum ekranına döndürür.
5. Ana sayfa yüklenemediğinde hata ekranı, açıklamanın yanında çalışır durumda yeniden dene ve adresi değiştir eylemlerini sunar.

## 1. Gradle, Kotlin, KSP ve Room uyumu

- [ ] `gradle/libs.versions.toml`, `app_user/build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties` ve `.github/workflows/build-apk.yml` dosyalarındaki mevcut AGP, Gradle, JDK, SDK ve NDK ayarlarını birlikte değerlendir.
- [ ] Mevcut AGP 9.4.0 yapılandırmasının yerleşik Kotlin desteğiyle çalışacak KSP ve Room sürümlerini resmi sürüm/uyumluluk belgelerinden doğrula. Sürümleri tahmin ederek ekleme; Gradle sync ve derleme sonucuyla seçimi doğrula.
- [ ] Uyumlu KSP sürümünü ve Room sürümünü version catalog'a ekle. KSP eklentisini ve `room-runtime` ile `room-compiler` bağımlılıklarını `app_user` modülüne bağla; Room derleyicisini `ksp(...)` ile çalıştır.
- [ ] Room şema çıktısının proje içinde sürümlenmesi için KSP/Room şema dizinini yapılandır. İlk şema dosyasının üretildiğini kontrol et.
- [ ] Var olan NDK/CMake/WebSocket derlemesini etkilemediğini `:app_user:assembleDebug` ile doğrula. Uyum sorunu çıkarsa sürüm ve yapılandırma kararını proje notlarına yaz.

**Bitti ölçütü:** Gradle sync başarılı, Room kaynak üretimi gerçekleşiyor, debug APK derleniyor; sürüm katalogu, modül ve CI ayarları birbirleriyle tutarlı.

## 2. Room veri modeli ve erişimi

- [ ] `app_user/src/main/java/com/example/hostuser/` altında tek kayıtlı sunucu adresini temsil eden entity oluştur: sabit kimlik (`id = 1`) ve `url`. Gerekmeyen alanları ekleme.
- [ ] DAO'ya `getSavedHost()`, tek kaydı ekleyen/güncelleyen işlem ve kaydı silen işlem ekle. Tek kayıt kuralını veritabanı anahtarıyla koru.
- [ ] `LocalDb.kt` içinde Room veritabanını sürüm `1` olarak tanımla; uygulama bağlamını kullanan tek örnek üzerinden DAO'ya erişim sağla.
- [ ] Şema dışa aktarımını etkinleştir ve üretilen ilk şemayı projeye dahil et. Gelecek şema değişiklikleri için sürüm ve migration gerekeceğini proje notlarında belirt; otomatik veri silen migration kullanma.
- [ ] Veritabanı okuma/yazma/silme işlemlerini ana UI iş parçacığı dışında çalıştır. Mevcut `MainActivity` içindeki tek iş parçacıklı `io` yürütücüsü kullanılabilir; UI yalnızca ana iş parçacığında güncellensin.

**Bitti ölçütü:** Kaydet–oku–güncelle–sil işlemleri tek URL için çalışıyor; uygulama işlemi kapatılıp yeniden başlatıldığında kayıt korunuyor, silindiğinde boş dönüyor.

## 3. Kurulum ekranı ve kayıt akışı

- [ ] `MainActivity.kt` için iki açık durum tasarla: **kurulum** (URL yok) ve **gösterim** (URL var). Mevcut WebView ve URL dönüştürme davranışını yeni akışa bağla.
- [ ] Açılışta Room'dan URL oku. Okuma tamamlanana kadar yanlışlıkla kurulum/gösterim ekranını kısa süreli gösterme; sonuç boşsa kurulum, doluysa kayıtlı adresi aç.
- [ ] Kurulum ekranına URL giriş alanı, kaydet/aç eylemi ve anlaşılır giriş hatası ekle. Boş veya desteklenmeyen adresi kaydetme. `https://`, `http://`, `ws://`, `wss://`, ngrok alan adı ve mevcut LAN adresi kullanımını değerlendir.
- [ ] Kaydedilecek değeri kırpılmış ve tekrar açılabilir biçimde tut. WebView için `toWebUrl(...)` ve WebSocket için `nativeConnect(...)` dallarını tek açma işlevinden çalıştır; mevcut ngrok başlığını koru.
- [ ] Kaydetme sürerken düğmenin tekrar basılmasını önle. Room yazması başarılı olduğunda gösterim durumuna geç; başarısızsa kurulum ekranında kalıp hatayı göster.
- [ ] İlk kurulumdan sonra Activity yeniden oluşturulduğunda ve uygulama işlemi yeniden başladığında kayıtlı URL'nin otomatik açıldığını doğrula.

**Bitti ölçütü:** İlk açılışta URL soruluyor; başarılı kayıt sonrası içerik açılıyor; sonraki açılışta giriş alanına ihtiyaç kalmadan aynı URL açılıyor.

## 4. Adres değiştirme ve sıfırlama

- [ ] Gösterim ekranından erişilebilir **Adresi değiştir / sıfırla** eylemi ekle. Kullanıcı adresi yeniden girebilsin; yalnızca uygulama verilerini temizlemek zorunda kalmasın.
- [ ] Sıfırlamada Room kaydını sil; WebView'daki mevcut sayfayı kapat/boşalt ve kurulum durumuna dön. Açık WebSocket oturumu için `UserCore` ve `jni_user.cpp` üzerinden `ClientSession.disconnect()` çağrısını erişilebilir kıl.
- [ ] Kayıt silme ve açık oturumu kapatma işlemlerinin sırasını belirle. Silme başarısız olursa kurulum ekranında “adres silindi” izlenimi verme.
- [ ] Adres değiştirirken önceki adresin yerine yeni kayıt yazıldığını ve yeniden açılışta yalnızca son adresin kullanıldığını doğrula.

**Bitti ölçütü:** Sıfırlamadan sonra uygulama yeniden açıldığında kurulum görünür; eski URL otomatik yüklenmez ve eski WebSocket oturumu açık kalmaz.

## 5. Hata ekranı, bağlantılar ve gezinme

- [ ] `MainActivity.kt` içindeki `onReceivedError` akışını ana sayfa hataları için düzenle. Hatalı URL ve kısa hata açıklaması gösterilmeye devam etsin.
- [ ] Hata ekranına **Yeniden dene** (son kayıtlı adresi tekrar açar) ve **Adresi değiştir** (kurulum/düzenleme ekranına gider) eylemleri ekle. Bağlantılar/düğmeler gerçekten çalışsın; hata HTML'ine yalnızca görünür metin eklemekle yetinme.
- [ ] WebView içindeki normal geri/ileri geçmişi ile kurulum, gösterim ve hata durumları arasındaki geri davranışını tanımla. Hata ekranından dönüşte aynı hata sayfasına sıkışılmadığını kontrol et.
- [ ] HTTP(S) yükleme hatalarını ve WS(S) bağlantı hatalarının bugün yalnızca `logcat`te görünmesini ayrı ele al. WS(S) için UI'da hata gösterimi isteniyorsa yerel koddan Kotlin'e durum bildirimi ekle; bağlantı başarılı olmuş gibi ekran gösterme.

**Bitti ölçütü:** Ağ/adres hatasında kullanıcı yeniden deneyebiliyor veya adresi düzeltebiliyor; mevcut WebView geri gezinmesi bozulmuyor.

## 6. Proje notları ve doğrulama

- [ ] Proje notlarına kurulum akışını, Room tek kayıt şemasını, seçilen Room/KSP/AGP sürümlerini, URL sıfırlama davranışını ve hata ekranı eylemlerini ekle. Mevcut `THIRD_PARTY.txt` bağımlılık kaydını yalnızca ilgili değişiklik varsa güncelle.
- [ ] `./gradlew :app_user:assembleDebug :app_user:assembleRelease` çalıştır; yerel Android SDK yoksa aynı görevleri mevcut GitHub Actions akışında doğrula. Derleme sonucu doğrulanmadan “tamamlandı” işaretleme.
- [ ] Kurulum ve saklama için şu elle kontrol dizisini uygula: temiz kurulum → URL kaydı → içerik açma → uygulamayı kapat/aç → otomatik açma → adres değiştirme → kapat/aç → son adres → sıfırlama → kapat/aç → kurulum ekranı.
- [ ] Ayrı adres türlerini kontrol et: ngrok alan adı, `https://` URL, izin verilen yerel `http://IP:port`, `ws://` ve `wss://`. Geçersiz/boş giriş, ağ yokken yeniden deneme ve hata ekranından adres değiştirme yollarını da kontrol et.
- [ ] Room DAO'nun tek kayıt kuralı ile kaydet/oku/sil davranışını doğrulayan odaklı test ekle. UI akışının cihaz veya emülatörde çalıştığını ayrıca doğrula.
- [ ] CI'nin ürettiği APK'ların hem debug hem release çıktılarında bulunduğunu ve mevcut yerel kütüphanelerin paketlenmeye devam ettiğini kontrol et.

**Bitti ölçütü:** Derlemeler başarılı; temiz kurulum, kalıcılık, otomatik açma, değiştirme, sıfırlama, hata eylemleri ve mevcut HTTP/WS yolları doğrulanmış; sonuçlar proje notlarına kaydedilmiş.

## Beklenen dosya değişiklikleri

| Dosya/alan | Planlanan değişiklik |
| --- | --- |
| `gradle/libs.versions.toml` | Room ve KSP sürüm/eklenti tanımları |
| `app_user/build.gradle.kts` | KSP, Room çalışma zamanı ve derleyici; şema çıktısı |
| `app_user/src/main/java/com/example/hostuser/LocalDb.kt` | Room veritabanı |
| `app_user/src/main/java/com/example/hostuser/` | Kayıtlı host entity ve DAO |
| `app_user/src/main/java/com/example/hostuser/MainActivity.kt` | Kurulum/gösterim/hata durumları, açılışta okuma, kayıt, değiştirme ve sıfırlama |
| `app_user/src/main/java/com/example/hostuser/UserCore.kt` ve `app_user/src/main/cpp/jni_user.cpp` | Sıfırlamada WebSocket bağlantısını kapatma yolu |
| Room şema dizini | İlk veritabanı şeması |
| Proje notları | Akış, sürümler, veri modeli ve doğrulama sonuçları |

**Tamamlanma kuralı:** Kutular yalnızca ilgili kod, derleme ve davranış doğrulaması bittikten sonra işaretlenir. Bu dosya mevcut durum ve yapılacak işler planıdır; uygulama değişikliklerinin tamamlandığını ifade etmez.
