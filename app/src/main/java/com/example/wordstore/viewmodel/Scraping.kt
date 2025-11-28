package com.example.wordstore.viewmodel

import com.example.wordstore.data.Definition
import com.example.wordstore.data.ExamplePair
import com.example.wordstore.data.FetchState
import com.example.wordstore.data.TranslationResult
import com.example.wordstore.data.WordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

suspend fun fetchCambridgeTranslation(word: String): FetchState {
    val url = "https://dictionary.cambridge.org/dictionary/english-turkish/${word.trim().lowercase()}"

    return withContext(Dispatchers.IO) {
        try {
            val doc: Document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
                .timeout(10000)
                .get()

            // Ana sözlük içeriği var mı kontrol et (kelime bulunamadı sayfası farklı olabilir)
            val dictionaryBody = doc.selectFirst("div.pr.dictionary")
            if (dictionaryBody == null) {
                if (doc.select("div.pr.entry-body__el").isEmpty()) {
                    return@withContext FetchState.WordNotFound
                }
            }


            val wordEntries = mutableListOf<WordEntry>()

            // Her bir ana anlam bloğunu (noun, verb vs.) seç
            // div.pr.entry-body__el -> Bu her bir kelime türü için bir blok gibi duruyor
            val entryElements = doc.select("div.pr.entry-body__el")

            if (entryElements.isEmpty()) {
                // Eğer hiç entry bulunamadıysa (beklenmedik bir durum veya sayfa yapısı değişikliği)
                if (doc.selectFirst("h1.heading > span.hw")?.text()?.equals(word, ignoreCase = true) == true) {
                    // Kelime başlıkta var ama entry yoksa, "No meanings found" gibi bir durum olabilir.
                    // Ya da Cambridge bazen sadece başlığı verir ama detaylı tanım bloğu olmaz.
                    // Bu durumu ayrıca ele almak gerekebilir. Şimdilik WordNotFound gibi davranalım.
                    return@withContext FetchState.WordNotFound
                }
                return@withContext FetchState.WordNotFound
            }


            for (entryElement in entryElements) {
                val partOfSpeech = entryElement.selectFirst("span.pos.dpos")?.text()?.trim() ?: "Unknown POS"
                val definitions = mutableListOf<Definition>()

                // Anlam bloklarını (sense blocks) seç. Her bir POS için birden fazla anlam olabilir.
                // .sense-body dsense_b -> Bu her bir anlam için bir blok
                // Bazen .def-block.ddef_block içinde de direkt çeviriler olabilir.
                // Bu selector'leri birleştirebiliriz: entryElement.select(".sense-body.dsense_b, .def-block.ddef_block")
                // DÜZELTME: Hem sense-body hem de def-block seçildiğinde, eğer def-block sense-body içindeyse
                // aynı içerik iki kez işleniyor (bir kez parent, bir kez child olarak).
                // Bu yüzden sadece en içteki anlam bloğu olan .def-block.ddef_block'u seçiyoruz.
                // Eğer hiç blok bulunamazsa aşağıdaki if (senseBlocks.isEmpty()) bloğu devreye girip genel aramayı yapacak.
                val senseBlocks = entryElement.select("div.def-block.ddef_block")

                if (senseBlocks.isEmpty()) { // Eğer bu POS için anlam bloğu yoksa, doğrudan entryElement içindeki çevirilere bak
                    val directTranslations = entryElement.select("span.trans.dtrans.dtrans-se, span.trans.dtrans")
                        .mapNotNull { it.text()?.trim() }
                        .filter { it.isNotBlank() }
                    if (directTranslations.isNotEmpty()) {
                        definitions.add(Definition(turkishTranslations = directTranslations))
                    }
                } else {
                    for (senseBlock in senseBlocks) {
                        val translations = senseBlock.select("span.trans.dtrans.dtrans-se, span.trans.dtrans") // .dtrans-se Türkçe için daha spesifik olabilir
                            .mapNotNull { it.text()?.trim() }
                            .filter { it.isNotBlank() } // Boş çevirileri filtrele

                        if (translations.isEmpty()) continue // Çeviri yoksa bu anlam bloğunu atla

                        val examples = mutableListOf<ExamplePair>()
                        // Örnek cümle bloklarını seç (div.examp.dexamp)
                        val exampleBlocks = senseBlock.select("div.examp.dexamp")
                        for (exBlock in exampleBlocks) {
                            val engExample = exBlock.selectFirst("span.eg.deg")?.text()?.trim()
                            // Örnek cümlenin Türkçe çevirisi de aynı `.trans.dtrans` sınıfını kullanabilir.
                            // İngilizce örneğin hemen altındaki `.trans` elemanını almaya çalışalım.
                            val turExample = exBlock.selectFirst("span.trans.dtrans")?.text()?.trim()

                            if (engExample != null && turExample != null && engExample.isNotBlank() && turExample.isNotBlank()) {
                                examples.add(ExamplePair(engExample, turExample))
                            }
                        }
                        definitions.add(Definition(turkishTranslations = translations, examples = examples))
                    }
                }
                if (definitions.isNotEmpty()) {
                    wordEntries.add(WordEntry(partOfSpeech = partOfSpeech, definitions = definitions))
                }
            }

            if (wordEntries.isEmpty()) {
                // Hiçbir anlamlı veri bulunamadıysa (belki sayfa yapısı tamamen değişti)
                return@withContext FetchState.WordNotFound // Ya da farklı bir hata durumu
            }

            FetchState.Success(TranslationResult(word = word, entries = wordEntries))

        } catch (e: IOException) {
            // Ağ hatası veya parse hatası
            e.printStackTrace()
            FetchState.Error("Network error or parsing failed: ${e.localizedMessage}")
        } catch (e: Exception) {
            // Diğer beklenmedik hatalar
            e.printStackTrace()
            FetchState.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }
}