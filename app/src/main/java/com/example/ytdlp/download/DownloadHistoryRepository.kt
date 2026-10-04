package com.example.ytdlp.download

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File

object DownloadHistoryRepository {

    private const val PREFS_NAME =
        "sydown_download_history"

    private const val KEY_HISTORY =
        "download_history"

    private const val MAX_HISTORY_ITEMS =
        500

    private val _items =
        MutableStateFlow<
                List<DownloadHistoryItem>
                >(
            emptyList()
        )

    val items:
            StateFlow<List<DownloadHistoryItem>> =
        _items.asStateFlow()

    private var initialized =
        false

    @Synchronized
    fun initialize(
        context: Context
    ) {
        if (initialized) {
            return
        }

        val appContext =
            context.applicationContext

        _items.value =
            readHistory(
                appContext
            )

        initialized =
            true
    }

    @Synchronized
    fun add(
        context: Context,
        item: DownloadHistoryItem
    ) {
        val appContext =
            context.applicationContext

        initialize(
            appContext
        )

        val previousItems =
            _items.value

        val allItems =
            buildList {

                add(item)

                addAll(
                    previousItems.filter {
                        it.id != item.id
                    }
                )
            }

        val updated =
            allItems.take(
                MAX_HISTORY_ITEMS
            )

        /*
         * إذا تجاوز السجل الحد الأقصى 500 عنصر،
         * نحذف الصور الدائمة للعناصر التي خرجت
         * من السجل تلقائيًا.
         */
        val keptIds =
            updated
                .mapTo(
                    mutableSetOf()
                ) {
                    it.id
                }

        val removedByLimit =
            allItems.filter {
                it.id !in keptIds
            }

        _items.value =
            updated

        writeHistory(
            context =
                appContext,

            items =
                updated
        )

        deletePersistentThumbnails(
            items =
                removedByLimit
        )
    }

    @Synchronized
    fun removeFromHistory(
        context: Context,
        ids: Set<String>
    ) {
        if (ids.isEmpty()) {
            return
        }

        val appContext =
            context.applicationContext

        initialize(
            appContext
        )

        /*
         * نحتفظ بالعناصر المراد حذفها مؤقتًا
         * حتى نستطيع تنظيف صورها الدائمة.
         */
        val removedItems =
            _items.value.filter {
                it.id in ids
            }

        val updated =
            _items.value.filterNot {
                it.id in ids
            }

        _items.value =
            updated

        writeHistory(
            context =
                appContext,

            items =
                updated
        )

        /*
         * حذف السجل لا يحذف ملف الفيديو أو الصوت.
         * هنا نحذف فقط Thumbnail الداخلية الخاصة
         * بسجل SyDown.
         */
        deletePersistentThumbnails(
            items =
                removedItems
        )
    }

    @Synchronized
    fun clearHistory(
        context: Context
    ) {
        val appContext =
            context.applicationContext

        initialize(
            appContext
        )

        val removedItems =
            _items.value

        _items.value =
            emptyList()

        writeHistory(
            context =
                appContext,

            items =
                emptyList()
        )

        /*
         * مسح السجل يعني أيضًا تنظيف الصور
         * الدائمة المرتبطة به، مع إبقاء ملفات
         * الوسائط نفسها دون تغيير.
         */
        deletePersistentThumbnails(
            items =
                removedItems
        )
    }

    /*
     * =====================================================
     * THUMBNAIL CLEANUP
     * =====================================================
     */

    private fun deletePersistentThumbnails(
        items: Collection<DownloadHistoryItem>
    ) {
        items.forEach { item ->

            val thumbnailPath =
                item.thumbnailPath
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: return@forEach

            try {

                val thumbnailFile =
                    File(
                        thumbnailPath
                    )

                if (
                    thumbnailFile.exists() &&
                    thumbnailFile.isFile
                ) {
                    thumbnailFile.delete()
                }

            } catch (_: Throwable) {

                /*
                 * فشل تنظيف صورة صغيرة لا يجب أن
                 * يمنع حذف عنصر السجل.
                 */
            }
        }
    }

    private fun readHistory(
        context: Context
    ): List<DownloadHistoryItem> {

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val raw =
            preferences.getString(
                KEY_HISTORY,
                null
            )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            buildList {

                for (
                index in 0 until array.length()
                ) {

                    val json =
                        array.optJSONObject(
                            index
                        )
                            ?: continue

                    val item =
                        DownloadHistoryItem
                            .fromJson(
                                json
                            )
                            ?: continue

                    add(item)
                }
            }

        } catch (
            _: Throwable
        ) {

            emptyList()
        }
    }

    private fun writeHistory(
        context: Context,
        items: List<DownloadHistoryItem>
    ) {
        val array =
            JSONArray()

        items.forEach { item ->

            array.put(
                item.toJson()
            )
        }

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_HISTORY,
                array.toString()
            )
            .apply()
    }
}