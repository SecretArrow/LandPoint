# Bantuan LandPoint

Semua di LandPoint berjalan di ponsel ini, termasuk halaman ini — ia disertakan di dalam aplikasi, jadi isinya sama persis meski tanpa sinyal. Setiap bagian di bawah sesuai dengan satu bagian aplikasi.

## Merekam

**Simpan tanah** memulai sebuah catatan. Beri nama bidang tanahnya, lalu tentukan letaknya dengan salah satu dari tiga cara: ketik lintang dan bujur langsung dari surat ukur atau sertifikat, ketuk **Pakai lokasi saya** sambil Anda berdiri di tanahnya, atau biarkan keduanya kosong dulu dan tambahkan batasnya nanti.

Saat GPS membaca, LandPoint merata-ratakan beberapa sinyal menjadi satu dan menampilkan ketelitiannya dalam meter. Pergilah ke luar ruangan — atap dan tajuk pohon yang rapat mengaburkan sinyal — dan biarkan ketelitinya stabil sebelum Anda menerima hasilnya. Posisi dari menara seluler dan Wi-Fi diberi label demikian, karena bisa meleset lebih dari satu blok kota.

**Cari alamat** mengisi kolom alamat dari koordinatnya. Ia memakai geocoder ponsel dan butuh koneksi; sisanya bekerja luring.

Foto tanah, dan foto tiap sudut, diambil dari kamera atau galeri. Setiap foto bisa diberi keterangan — misalnya, "patok batas di sisi utara". Salinan foto yang dibagikan bisa dicopot tag GPS-nya; setelannya ada di Privasi dan keamanan.

## Mengukur

**Tandai di peta** membuka pemilih sudut, tempat batas digambar sudut demi sudut. Ada lebih dari satu cara menaruh sebuah sudut, karena tidak ada satu cara yang cocok untuk semua tanah:

- Berdiri di sudutnya dan ambil sinyal GPS di tempat Anda berada.
- Taruh tanda di bidik jangka pada peta, lalu geser atau geser-halus sebelum menerimanya.
- Ketik koordinatnya sebagai derajat desimal, persis seperti tertulis di surat ukur.
- Ukur sebuah sudut dari sudut lain dengan jarak dan azimut, seperti cara surat hak menuliskan sisi-sisinya: derajat dari utara searah jarum jam, lalu panjang sisinya.
- Salin sudut yang dipakai dua bidang bersama, supaya batas yang berhimpit benar-benar berhimpit.

Tiga sudut atau lebih membentuk bangun tertutup dan menampilkan luas serta kelilingnya. Bila dua sisi bersilangan, aplikasi memperingatkan Anda — bangun yang bersilang bukan satu bangun tertutup, dan luasnya bukan luas tanah yang tercakup. Biasanya satu sudut salah urutan dan perlu digeser lebih awal atau lebih akhir.

**Ukur sambil berjalan** merekam batas terus-menerus selama Anda mengelilinginya, untuk sisi yang terlalu panjang atau terlalu lebat untuk dijadikan tempat berdiri di setiap sudut. Cara sudut demi sudut tetap lebih akurat; cara berjalan butuh pandangan langit yang jelas dan lintasan tertutup — selesailah di tempat Anda mulai.

Setiap sudut yang diukur menyimpan ketelitiannya, jadi Anda selalu bisa membedakan sinyal yang kokoh dari yang goyah dan tahu sudut mana yang perlu diukur ulang.

## Peta

Tab **Peta** menampilkan semua bidang tanah yang tersimpan. Ketuk sebuah pin atau garis batas untuk membuka catatannya. Pemilih gaya peta mengambang di atas peta: jalan raya, satelit, kontur, atau berkas peta yang Anda tambahkan sendiri.

Citra satelit menampilkan tanah seperti yang kamera lihat pada satu hari tertentu. Pakailah untuk mengenali tempatnya — bukan sebagai bukti di mana batas berjalan.

Dengan izin lokasi diberikan, chip readout membuka posisi Anda: lintang, bujur, ketelitian, ketinggian, kecepatan, arah, satelit mana yang dipakai sinyal dan seberapa kuat. Bilah skala menjawab "seberapa besar yang sedang saya lihat", dalam meter atau kaki.

Tanpa sinyal, petanya tetap kosong. Sudut, luas, posisi Anda sendiri, dan semua yang tersimpan tetap bekerja.

## Luring

Dua cara bekerja tanpa sinyal:

- **Unduh satu area.** Di Setelan, Peta luring: bingkai tanah yang diinginkan, periksa perkiraan tile dan ukurannya, lalu unduh. Unduhan bisa dijeda dan dilanjutkan, dan aplikasi menghentikan Anda sebelum satu bingkai melewati batas yang disimpan untuk satu area.
- **Tambahkan berkas peta (.pmtiles).** Satu berkas memuat satu wilayah utuh dan digambar sepenuhnya di ponsel — tidak memakai data sama sekali. Pilih dari pemilih gaya peta.

Citra satelit Esri dan kontur OpenTopoMap tidak bisa disimpan untuk pemakaian luring; ketentuan keduanya tidak mengizinkan pengunduhan massal. Berkas peta impor yang lisensinya Anda pegang sendiri adalah jalan untuk punya foto tanpa sinyal.

## Privasi

Semuanya tinggal di ponsel ini. Tidak ada akun, tidak ada pelacakan, tidak ada unggahan. Catatan dienkripsi dengan kunci yang disimpan di perangkat keras ponsel ini dan terkunci padanya — artinya juga satu-satunya cara memindahkan catatan ke ponsel baru adalah arsip cadangan, karena kuncinya tidak bisa dibawa pergi.

- **Kunci aplikasi** meminta sidik jari atau kunci layar setiap kali LandPoint dibuka.
- **Blokir tangkapan layar** menjaga aplikasi keluar dari pratinjau aplikasi terbaru dan menghentikan tangkapan layar serta perekaman layar.
- **Copot lokasi foto saat membagikan** mencopot tag GPS dari salinan yang dibagikan; koordinat yang dicetak pada gambar tetap terbaca, dan foto yang tersimpan tidak tersentuh.
- **Baru dihapus** menyimpan bidang yang dihapus selama 30 hari, lalu menghapusnya untuk selamanya.

Izin lokasi hanya dipakai untuk membaca sinyal GPS saat Anda mengukur atau melihat peta. Tidak ada yang dikirim ke mana pun.

## Ekspor

Dari Setelan, Cadangan dan ekspor:

- **Cadangan lengkap (dengan foto)** — satu arsip yang memuat setiap lokasi, batas, dan foto. Simpan salinannya di tempat lain: ponsel lain, kartu memori, penyimpanan awan Anda sendiri. Kata sandi membuat arsip tidak terbaca oleh siapa pun yang menemukannya — dan tidak ada cara memulihkan kata sandi yang terlupa.
- **JSON dan CSV** — salinan data; CSV terbuka di aplikasi lembar kerja.
- **Laporan PDF** — satu halaman rapi per bidang: sketsa peta, sudut, luas, pemilik, dan foto.
- **GPX dan KML** — untuk aplikasi GPS dan Google Earth.

Pulihkan dan impor mengembalikan catatan, dan menanyakan apa yang harus dilakukan bila lokasi yang diimpor sudah ada: lewati, ganti, atau simpan keduanya sebagai lokasi terpisah.
