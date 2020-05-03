package de.bigboot.ggtools.fang

class MatchManager {
    private var force = false
    private val queue = HashSet<String>()

    fun join(id: String): Boolean {
        return queue.add(id)
    }

    fun leave(id: String): Boolean {
        return queue.remove(id)
    }

    fun canPop(): Boolean = force || queue.size >= 10

    fun force() {
        force = true
    }

    fun pop() = queue.shuffled().take(10).apply {
        queue.removeAll(this)
        force = false
    }

    fun getPlayers(): Collection<String> = queue

    fun getNumPlayers() = queue.size
}