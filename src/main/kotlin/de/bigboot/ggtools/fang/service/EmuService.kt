package de.bigboot.ggtools.fang.service


interface EmuService {
    suspend fun getQueue(): List<String>
}
