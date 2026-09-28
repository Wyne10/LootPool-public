package me.wyne.lootpool.core

class ListHandle<T>(
    private val getter: () -> List<T>,
    private val setter: (List<T>) -> Unit
) {

    val items: List<T>
        get() = getter()

    val size: Int
        get() = items.size

    val indices: IntRange
        get() = items.indices

    operator fun get(index: Int): T = items[index]

    fun getOrNull(index: Int): T? = items.getOrNull(index)

    operator fun set(index: Int, value: T) {
        setter(items.toMutableList().also { it[index] = value })
    }

    fun add(value: T) {
        setter(items + value)
    }

    fun removeAt(index: Int): T {
        val removed = items[index]
        setter(items.toMutableList().also { it.removeAt(index) })
        return removed
    }

    fun move(index: Int, delta: Int): Int {
        val target = (index + delta).coerceIn(0, size - 1)
        if (target == index) return index
        setter(items.toMutableList().also { it.add(target, it.removeAt(index)) })
        return target
    }

    fun <R> nested(index: Int, getChild: (T) -> List<R>, withChild: (T, List<R>) -> T): ListHandle<R> =
        ListHandle({ getChild(this[index]) }, { this[index] = withChild(this[index], it) })

}
