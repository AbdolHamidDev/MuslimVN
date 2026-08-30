package com.example.muslimvn.domain.models

data class Reciter(
    val id: String,
    val name: String,
    val description: String,
    val identifier: String, // Mã dùng cho EveryAyah API
    val quranComId: Int,     // ID dùng cho Quran.com API (để lấy timing)
    val imageUrl: String,    // Đường dẫn ảnh trong assets
    val estimatedSizeMb: Int // Dung lượng ước tính tính bằng MB
)

val availableReciters = listOf(
    Reciter("1", "Mishary Rashid Alafasy", "Truyền cảm, hiện đại", "Alafasy_128kbps", 7, "images/Qari/Mishary_Rashid_Alafasy.webp", 600),
    Reciter("2", "Abdul Basit Abdul Samad", "Huyền thoại, âm vực rộng", "Abdul_Basit_Murattal_192kbps", 1, "images/Qari/Abdul_Basit_Abdul_Samad.webp", 900),
    Reciter("3", "Abdurrahman As-Sudais", "Nhanh, uy nghiêm (Imam Kaaba)", "Abdurrahmaan_As-Sudais_192kbps", 3, "images/Qari/Abdul_Rahman_Al-Sudais.webp", 900),
    Reciter("4", "Mahmoud Khalil Al-Husary", "Chuẩn mực, rõ chữ (Dễ học)", "Husary_128kbps", 6, "images/Qari/Mahmoud_Khalil_Al-Hussary.webp", 600),
    Reciter("5", "Mohamed Siddiq El-Minshawi", "U buồn, sâu lắng", "Minshawi_Murattal_128kbps", 8, "images/Qari/Muhammad_Siddiq_Al_Minshawi.webp", 600),
    Reciter("6", "Saud Al-Shuraim", "Truyền thống, nhịp điệu đều", "Saood_ash-Shuraym_128kbps", 4, "images/Qari/Saud_Al-Shuraim.webp", 600),
    Reciter("7", "Abu Bakr Al-Shatri", "Nhẹ nhàng, thanh thoát", "Abu_Bakr_Ash-Shaatree_128kbps", 12, "images/Qari/abu_bakr_al-shatri.webp", 600),
    Reciter("8", "Hani Ar-Rifai", "Đầy cảm xúc, thổn thức", "Hani_Rifai_192kbps", 5, "images/Qari/hani_ar-rifai.webp", 900)
)
