package de.bigboot.ggtools.fang.components.queue

import de.bigboot.ggtools.fang.components.ComponentSpec

interface QueueComponentSpec : ComponentSpec {
    companion object {
        const val ID_PREFIX = "QUEUE_MESSAGE"
    }
}