package com.nangmanroad.core.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** A comment the user wrote on this phone; [parentId] makes it a reply. */
data class LocalComment(
    val id: String,
    val spotId: String,
    val parentId: String?,
    val text: String,
    val isQuestion: Boolean,
    val createdAt: Long,
)

/** A tip the user suggested on someone else's post. */
data class LocalTip(val spotId: String, val tip: HoneyTip)

/**
 * What the user created while not signed in to the sync server: their own posts, comments and
 * suggested tips, plus a few lists the server otherwise keeps. Stored as JSON in preferences;
 * photos live in the app's files directory.
 */
class PostStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("phone_posts", Context.MODE_PRIVATE)

    fun loadPosts(author: Author, now: Long = System.currentTimeMillis()): List<Spot> =
        readArray(KEY_POSTS).map { it.toSpot(author, now) }

    fun addPost(post: Spot, createdAt: Long = System.currentTimeMillis()) {
        writeArray(KEY_POSTS, readArray(KEY_POSTS).apply { put(post.toJson(createdAt)) })
    }

    fun loadComments(): List<LocalComment> = readArray(KEY_COMMENTS).map {
        LocalComment(
            id = it.getString("id"),
            spotId = it.getString("spotId"),
            parentId = it.optString("parentId").ifEmpty { null },
            text = it.getString("text"),
            isQuestion = it.getBoolean("isQuestion"),
            createdAt = it.getLong("createdAt"),
        )
    }

    fun addComment(comment: LocalComment) {
        writeArray(
            KEY_COMMENTS,
            readArray(KEY_COMMENTS).apply {
                put(
                    JSONObject()
                        .put("id", comment.id)
                        .put("spotId", comment.spotId)
                        .put("parentId", comment.parentId ?: "")
                        .put("text", comment.text)
                        .put("isQuestion", comment.isQuestion)
                        .put("createdAt", comment.createdAt),
                )
            },
        )
    }

    fun loadTips(): List<LocalTip> = readArray(KEY_TIPS).map { LocalTip(it.getString("spotId"), it.toTip()) }

    fun addTip(tip: LocalTip) {
        writeArray(KEY_TIPS, readArray(KEY_TIPS).apply { put(tip.tip.toJson().put("spotId", tip.spotId)) })
    }

    var visited: Set<String>
        get() = prefs.getStringSet(KEY_VISITED, null)?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_VISITED, value).apply()

    var savedProducts: Set<String>
        get() = prefs.getStringSet(KEY_PRODUCTS, null)?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_PRODUCTS, value).apply()

    /** Spots queued for the car's 저장 목록; delivered once account sync exists. */
    var carQueue: Set<String>
        get() = prefs.getStringSet(KEY_CAR_QUEUE, null)?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_CAR_QUEUE, value).apply()

    private fun readArray(key: String): JSONArray = JSONArray(prefs.getString(key, "[]"))

    private fun writeArray(key: String, array: JSONArray) {
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> = (0 until length()).map { transform(getJSONObject(it)) }

    private fun Spot.toJson(createdAt: Long) = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("region", region)
        .put("address", address)
        .put("lat", location.latitude)
        .put("lng", location.longitude)
        .put("category", category.name)
        .put("story", story)
        .put("photos", JSONArray(photos))
        .put("tips", JSONArray(tips.map { it.toJson() }))
        .put("products", JSONArray(products.map { it.toJson() }))
        .put("visibility", visibility.name)
        .put("approximate", approximateLocation)
        .put("createdAt", createdAt)

    private fun JSONObject.toSpot(author: Author, now: Long): Spot {
        val photos = getJSONArray("photos")
        return Spot(
            id = getString("id"),
            title = getString("title"),
            region = getString("region"),
            address = getString("address"),
            location = LatLng(getDouble("lat"), getDouble("lng")),
            category = Category.valueOf(getString("category")),
            author = author,
            story = getString("story"),
            photos = (0 until photos.length()).map { photos.getString(it) },
            likeCount = 0,
            saveCount = 0,
            postedDaysAgo = TimeUnit.MILLISECONDS.toDays(now - getLong("createdAt")).toInt(),
            tips = getJSONArray("tips").map { it.toTip() },
            products = getJSONArray("products").map { it.toProduct() },
            comments = emptyList(),
            visibility = Visibility.valueOf(getString("visibility")),
            approximateLocation = getBoolean("approximate"),
        )
    }

    private fun HoneyTip.toJson() = JSONObject()
        .put("id", id)
        .put("kind", kind.name)
        .put("summary", summary)
        .put("detail", detail)
        .put("status", status.name)
        .put("helpful", helpfulCount)
        .put("confirmedDaysAgo", confirmedDaysAgo)
        .put("reporter", reporter)

    private fun JSONObject.toTip() = HoneyTip(
        id = getString("id"),
        kind = TipKind.valueOf(getString("kind")),
        summary = getString("summary"),
        detail = getString("detail"),
        status = TipStatus.valueOf(getString("status")),
        helpfulCount = getInt("helpful"),
        confirmedDaysAgo = getInt("confirmedDaysAgo"),
        reporter = getString("reporter"),
    )

    private fun Product.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("reason", reason)
        .put("recommender", recommender)
        .put("price", priceWon ?: -1)
        .put("sponsored", isSponsored)
        .put("image", imageUrl)

    private fun JSONObject.toProduct() = Product(
        id = getString("id"),
        name = getString("name"),
        reason = getString("reason"),
        recommender = getString("recommender"),
        priceWon = getInt("price").takeIf { it >= 0 },
        isSponsored = getBoolean("sponsored"),
        imageUrl = getString("image"),
    )

    private companion object {
        const val KEY_POSTS = "posts"
        const val KEY_COMMENTS = "comments"
        const val KEY_TIPS = "tips"
        const val KEY_VISITED = "visited"
        const val KEY_PRODUCTS = "saved_products"
        const val KEY_CAR_QUEUE = "car_queue"
    }
}

/** Builds the reply tree for a spot from the sample comments plus what the user wrote here. */
fun mergeComments(base: List<Comment>, local: List<LocalComment>, myName: String): List<Comment> {
    fun LocalComment.toComment() = Comment(id, myName, text, "방금 전", isQuestion)
    val replies = local.filter { it.parentId != null }.groupBy { it.parentId }
    val merged = base.map { comment ->
        comment.copy(replies = comment.replies + replies[comment.id].orEmpty().map { it.toComment() })
    }
    val mine = local.filter { it.parentId == null }.sortedByDescending { it.createdAt }.map { top ->
        top.toComment().copy(replies = replies[top.id].orEmpty().map { it.toComment() })
    }
    return mine + merged
}
