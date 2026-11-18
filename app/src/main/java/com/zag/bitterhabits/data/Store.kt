package com.zag.bitterhabits.data

import android.content.Context
import com.zag.bitterhabits.widget.Widgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class Freq { DAILY, WEEKLY }

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emblem: String,
    val color: Int,
    val freq: Freq = Freq.DAILY,
    val weeklyTarget: Int = 3,
    /** Planned rest days for every-day habits. 1 = Monday ... 7 = Sunday. */
    val restDays: Set<Int> = emptySet(),
    val reminderOn: Boolean = false,
    /** Minutes after midnight. */
    val reminderMinutes: Int = 8 * 60,
    val rateOn: Boolean = false,
    val archived: Boolean = false,
    val order: Int = 0,
)

data class AppData(
    val habits: List<Habit> = emptyList(),
    /** habitId -> epoch days ticked as done */
    val done: Map<String, Set<Long>> = emptyMap(),
    /** habitId -> epoch days marked as rest by hand */
    val rest: Map<String, Set<Long>> = emptyMap(),
    /** habitId -> (epoch day -> bitterness 1..5) */
    val ratings: Map<String, Map<Long, Int>> = emptyMap(),
    val setupDone: Boolean = false,
) {
    val active: List<Habit> get() = habits.filter { !it.archived }.sortedBy { it.order }
    val archivedHabits: List<Habit> get() = habits.filter { it.archived }.sortedBy { it.order }
    fun habit(id: String): Habit? = habits.firstOrNull { it.id == id }
    fun doneSet(id: String): Set<Long> = done[id] ?: emptySet()
    fun restSet(id: String): Set<Long> = rest[id] ?: emptySet()

    fun setDone(id: String, day: Long, on: Boolean): AppData {
        val s = doneSet(id).toMutableSet()
        if (on) s.add(day) else s.remove(day)
        return copy(done = done + (id to s))
    }

    fun setRest(id: String, day: Long, on: Boolean): AppData {
        val s = restSet(id).toMutableSet()
        if (on) s.add(day) else s.remove(day)
        return copy(rest = rest + (id to s))
    }

    fun setRating(id: String, day: Long, value: Int?): AppData {
        val m = (ratings[id] ?: emptyMap()).toMutableMap()
        if (value == null) m.remove(day) else m[day] = value
        return copy(ratings = ratings + (id to m))
    }

    fun upsert(h: Habit): AppData {
        val exists = habits.any { it.id == h.id }
        val list = if (exists) habits.map { if (it.id == h.id) h else it }
        else habits + h.copy(order = (habits.maxOfOrNull { it.order } ?: -1) + 1)
        return copy(habits = list)
    }

    fun delete(id: String): AppData = copy(
        habits = habits.filter { it.id != id },
        done = done - id, rest = rest - id, ratings = ratings - id,
    )

    fun move(id: String, delta: Int): AppData {
        val list = active.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j < 0 || j >= list.size) return this
        val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        val orders = list.mapIndexed { idx, h -> h.id to idx }.toMap()
        return copy(habits = habits.map { h -> orders[h.id]?.let { h.copy(order = it) } ?: h })
    }
}

/** The five habits offered on first run, in colour order. */
object Defaults {
    data class Starter(val name: String, val line: String, val emblem: String, val freq: Freq, val target: Int, val rest: Set<Int>)

    val starters = listOf(
        Starter("Meditation", "Ten minutes. Sit with it.", "focus", Freq.DAILY, 3, emptySet()),
        Starter("Move for 30 mins", "Walk, run, lift. Just move.", "bolt", Freq.WEEKLY, 3, emptySet()),
        Starter("Read 10 pages", "Paper, not a feed.", "book", Freq.WEEKLY, 4, emptySet()),
        Starter("Screentime <4 Hrs", "The feed will survive without you.", "nophone", Freq.DAILY, 3, emptySet()),
        Starter("Sleep & recover shot", "45ml. Bitter on purpose.", "shot", Freq.DAILY, 3, setOf(7)),
    )
}

object Store {
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data
    private var file: File? = null

    @Synchronized
    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.applicationContext.filesDir, "bitter_habits.json")
        file = f
        _data.value = try {
            if (f.exists()) fromJson(f.readText()) else AppData()
        } catch (e: Exception) {
            AppData()
        }
    }

    fun current(ctx: Context): AppData {
        init(ctx); return _data.value
    }

    @Synchronized
    fun update(ctx: Context, change: (AppData) -> AppData) {
        init(ctx)
        val next = change(_data.value)
        _data.value = next
        write(next)
        Widgets.refresh(ctx.applicationContext)
    }

    fun replaceAll(ctx: Context, json: String) {
        val parsed = fromJson(json)
        update(ctx) { parsed }
    }

    fun exportJson(ctx: Context): String = toJson(current(ctx)).toString(2)

    private fun write(d: AppData) {
        val f = file ?: return
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(toJson(d).toString())
        if (!tmp.renameTo(f)) {
            f.writeText(tmp.readText()); tmp.delete()
        }
    }

    // ---------- JSON ----------
    fun toJson(d: AppData): JSONObject {
        val o = JSONObject()
        o.put("version", 1)
        o.put("setupDone", d.setupDone)
        val hs = JSONArray()
        d.habits.forEach { h ->
            hs.put(JSONObject().apply {
                put("id", h.id); put("name", h.name); put("emblem", h.emblem); put("color", h.color)
                put("freq", h.freq.name); put("weeklyTarget", h.weeklyTarget)
                put("restDays", JSONArray(h.restDays.sorted()))
                put("reminderOn", h.reminderOn); put("reminderMinutes", h.reminderMinutes)
                put("rateOn", h.rateOn); put("archived", h.archived); put("order", h.order)
            })
        }
        o.put("habits", hs)
        o.put("done", setsToJson(d.done))
        o.put("rest", setsToJson(d.rest))
        val r = JSONObject()
        d.ratings.forEach { (id, m) ->
            val mo = JSONObject(); m.forEach { (day, v) -> mo.put(day.toString(), v) }; r.put(id, mo)
        }
        o.put("ratings", r)
        return o
    }

    private fun setsToJson(m: Map<String, Set<Long>>): JSONObject {
        val o = JSONObject()
        m.forEach { (id, s) -> o.put(id, JSONArray(s.sorted())) }
        return o
    }

    private fun setsFromJson(o: JSONObject?): Map<String, Set<Long>> {
        if (o == null) return emptyMap()
        val out = mutableMapOf<String, Set<Long>>()
        o.keys().forEach { id ->
            val a = o.getJSONArray(id)
            out[id] = (0 until a.length()).map { a.getLong(it) }.toSet()
        }
        return out
    }

    fun fromJson(text: String): AppData {
        val o = JSONObject(text)
        val hs = o.optJSONArray("habits") ?: JSONArray()
        val habits = (0 until hs.length()).map { i ->
            val h = hs.getJSONObject(i)
            val rd = h.optJSONArray("restDays") ?: JSONArray()
            Habit(
                id = h.getString("id"), name = h.getString("name"), emblem = h.optString("emblem", "squig"),
                color = h.optInt("color", 0), freq = Freq.valueOf(h.optString("freq", "DAILY")),
                weeklyTarget = h.optInt("weeklyTarget", 3),
                restDays = (0 until rd.length()).map { rd.getInt(it) }.toSet(),
                reminderOn = h.optBoolean("reminderOn", false), reminderMinutes = h.optInt("reminderMinutes", 480),
                rateOn = h.optBoolean("rateOn", false), archived = h.optBoolean("archived", false), order = h.optInt("order", i),
            )
        }
        val ratings = mutableMapOf<String, Map<Long, Int>>()
        o.optJSONObject("ratings")?.let { r ->
            r.keys().forEach { id ->
                val mo = r.getJSONObject(id)
                val m = mutableMapOf<Long, Int>()
                mo.keys().forEach { k -> m[k.toLong()] = mo.getInt(k) }
                ratings[id] = m
            }
        }
        return AppData(
            habits = habits,
            done = setsFromJson(o.optJSONObject("done")),
            rest = setsFromJson(o.optJSONObject("rest")),
            ratings = ratings,
            setupDone = o.optBoolean("setupDone", habits.isNotEmpty()),
        )
    }
}
