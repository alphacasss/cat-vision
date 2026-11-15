package id.cv.catvision.breed

/** Warna bulu yang dipakai untuk mencocokkan ciri visual kucing. */
enum class CoatColor(val label: String) {
    WHITE("Putih"),
    BLACK("Hitam"),
    BLUE("Abu-biru"),
    ORANGE("Oranye"),
    CREAM("Krem"),
    BROWN("Cokelat"),
    SILVER("Silver"),
    GOLDEN("Golden"),
    LILAC("Lilac"),
    ANY("Beragam"),
}

/** Pola bulu yang bisa dikenali dari foto. */
enum class CoatPattern(val label: String) {
    SOLID("Solid"),
    TABBY("Tabby/belang"),
    BICOLOR("Bicolor/tuxedo"),
    COLORPOINT("Colorpoint"),
    CALICO("Calico"),
    TORTIE("Tortie"),
    TICKED("Ticked"),
    SPOTTED("Spotted/rosette"),
    ANY("Beragam"),
}

/**
 * Satu entri database breed kucing. Data ini dipakai untuk ensiklopedia
 * in-app sekaligus sebagai dasar pencocokan ciri visual (warna + pola).
 */
data class CatBreed(
    val id: String,
    val name: String,
    val origin: String,
    val colors: Set<CoatColor>,
    val patterns: Set<CoatPattern>,
    val size: String,
    val weight: String,
    val lifespan: String,
    val temperament: String,
    val traits: List<String>,
    val care: String,
    val popularity: Int,
    val contest: Boolean = false,
    val colorpoint: Boolean = false,
    val modelHints: List<String> = emptyList(),
)

private fun colors(vararg values: CoatColor): Set<CoatColor> = values.toSet()
private fun patterns(vararg values: CoatPattern): Set<CoatPattern> = values.toSet()

/**
 * Database breed kucing populer, termasuk breed yang sering ikut kontes
 * (BSH, Scottish Fold, Ragdoll, American Curl, American Shorthair, dll).
 */
object CatBreeds {

    val all: List<CatBreed> = listOf(
        CatBreed(
            id = "british-shorthair",
            name = "British Shorthair",
            origin = "Inggris",
            colors = colors(CoatColor.BLUE, CoatColor.SILVER, CoatColor.WHITE, CoatColor.BLACK, CoatColor.CREAM, CoatColor.ORANGE, CoatColor.LILAC),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.COLORPOINT),
            size = "Sedang - besar",
            weight = "4 - 8 kg",
            lifespan = "12 - 17 tahun",
            temperament = "Tenang, mandiri, setia, tidak rewel",
            traits = listOf(
                "Kepala bulat besar dengan pipi tembem",
                "Bulu pendek sangat padat seperti plush",
                "Mata bulat tembaga, emas, atau biru",
                "Badan kekar dengan kaki tebal",
                "Varian colorpoint dikenal sebagai BSH Colorpoint"
            ),
            care = "Sisir 2-3 kali seminggu, bersihkan mata dan telinga rutin.",
            popularity = 10,
            contest = true,
            modelHints = listOf("tabby", "tiger cat")
        ),
        CatBreed(
            id = "scottish-fold",
            name = "Scottish Fold",
            origin = "Skotlandia",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.CALICO),
            size = "Kecil - sedang",
            weight = "3 - 6 kg",
            lifespan = "11 - 15 tahun",
            temperament = "Manis, tenang, sosial, suka menempel",
            traits = listOf(
                "Telinga terlipat ke depan dan ke bawah",
                "Wajah bulat seperti burung hantu",
                "Mata besar dan bulat",
                "Bulu pendek padat, semua warna diizinkan"
            ),
            care = "Bersihkan lipatan telinga, sisir mingguan, jaga berat badan.",
            popularity = 9,
            contest = true
        ),
        CatBreed(
            id = "ragdoll",
            name = "Ragdoll",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.CREAM, CoatColor.WHITE, CoatColor.BROWN, CoatColor.BLUE, CoatColor.LILAC),
            patterns = patterns(CoatPattern.COLORPOINT, CoatPattern.BICOLOR),
            size = "Besar",
            weight = "5 - 9 kg",
            lifespan = "12 - 17 tahun",
            temperament = "Sangat lembut, santai, mudah dibawa",
            traits = listOf(
                "Bulu semi-panjang dengan kerah leher",
                "Mata biru selalu",
                "Pola colorpoint dengan badan terang",
                "Kaki depan putih seperti sarung tangan",
                "Badan besar dan berat"
            ),
            care = "Sisir 3-4 kali seminggu, terutama bagian belakang dan ekor.",
            popularity = 9,
            contest = true,
            colorpoint = true,
            modelHints = listOf("siamese cat")
        ),
        CatBreed(
            id = "american-curl",
            name = "American Curl",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.CALICO),
            size = "Kecil - sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Ramah, aktif, ingin tahu, mudah bergaul",
            traits = listOf(
                "Telinga melengkung ke belakang seperti bulan sabit",
                "Bulu pendek atau semi-panjang",
                "Ekor lebat seperti plum",
                "Wajah sedikit segitiga dengan mata besar"
            ),
            care = "Jangan tekuk telinga; sisir 1-2 kali seminggu.",
            popularity = 6,
            contest = true
        ),
        CatBreed(
            id = "american-shorthair",
            name = "American Shorthair",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.SILVER, CoatColor.BLACK, CoatColor.WHITE, CoatColor.ORANGE, CoatColor.BROWN, CoatColor.CREAM),
            patterns = patterns(CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.SOLID),
            size = "Sedang",
            weight = "4 - 7 kg",
            lifespan = "13 - 17 tahun",
            temperament = "Tenang, ramah, mandiri, pemburu ulung",
            traits = listOf(
                "Bulu pendek rapat dan tahan air",
                "Kepala bulat lebar dengan moncong kotak",
                "Silver tabby klasik adalah warna ikonik",
                "Badan berotot dengan dada lebar"
            ),
            care = "Sisir mingguan, cukup aktif sehingga perlu mainan.",
            popularity = 8,
            contest = true,
            modelHints = listOf("tabby", "tiger cat")
        ),
        CatBreed(
            id = "persian",
            name = "Persian",
            origin = "Iran (Persia)",
            colors = colors(CoatColor.WHITE, CoatColor.CREAM, CoatColor.ORANGE, CoatColor.BLUE, CoatColor.BLACK, CoatColor.SILVER, CoatColor.GOLDEN),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.CALICO),
            size = "Sedang",
            weight = "3 - 6 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Tenang, manja, elegan, tidak suka keramaian",
            traits = listOf(
                "Bulu panjang sangat tebal",
                "Wajah datar dengan hidung pesek",
                "Mata bulat besar",
                "Badan cobby dengan kaki pendek"
            ),
            care = "Sisir harian, bersihkan mata dan hidung, butuh ruangan sejuk.",
            popularity = 9,
            contest = true,
            modelHints = listOf("persian cat")
        ),
        CatBreed(
            id = "exotic-shorthair",
            name = "Exotic Shorthair",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.CALICO),
            size = "Sedang",
            weight = "3 - 6 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Tenang, manja, ramah seperti Persian",
            traits = listOf(
                "Wajah datar seperti Persian",
                "Bulu pendek plush yang mudah dirawat",
                "Badan cobby dan bulat",
                "Mata besar dan lebar"
            ),
            care = "Sisir 2 kali seminggu, bersihkan lipatan mata.",
            popularity = 7,
            contest = true,
            modelHints = listOf("persian cat")
        ),
        CatBreed(
            id = "siamese",
            name = "Siamese",
            origin = "Thailand",
            colors = colors(CoatColor.CREAM, CoatColor.BROWN, CoatColor.BLUE, CoatColor.LILAC),
            patterns = patterns(CoatPattern.COLORPOINT),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 20 tahun",
            temperament = "Sangat vokal, cerdas, sosial, aktif",
            traits = listOf(
                "Colorpoint kontras: badan terang, wajah/telinga/kaki/ekor gelap",
                "Mata biru",
                "Badan langsing dan berotot",
                "Kepala segitiga dengan telinga besar"
            ),
            care = "Sisir mingguan, butuh interaksi dan stimulasi harian.",
            popularity = 9,
            contest = true,
            colorpoint = true,
            modelHints = listOf("siamese cat")
        ),
        CatBreed(
            id = "balinese",
            name = "Balinese",
            origin = "Thailand / Amerika Serikat",
            colors = colors(CoatColor.CREAM, CoatColor.BROWN, CoatColor.BLUE, CoatColor.LILAC),
            patterns = patterns(CoatPattern.COLORPOINT),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 18 tahun",
            temperament = "Cerdas, vokal, lembut, sosial",
            traits = listOf(
                "Seperti Siamese tetapi berbulu semi-panjang",
                "Ekor plum dan bulu halus",
                "Colorpoint dengan mata biru",
                "Badan panjang dan ramping"
            ),
            care = "Sisir 3 kali seminggu agar bulu tidak kusut.",
            popularity = 6,
            colorpoint = true,
            modelHints = listOf("siamese cat")
        ),
        CatBreed(
            id = "himalayan",
            name = "Himalayan",
            origin = "Amerika Serikat / Inggris",
            colors = colors(CoatColor.CREAM, CoatColor.WHITE, CoatColor.BLUE, CoatColor.BROWN, CoatColor.LILAC),
            patterns = patterns(CoatPattern.COLORPOINT),
            size = "Sedang",
            weight = "3 - 6 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Tenang, manja, lembut",
            traits = listOf(
                "Persian dengan pola colorpoint",
                "Mata biru",
                "Wajah datar dan bulu panjang",
                "Titik warna pada wajah, telinga, kaki, ekor"
            ),
            care = "Sisir harian dan bersihkan mata setiap hari.",
            popularity = 7,
            contest = true,
            colorpoint = true,
            modelHints = listOf("persian cat")
        ),
        CatBreed(
            id = "maine-coon",
            name = "Maine Coon",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.BROWN, CoatColor.SILVER, CoatColor.ORANGE, CoatColor.BLACK, CoatColor.WHITE, CoatColor.CREAM),
            patterns = patterns(CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.SOLID),
            size = "Sangat besar",
            weight = "5 - 11 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Ramah, cerdas, gagah, seperti anjing",
            traits = listOf(
                "Badan sangat besar dan panjang",
                "Bulu panjang dengan kerah leher",
                "Telinga berjumbai seperti lynx",
                "Moncong kotak dan ekor sangat lebat",
                "Sering memiliki pola tabby"
            ),
            care = "Sisir 3-4 kali seminggu, terutama bagian perut dan ekor.",
            popularity = 9,
            contest = true,
            modelHints = listOf("tabby", "tiger cat")
        ),
        CatBreed(
            id = "siberian",
            name = "Siberian",
            origin = "Rusia",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.SOLID),
            size = "Besar",
            weight = "5 - 9 kg",
            lifespan = "12 - 18 tahun",
            temperament = "Ramah, aktif, mandiri, penyayang",
            traits = listOf(
                "Bulu tiga lapis yang tahan dingin",
                "Badan kokoh dengan kaki kuat",
                "Ekor tebal dan berbulu",
                "Wajah agak bulat dengan mata almond"
            ),
            care = "Sisir 2-3 kali seminggu, lebih sering saat ganti bulu.",
            popularity = 6,
            contest = true
        ),
        CatBreed(
            id = "norwegian-forest",
            name = "Norwegian Forest Cat",
            origin = "Norwegia",
            colors = colors(CoatColor.BROWN, CoatColor.SILVER, CoatColor.BLACK, CoatColor.WHITE, CoatColor.ORANGE),
            patterns = patterns(CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.SOLID),
            size = "Besar",
            weight = "4 - 9 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Tenang, mandiri, ramah, suka memanjat",
            traits = listOf(
                "Bulu panjang dengan mantel tahan air",
                "Kaki belakang berbulu seperti celana",
                "Kepala segitiga dengan profil lurus",
                "Ekor plum yang panjang"
            ),
            care = "Sisir 2-3 kali seminggu, sediakan tempat memanjat.",
            popularity = 6,
            contest = true
        ),
        CatBreed(
            id = "bengal",
            name = "Bengal",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.GOLDEN, CoatColor.BROWN, CoatColor.SILVER),
            patterns = patterns(CoatPattern.SPOTTED, CoatPattern.TABBY),
            size = "Sedang - besar",
            weight = "4 - 8 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Sangat aktif, cerdas, atletis, suka air",
            traits = listOf(
                "Rosette seperti macan tutul",
                "Bulu berkilau seperti satin",
                "Badan atletis dan panjang",
                "Kepala lebar dengan mata almond"
            ),
            care = "Bulu mudah dirawat, butuh banyak mainan dan ruang gerak.",
            popularity = 8,
            contest = true,
            modelHints = listOf("tiger cat", "egyptian cat")
        ),
        CatBreed(
            id = "abyssinian",
            name = "Abyssinian",
            origin = "Ethiopia",
            colors = colors(CoatColor.BROWN, CoatColor.GOLDEN, CoatColor.BLUE, CoatColor.SILVER),
            patterns = patterns(CoatPattern.TICKED, CoatPattern.TABBY),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Sangat aktif, cerdas, ingin tahu",
            traits = listOf(
                "Bulu ticked dengan pita warna di tiap helai",
                "Badan langsing dan berotot",
                "Telinga besar dan tegak",
                "Mata almond hijau atau emas"
            ),
            care = "Sisir mingguan, sediakan banyak aktivitas.",
            popularity = 7,
            contest = true,
            modelHints = listOf("egyptian cat")
        ),
        CatBreed(
            id = "russian-blue",
            name = "Russian Blue",
            origin = "Rusia",
            colors = colors(CoatColor.BLUE),
            patterns = patterns(CoatPattern.SOLID),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "15 - 20 tahun",
            temperament = "Tenang, pemalu, setia pada satu orang",
            traits = listOf(
                "Bulu biru-abu solid berkilau",
                "Mata hijau zamrud",
                "Badan ramping dengan kaki panjang",
                "Senyum khas pada sudut mulut"
            ),
            care = "Sisir mingguan, cukup dengan lingkungan tenang.",
            popularity = 7,
            contest = true
        ),
        CatBreed(
            id = "chartreux",
            name = "Chartreux",
            origin = "Prancis",
            colors = colors(CoatColor.BLUE),
            patterns = patterns(CoatPattern.SOLID),
            size = "Sedang",
            weight = "3 - 7 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Tenang, cerdas, penyayang, tidak vokal",
            traits = listOf(
                "Bulu biru-abu tebal seperti wol",
                "Mata tembaga atau oranye",
                "Badan kekar dengan kaki pendek",
                "Senyum khas seperti Russian Blue"
            ),
            care = "Sisir 2 kali seminggu.",
            popularity = 5,
            contest = true
        ),
        CatBreed(
            id = "burmese",
            name = "Burmese",
            origin = "Myanmar / Thailand",
            colors = colors(CoatColor.BROWN, CoatColor.CREAM, CoatColor.BLUE, CoatColor.LILAC),
            patterns = patterns(CoatPattern.SOLID),
            size = "Sedang",
            weight = "3 - 6 kg",
            lifespan = "12 - 18 tahun",
            temperament = "Sangat sosial, vokal, manja",
            traits = listOf(
                "Bulu satin pendek dan halus",
                "Mata emas atau kuning",
                "Badan kompak dan berotot",
                "Warna sable cokelat gelap paling umum"
            ),
            care = "Sisir mingguan, butuh perhatian dan interaksi.",
            popularity = 6,
            contest = true
        ),
        CatBreed(
            id = "birman",
            name = "Birman",
            origin = "Myanmar / Prancis",
            colors = colors(CoatColor.CREAM, CoatColor.WHITE, CoatColor.BLUE, CoatColor.BROWN, CoatColor.LILAC),
            patterns = patterns(CoatPattern.COLORPOINT, CoatPattern.BICOLOR),
            size = "Sedang",
            weight = "4 - 6 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Tenang, manis, sosial, lembut",
            traits = listOf(
                "Colorpoint dengan sarung tangan putih",
                "Mata biru",
                "Bulu semi-panjang halus",
                "Kaki belakang juga memiliki white gloves"
            ),
            care = "Sisir 2-3 kali seminggu.",
            popularity = 6,
            contest = true,
            colorpoint = true,
            modelHints = listOf("siamese cat")
        ),
        CatBreed(
            id = "devon-rex",
            name = "Devon Rex",
            origin = "Inggris",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR),
            size = "Kecil",
            weight = "2 - 4 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Ceria, nakal, sangat sosial",
            traits = listOf(
                "Bulu keriting tipis dan lembut",
                "Telinga sangat besar",
                "Wajah elf dengan pipi menonjol",
                "Mata besar dan lebar"
            ),
            care = "Bersihkan telinga, jaga kehangatan, sisir dengan tangan.",
            popularity = 5,
            contest = true
        ),
        CatBreed(
            id = "cornish-rex",
            name = "Cornish Rex",
            origin = "Inggris",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR),
            size = "Kecil - sedang",
            weight = "2 - 5 kg",
            lifespan = "11 - 15 tahun",
            temperament = "Aktif, cerdas, suka bermain",
            traits = listOf(
                "Bulu keriting bergelombang",
                "Badan langsing dengan punggung melengkung",
                "Telinga besar",
                "Kaki panjang dan ekor tipis"
            ),
            care = "Jaga kehangatan, sisir ringan, bersihkan telinga.",
            popularity = 5,
            contest = true
        ),
        CatBreed(
            id = "sphynx",
            name = "Sphynx",
            origin = "Kanada",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.BICOLOR, CoatPattern.TABBY),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Sangat sosial, manja, aktif, hangat",
            traits = listOf(
                "Hampir tanpa bulu, kulit berkerut",
                "Telinga sangat besar",
                "Badan hangat dan berotot",
                "Mata besar seperti lemon"
            ),
            care = "Mandi rutin, bersihkan telinga, pakai pakaian saat dingin.",
            popularity = 7,
            contest = true
        ),
        CatBreed(
            id = "munchkin",
            name = "Munchkin",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR),
            size = "Kecil",
            weight = "2 - 4 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Ramah, ceria, suka bermain",
            traits = listOf(
                "Kaki sangat pendek",
                "Badan panjang seperti dachshund",
                "Kepala bulat dengan mata besar",
                "Semua warna dan pola diizinkan"
            ),
            care = "Jaga berat badan, sediakan akses tanpa lompatan tinggi.",
            popularity = 6,
            contest = true
        ),
        CatBreed(
            id = "savannah",
            name = "Savannah",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.GOLDEN, CoatColor.BROWN, CoatColor.SILVER),
            patterns = patterns(CoatPattern.SPOTTED, CoatPattern.TABBY),
            size = "Besar dan tinggi",
            weight = "5 - 11 kg",
            lifespan = "12 - 20 tahun",
            temperament = "Aktif, cerdas, eksotis, butuh ruang",
            traits = listOf(
                "Tubuh tinggi dengan kaki panjang",
                "Bintik hitam seperti serval",
                "Telinga besar dan tinggi",
                "Bulu pendek dengan pola spotted"
            ),
            care = "Butuh stimulasi tinggi, ruang gerak, dan mainan interaktif.",
            popularity = 5,
            contest = true,
            modelHints = listOf("tiger cat")
        ),
        CatBreed(
            id = "turkish-angora",
            name = "Turkish Angora",
            origin = "Turki",
            colors = colors(CoatColor.WHITE, CoatColor.BLACK, CoatColor.ORANGE, CoatColor.BLUE, CoatColor.CREAM),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR),
            size = "Kecil - sedang",
            weight = "2 - 5 kg",
            lifespan = "12 - 18 tahun",
            temperament = "Elegan, aktif, cerdas, suka perhatian",
            traits = listOf(
                "Bulu semi-panjang halus seperti sutra",
                "Badan ramping dan anggun",
                "Ekor plum yang panjang",
                "Mata biru, amber, atau odd-eye"
            ),
            care = "Sisir 2 kali seminggu, tidak perlu mandi sering.",
            popularity = 5,
            contest = true
        ),
        CatBreed(
            id = "turkish-van",
            name = "Turkish Van",
            origin = "Turki",
            colors = colors(CoatColor.WHITE, CoatColor.ORANGE, CoatColor.CREAM),
            patterns = patterns(CoatPattern.BICOLOR, CoatPattern.COLORPOINT),
            size = "Sedang - besar",
            weight = "4 - 9 kg",
            lifespan = "12 - 17 tahun",
            temperament = "Aktif, cerdas, suka air, mandiri",
            traits = listOf(
                "Badan putih dengan kepala dan ekor berwarna",
                "Bulu semi-panjang tahan air",
                "Mata biru, amber, atau odd-eye",
                "Badan berotot dan besar"
            ),
            care = "Sisir mingguan, siapkan akses air untuk bermain.",
            popularity = 4,
            contest = true
        ),
        CatBreed(
            id = "manx",
            name = "Manx",
            origin = "Pulau Man",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR),
            size = "Sedang",
            weight = "3 - 6 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Ceria, setia, pemburu ulung",
            traits = listOf(
                "Tidak memiliki ekor (tailless)",
                "Badan bulat dengan kaki belakang panjang",
                "Bulu pendek atau panjang",
                "Kepala bulat dengan pipi lebar"
            ),
            care = "Sisir mingguan, jaga punggung dan berat badan.",
            popularity = 4,
            contest = true
        ),
        CatBreed(
            id = "japanese-bobtail",
            name = "Japanese Bobtail",
            origin = "Jepang",
            colors = colors(CoatColor.WHITE, CoatColor.BLACK, CoatColor.ORANGE, CoatColor.CREAM),
            patterns = patterns(CoatPattern.CALICO, CoatPattern.BICOLOR, CoatPattern.SOLID, CoatPattern.TABBY),
            size = "Kecil - sedang",
            weight = "2 - 4 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Aktif, cerdas, vokal, sosial",
            traits = listOf(
                "Ekor pendek seperti pompom",
                "Bulu sedang dengan pola tricolor klasik",
                "Badan ramping dan berotot",
                "Kepala segitiga dengan mata besar"
            ),
            care = "Sisir mingguan, cukup mudah dirawat.",
            popularity = 4,
            contest = true
        ),
        CatBreed(
            id = "ragamuffin",
            name = "Ragamuffin",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.COLORPOINT),
            size = "Besar",
            weight = "4 - 9 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Sangat lembut, sabar, sosial",
            traits = listOf(
                "Bulu panjang tebal dan halus",
                "Badan besar dengan tulang kuat",
                "Mata besar dan ekspresif",
                "Semua warna dan pola diizinkan"
            ),
            care = "Sisir 3-4 kali seminggu.",
            popularity = 4,
            contest = true,
            colorpoint = true
        ),
        CatBreed(
            id = "scottish-straight",
            name = "Scottish Straight",
            origin = "Skotlandia",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.CALICO),
            size = "Kecil - sedang",
            weight = "3 - 6 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Tenang, manis, sosial",
            traits = listOf(
                "Seperti Scottish Fold tetapi telinga tegak",
                "Wajah bulat dan pipi tembem",
                "Bulu pendek padat",
                "Mata besar dan bulat"
            ),
            care = "Sisir mingguan, bersihkan telinga.",
            popularity = 5,
            contest = true
        ),
        CatBreed(
            id = "highland-fold",
            name = "Highland Fold",
            origin = "Skotlandia",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR, CoatPattern.CALICO),
            size = "Sedang",
            weight = "3 - 6 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Manis, tenang, penyayang",
            traits = listOf(
                "Scottish Fold berbulu panjang",
                "Telinga terlipat ke depan",
                "Bulu panjang dan lembut",
                "Wajah bulat dengan mata besar"
            ),
            care = "Sisir 3 kali seminggu, bersihkan lipatan telinga.",
            popularity = 4,
            contest = true
        ),
        CatBreed(
            id = "oriental-shorthair",
            name = "Oriental Shorthair",
            origin = "Inggris / Amerika Serikat",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.COLORPOINT),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Vokal, cerdas, sangat sosial",
            traits = listOf(
                "Badan panjang dan langsing",
                "Telinga sangat besar",
                "Kepala segitiga dengan hidung panjang",
                "Warna dan pola sangat beragam"
            ),
            care = "Sisir mingguan, butuh banyak interaksi.",
            popularity = 4,
            contest = true,
            colorpoint = true
        ),
        CatBreed(
            id = "tonkinese",
            name = "Tonkinese",
            origin = "Kanada / Amerika Serikat",
            colors = colors(CoatColor.CREAM, CoatColor.BROWN, CoatColor.BLUE, CoatColor.LILAC),
            patterns = patterns(CoatPattern.COLORPOINT, CoatPattern.SOLID),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Ramah, aktif, vokal, manja",
            traits = listOf(
                "Persilangan Siamese dan Burmese",
                "Mata aqua khas",
                "Bulu satin pendek",
                "Pola colorpoint atau solid"
            ),
            care = "Sisir mingguan, butuh teman bermain.",
            popularity = 4,
            contest = true,
            colorpoint = true,
            modelHints = listOf("siamese cat")
        ),
        CatBreed(
            id = "snowshoe",
            name = "Snowshoe",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.WHITE, CoatColor.CREAM, CoatColor.BROWN, CoatColor.BLUE),
            patterns = patterns(CoatPattern.COLORPOINT, CoatPattern.BICOLOR),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Ramah, cerdas, sosial, aktif",
            traits = listOf(
                "Colorpoint dengan kaki putih",
                "Mata biru",
                "Bulu pendek dengan pola bicolor",
                "Badan berotot"
            ),
            care = "Sisir mingguan.",
            popularity = 3,
            contest = true,
            colorpoint = true,
            modelHints = listOf("siamese cat")
        ),
        CatBreed(
            id = "korat",
            name = "Korat",
            origin = "Thailand",
            colors = colors(CoatColor.BLUE),
            patterns = patterns(CoatPattern.SOLID),
            size = "Kecil - sedang",
            weight = "2 - 4 kg",
            lifespan = "15 - 20 tahun",
            temperament = "Tenang, setia, cerdas, tidak vokal",
            traits = listOf(
                "Bulu biru-abu dengan ujung berkilau",
                "Mata hijau",
                "Kepala berbentuk hati",
                "Badan kecil dan berotot"
            ),
            care = "Sisir mingguan, cukup mudah dirawat.",
            popularity = 4,
            contest = true
        ),
        CatBreed(
            id = "bombay",
            name = "Bombay",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.BLACK),
            patterns = patterns(CoatPattern.SOLID),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Sosial, cerdas, aktif, seperti mini-panther",
            traits = listOf(
                "Bulu hitam pekat berkilau",
                "Mata tembaga atau emas",
                "Badan ramping dan berotot",
                "Kepala bulat dengan hidung hitam"
            ),
            care = "Sisir mingguan, butuh interaksi dan mainan.",
            popularity = 5,
            contest = true
        ),
        CatBreed(
            id = "singapura",
            name = "Singapura",
            origin = "Singapura",
            colors = colors(CoatColor.CREAM, CoatColor.BROWN),
            patterns = patterns(CoatPattern.TICKED, CoatPattern.TABBY),
            size = "Sangat kecil",
            weight = "2 - 3 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Aktif, cerdas, ramah, suka menempel",
            traits = listOf(
                "Salah satu breed terkecil",
                "Bulu ticked berwarna ivory dan cokelat",
                "Mata besar dan bulat",
                "Telinga besar"
            ),
            care = "Sisir mingguan, jaga kehangatan.",
            popularity = 3,
            contest = true
        ),
        CatBreed(
            id = "selkirk-rex",
            name = "Selkirk Rex",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.TABBY, CoatPattern.BICOLOR),
            size = "Sedang - besar",
            weight = "4 - 7 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Tenang, manja, ramah",
            traits = listOf(
                "Bulu keriting tebal seperti domba",
                "Pipi tembem dan badan cobby",
                "Mata bulat besar",
                "Semua warna diizinkan"
            ),
            care = "Sisir 2-3 kali seminggu agar bulu tidak menggumpal.",
            popularity = 4,
            contest = true
        ),
        CatBreed(
            id = "peterbald",
            name = "Peterbald",
            origin = "Rusia",
            colors = colors(CoatColor.ANY),
            patterns = patterns(CoatPattern.SOLID, CoatPattern.COLORPOINT, CoatPattern.TABBY),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Sosial, cerdas, aktif",
            traits = listOf(
                "Bulu sangat tipis atau hampir tanpa bulu",
                "Badan langsing dan panjang",
                "Telinga besar",
                "Kepala segitiga"
            ),
            care = "Jaga kehangatan, bersihkan kulit dan telinga rutin.",
            popularity = 3,
            contest = true,
            colorpoint = true
        ),
        CatBreed(
            id = "khao-manee",
            name = "Khao Manee",
            origin = "Thailand",
            colors = colors(CoatColor.WHITE),
            patterns = patterns(CoatPattern.SOLID),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 16 tahun",
            temperament = "Aktif, cerdas, vokal, ramah",
            traits = listOf(
                "Bulu putih bersih",
                "Mata biru, amber, atau odd-eye",
                "Badan berotot",
                "Kepala berbentuk hati"
            ),
            care = "Bersihkan mata dan telinga, sisir mingguan.",
            popularity = 3,
            contest = true
        ),
        CatBreed(
            id = "egyptian-mau",
            name = "Egyptian Mau",
            origin = "Mesir",
            colors = colors(CoatColor.SILVER, CoatColor.BROWN, CoatColor.GOLDEN, CoatColor.BLACK),
            patterns = patterns(CoatPattern.SPOTTED, CoatPattern.TABBY),
            size = "Sedang",
            weight = "3 - 5 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Aktif, cepat, cerdas, sedikit pemalu",
            traits = listOf(
                "Bintik alami pada bulu",
                "Bulu bertotol dengan pola spotted",
                "Badan atletis dan ramping",
                "Mata hijau atau amber"
            ),
            care = "Sisir mingguan, sediakan ruang untuk berlari.",
            popularity = 5,
            contest = true,
            modelHints = listOf("egyptian cat")
        ),
        CatBreed(
            id = "toyger",
            name = "Toyger",
            origin = "Amerika Serikat",
            colors = colors(CoatColor.ORANGE, CoatColor.GOLDEN, CoatColor.BROWN, CoatColor.SILVER),
            patterns = patterns(CoatPattern.TABBY, CoatPattern.SPOTTED),
            size = "Sedang",
            weight = "4 - 7 kg",
            lifespan = "12 - 15 tahun",
            temperament = "Ramah, aktif, cerdas",
            traits = listOf(
                "Pola tabby seperti harimau mainan",
                "Garis gelap melingkar di badan",
                "Badan panjang dan berotot",
                "Bulu pendek dengan kontras tinggi"
            ),
            care = "Sisir mingguan, butuh mainan dan aktivitas.",
            popularity = 3,
            contest = true,
            modelHints = listOf("tiger cat")
        ),
    )

    val byId: Map<String, CatBreed> = all.associateBy { it.id }
    val contestBreeds: List<CatBreed> = all.filter { it.contest }
}
