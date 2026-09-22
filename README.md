# BitWhisper

Asisten suara Android offline bergaya JARVIS.

## Status prototype

- Fondasi Android Kotlin native
- Model default: Whisper base multilingual dan Qwen2.5 1.5B Instruct Q4_K_M; gratis/open-weight dan berjalan lokal
- Foreground service untuk operasi background
- Accessibility Service untuk memasukkan teks ke field aktif
- Router intent offline: buka aplikasi, catatan, timer, flashlight, dan dikte
- Abstraksi `OfflineChatEngine` untuk integrasi model chatbot lokal GGUF/llama.cpp
- Riwayat percakapan lokal dan pilihan output Text/Voice/Text + Voice
- Variasi perintah awal Bahasa Jawa (Jawa/ngoko) dan pilihan bahasa input otomatis, Indonesia, atau Jawa
- Dua mode chatbot: Umum dan Scientific
- Fondasi agent planner, memory store, dan local skills sebelum integrasi chatbot Qwen
- Kerangka wake word "Hey BitWhisper" dengan Volume Up sebagai fallback; engine keyword offline masih perlu dipasang
- Trigger Volume Up dan pipeline Whisper masih dalam tahap implementasi dan harus diuji pada Infinix/XOS

## Target perangkat

Diuji dan ditargetkan untuk **Infinix GT 30 Pro / XOS**. Karena XOS dapat menghentikan service background, BitWhisper perlu diberi izin autostart dan battery usage `Unrestricted`.

## Target penggunaan

1. Aktifkan BitWhisper dan izinkan mikrofon, notifikasi, Accessibility Service, serta unrestricted battery usage.
2. Di XOS, izinkan BitWhisper pada pengaturan **Auto-start** dan jangan masukkan aplikasi ke daftar memory cleanup.
3. Jika Game Mode/XArena mengambil alih tombol volume, nonaktifkan shortcut volume khusus game untuk pengujian BitWhisper.
2. Tahan Volume Up untuk merekam dikte.
3. Lepaskan tombol untuk memproses ucapan secara lokal.
4. Perintah perangkat diproses oleh `CommandRouter`; pertanyaan umum diteruskan ke chatbot lokal.

Semua model AI direncanakan berjalan di perangkat. Tidak ada layanan cloud yang diwajibkan.
