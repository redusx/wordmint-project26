<p align="center">
  <h1 align="center">📚 WordStore</h1>
  <p align="center">
    <strong>Akıllı Kelime Defteriniz — Çeviri, Tanım ve Telaffuz Bir Arada</strong>
  </p>
  <p align="center">
    <a href="#özellikler">Özellikler</a> •
    <a href="#ekran-görüntüleri">Ekran Görüntüleri</a> •
    <a href="#mimari">Mimari</a> •
    <a href="#kurulum">Kurulum</a> •
    <a href="#teknoloji-yığını">Teknolojiler</a>
  </p>
</p>

---

## 🌟 Nedir?

**WordStore**, yabancı dil öğrenenlerin kelime hazinelerini güçlendirmek için tasarlanmış modern bir Android uygulamasıdır. Sadece bir kelime defteri değil; **yapay zekâ destekli çeviri**, **sözlük entegrasyonu**, **telaffuz dinleme** ve **çevrimdışı çalışabilme** yetenekleriyle tam bir dil öğrenme asistanıdır.

> Bir kelime aratın → tanımını, örnek cümlelerini ve fonetik gösterimini görün → çevirisini alın → tek tuşla kaydedin. Hepsi **premium, cam etkili (glassmorphism)** bir arayüzde.

---

## ✨ Özellikler

### 🔍 Akıllı Kelime Arama
- **Sözlük API** entegrasyonu ile İngilizce kelimelerin detaylı tanımlarını, sözcük türlerini, eşanlamlı/zıtanlamlılarını ve örnek cümlelerini anında görüntüleyin.
- Aranılan her kelime için **fonetik transkripsiyon** bilgisi sunulur.

### 🌐 Çoklu Dil Çevirisi (Cihaz Üzerinde)
- **Google ML Kit** ile çeviri doğrudan cihaz üzerinde yapılır — internet bağlantısı gerekmez!
- Dil modelleri tek seferde indirilir ve çevrimdışı kullanıma hazır hale gelir.
- **60+ desteklenen dil** arasında özgürce geçiş yapabilirsiniz.
- İngilizce tanımlar ve örnek cümleler otomatik olarak seçilen hedef dile çevrilir.

### 🔊 Telaffuz Dinleme
- Kaydettiğiniz her kelimenin doğru telaffuzunu **tek dokunuşla dinleyin**.
- Google Dictionary ses API'si üzerinden yüksek kaliteli ses akışı sağlanır.

### 💾 Kişisel Kelime Defteri
- Kelimeleri anlamları, çevirileri, fonetik bilgileri ve örnek cümleleriyle birlikte **yerel veritabanına** kaydedin.
- Kaydedilen kelimeler arasında kolayca gezinin, detaylarını inceleyin veya silin.
- **Room Database** ile verileriniz her zaman güvende.

### 🎨 Premium Liquid Glass Tasarım
- **VisionOS / Glassmorphism** esinli, modern ve şık arayüz.
- Animasyonlu kartlar, gradient butonlar, shimmer yükleme efektleri ve pürüzsüz geçişler.
- **Material 3 (Material You)** temelinde özenle tasarlanmış karanlık tema.
- Özel ikon seti ve uyumlu tipografi.

---

## 🏗️ Mimari

Uygulama **MVVM (Model-View-ViewModel)** mimarisi üzerine inşa edilmiştir:

```
📦 com.example.wordstore
├── 📂 data
│   ├── 📂 local
│   │   ├── database/     # Room Database & DAO
│   │   └── entity/       # Word entity
│   ├── 📂 remote
│   │   ├── api/          # Retrofit API tanımları
│   │   └── model/        # API veri modelleri
│   └── 📂 repository     # Repository katmanı
├── 📂 domain
│   └── model/            # İş mantığı modelleri
├── 📂 ui
│   ├── components/       # Yeniden kullanılabilir UI bileşenleri
│   │   ├── GlassComponents  # Liquid Glass UI kiti
│   │   ├── Components       # Genel UI bileşenleri
│   │   └── PronunciationPlayer  # Ses oynatıcı
│   ├── screens/
│   │   ├── home/         # Ana ekran (kelime listesi)
│   │   └── addword/      # Kelime ekleme & arama ekranı
│   └── theme/            # Tema, renkler, tipografi, ikonlar
├── 📂 viewmodel          # ViewModel katmanı
├── 📂 util               # Yardımcı sınıflar
│   ├── TranslationManager      # ML Kit çeviri yöneticisi
│   └── ModelDownloadNotificationManager
├── MainActivity.kt
└── WordStoreApplication.kt
```

### Veri Akışı

```
Kullanıcı Girişi
    ↓
ViewModel (DictionaryViewModel / WordViewModel)
    ↓
Repository (DictionaryRepository / WordRepository)
    ↓
┌─────────────────┐    ┌──────────────────────┐
│  Dictionary API  │    │  Room Local Database  │
│  (Retrofit)      │    │  (SQLite)             │
└─────────────────┘    └──────────────────────┘
    ↓
ML Kit Translation (Cihaz üzeri çeviri)
    ↓
UI (Jetpack Compose)
```

---

## 🛠️ Teknoloji Yığını

| Kategori | Teknoloji |
|---|---|
| **Dil** | Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Mimari** | MVVM, ViewModel, LiveData, StateFlow |
| **Veritabanı** | Room (SQLite) |
| **Ağ İstekleri** | Retrofit + OkHttp + Gson |
| **Çeviri Motoru** | Google ML Kit Translation (çevrimdışı) |
| **Navigasyon** | Jetpack Navigation Compose |
| **Eşzamanlılık** | Kotlin Coroutines + Flow |
| **HTML Ayrıştırma** | Jsoup |
| **Min SDK** | API 24 (Android 7.0) |
| **Target SDK** | API 35 |

---

## 📥 Kurulum

### Gereksinimler
- Android Studio Hedgehog (2023.1.1) veya üzeri
- JDK 11+
- Android SDK 35

### Adımlar

1. **Depoyu klonlayın:**
   ```bash
   git clone https://github.com/redusx/WordStoreApp.git
   cd WordStoreApp
   ```

2. **Android Studio'da açın** ve Gradle sync işlemini tamamlayın.

3. **Uygulamayı çalıştırın:** Bir emülatör veya fiziksel cihaz seçerek `Run` butonuna basın.

> **Not:** Dil modellerini ilk kullanımda indirmeniz gerekecektir. WiFi bağlantısı önerilir ancak mobil veri ile de indirme yapılabilir.

---

## 📖 Kullanım

1. **Ana Ekran:** Kaydettiğiniz tüm kelimeleri premium kartlar halinde görüntüleyin. İstatistiklerinizi takip edin.
2. **Kelime Ekle:** `+` butonuna basarak yeni kelime arama ekranına gidin.
3. **Kelime Aratın:** İngilizce bir kelime yazın → tanımları, çevirileri ve örnek cümleleri anında görün.
4. **Dil Seçin:** Kaynak ve hedef dili değiştirerek farklı dil çiftlerinde çeviri yapın.
5. **Kaydet:** Beğendiğiniz kelimeyi tüm detaylarıyla birlikte tek tuşla kaydedin.
6. **Detay Görüntüle:** Ana ekranda herhangi bir kelime kartına dokunarak detaylı bilgilere, fonetik gösterime ve telaffuz dinleme özelliğine erişin.

---

## 🤝 Katkıda Bulunma

Katkılarınızı memnuniyetle karşılıyoruz! Lütfen bir **Issue** açın veya **Pull Request** gönderin.

---

## 📄 Lisans

Bu proje açık kaynak olarak sunulmaktadır. Detaylar için `LICENSE` dosyasına bakınız.

---

<p align="center">
  <strong>WordStore</strong> ile kelime öğrenmek hiç bu kadar keyifli olmamıştı! 🚀
</p>