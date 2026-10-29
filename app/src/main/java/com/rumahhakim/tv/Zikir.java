package com.rumahhakim.tv;

/**
 * Twenty-four zikir, a different one for each hour of the day: zikir N at N o'clock, and the 24th at
 * midnight. Each has its Arabic text, how it is read, what it means, and the number its sound goes
 * by (files/zikir/NN.* pushed to the TV, the family's own recording, or assets/zikir/NN-*.* bundled
 * with the app).
 */
final class Zikir {
    final int number;
    final String arabic, rumi, maksud;

    private Zikir(int number, String arabic, String rumi, String maksud) {
        this.number = number;
        this.arabic = arabic;
        this.rumi = rumi;
        this.maksud = maksud;
    }

    static final Zikir[] ALL = {
            new Zikir(1, "سُبْحَانَ اللّٰهِ", "Subhanallah", "Maha Suci Allah"),
            new Zikir(2, "اَلْحَمْدُ لِلّٰهِ", "Alhamdulillah", "Segala puji bagi Allah"),
            new Zikir(3, "اَللّٰهُ أَكْبَرُ", "Allahu Akbar", "Allah Maha Besar"),
            new Zikir(4, "أَشْهَدُ أَنْ لَا إِلٰهَ إِلَّا اللّٰهُ، وَأَشْهَدُ أَنَّ مُحَمَّدًا رَسُولُ اللّٰهِ",
                    "Asyhadu an la ilaha illallah, wa asyhadu anna Muhammadar rasulullah",
                    "Aku bersaksi bahawa tiada tuhan melainkan Allah, dan aku bersaksi bahawa Muhammad itu utusan Allah"),
            new Zikir(5, "أَسْتَغْفِرُ اللّٰهَ الْعَظِيمَ", "Astaghfirullahal 'azim",
                    "Aku memohon ampun kepada Allah Yang Maha Agung"),
            new Zikir(6, "سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ، سُبْحَانَ اللّٰهِ الْعَظِيمِ",
                    "Subhanallahi wa bihamdihi, subhanallahil 'azim",
                    "Maha Suci Allah dan segala puji bagi-Nya, Maha Suci Allah Yang Maha Agung"),
            new Zikir(7, "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللّٰهِ", "La hawla wa la quwwata illa billah",
                    "Tiada daya dan tiada kekuatan melainkan dengan pertolongan Allah"),
            new Zikir(8, "حَسْبُنَا اللّٰهُ وَنِعْمَ الْوَكِيلُ", "Hasbunallah wa ni'mal wakil",
                    "Cukuplah Allah bagi kami, dan Dialah sebaik-baik pelindung"),
            new Zikir(9, "اَللّٰهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ، إِنَّكَ حَمِيدٌ مَجِيدٌ",
                    "Allahumma salli 'ala Muhammad wa 'ala ali Muhammad, kama sallaita 'ala Ibrahim wa 'ala ali Ibrahim, innaka hamidum majid",
                    "Ya Allah, limpahkanlah selawat ke atas Muhammad dan keluarganya, sebagaimana Engkau limpahkan ke atas Ibrahim dan keluarganya; sesungguhnya Engkau Maha Terpuji lagi Maha Mulia"),
            new Zikir(10, "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيمِ", "Bismillahir rahmanir rahim",
                    "Dengan nama Allah Yang Maha Pemurah lagi Maha Penyayang"),
            new Zikir(11, "سُبْحَانَ اللّٰهِ، وَالْحَمْدُ لِلّٰهِ، وَلَا إِلٰهَ إِلَّا اللّٰهُ، وَاللّٰهُ أَكْبَرُ",
                    "Subhanallah, walhamdulillah, wa la ilaha illallah, wallahu akbar",
                    "Maha Suci Allah, segala puji bagi Allah, tiada tuhan melainkan Allah, dan Allah Maha Besar"),
            new Zikir(12, "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
                    "Rabbana atina fid dunya hasanah, wa fil akhirati hasanah, wa qina 'azaban nar",
                    "Ya Tuhan kami, berilah kami kebaikan di dunia dan kebaikan di akhirat, dan peliharalah kami daripada azab neraka (al-Baqarah 2:201)"),
            new Zikir(13, "لَا إِلٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
                    "La ilaha illa anta subhanaka inni kuntu minaz zalimin",
                    "Tiada tuhan melainkan Engkau, Maha Suci Engkau, sesungguhnya aku termasuk orang yang zalim (al-Anbiya 21:87)"),
            new Zikir(14, "حَسْبِيَ اللّٰهُ لَا إِلٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
                    "Hasbiyallahu la ilaha illa huwa, 'alaihi tawakkaltu wa huwa rabbul 'arsyil 'azim",
                    "Cukuplah Allah bagiku, tiada tuhan melainkan Dia; kepada-Nya aku bertawakal, dan Dialah Tuhan Arasy yang agung (at-Taubah 9:129)"),
            new Zikir(15, "لَا إِلٰهَ إِلَّا اللّٰهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
                    "La ilaha illallahu wahdahu la syarika lah, lahul mulku wa lahul hamdu wa huwa 'ala kulli syai'in qadir",
                    "Tiada tuhan melainkan Allah Yang Esa, tiada sekutu bagi-Nya; bagi-Nya kerajaan dan pujian, dan Dia Maha Berkuasa atas segala sesuatu"),
            new Zikir(16, "رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ",
                    "Rabbighfirli wa tub 'alayya innaka antat tawwabur rahim",
                    "Ya Tuhanku, ampunilah aku dan terimalah taubatku; sesungguhnya Engkau Maha Penerima taubat lagi Maha Penyayang"),
            new Zikir(17, "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ", "Ya Hayyu ya Qayyum, bi rahmatika astaghith",
                    "Wahai Yang Maha Hidup, wahai Yang Maha Berdiri Sendiri, dengan rahmat-Mu aku memohon pertolongan"),
            new Zikir(18, "اَللّٰهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي",
                    "Allahumma innaka 'afuwwun tuhibbul 'afwa fa'fu 'anni",
                    "Ya Allah, sesungguhnya Engkau Maha Pemaaf dan suka memaafkan, maka maafkanlah aku"),
            new Zikir(19, "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا",
                    "Rabbana la tuzigh qulubana ba'da idz hadaitana",
                    "Ya Tuhan kami, janganlah Engkau pesongkan hati kami setelah Engkau beri kami petunjuk (Ali 'Imran 3:8)"),
            new Zikir(20, "رَبِّ زِدْنِي عِلْمًا", "Rabbi zidni 'ilma", "Ya Tuhanku, tambahkanlah ilmuku (Taha 20:114)"),
            new Zikir(21, "اَللّٰهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
                    "Allahumma salli wa sallim 'ala nabiyyina Muhammad",
                    "Ya Allah, limpahkanlah selawat dan salam ke atas nabi kami Muhammad"),
            new Zikir(22, "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا",
                    "Rabbana hab lana min azwajina wa dzurriyyatina qurrata a'yun, waj'alna lil muttaqina imama",
                    "Ya Tuhan kami, kurniakanlah kepada kami pasangan dan zuriat yang menyejukkan mata, dan jadikanlah kami pemimpin orang bertakwa (al-Furqan 25:74)"),
            new Zikir(23, "اَللّٰهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
                    "Allahumma a'inni 'ala dzikrika wa syukrika wa husni 'ibadatik",
                    "Ya Allah, bantulah aku untuk mengingati-Mu, bersyukur kepada-Mu dan memperelok ibadahku kepada-Mu"),
            new Zikir(24, "بِسْمِ اللّٰهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
                    "Bismillahil ladzi la yadhurru ma'asmihi syai'un fil ardhi wa la fis sama'i wa huwas sami'ul 'alim",
                    "Dengan nama Allah, yang bersama nama-Nya tiada sesuatu pun di bumi dan di langit dapat memudaratkan, dan Dialah Yang Maha Mendengar lagi Maha Mengetahui"),
    };

    /** The zikir for an hour of the day (0 to 23): zikir N at N o'clock, the 24th at midnight. */
    static Zikir forHour(int hour) {
        int h = ((hour % 24) + 24) % 24;
        return ALL[(h == 0 ? 24 : h) - 1];
    }
}
