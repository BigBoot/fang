package de.bigboot.ggtools.fang.service

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.ScaleMethod
import com.sksamuel.scrimage.nio.PngWriter
import de.bigboot.ggtools.fang.api.mistforge.MistforgeApi
import de.bigboot.ggtools.fang.api.mistforge.model.Hero
import de.bigboot.ggtools.fang.api.mistforge.model.SkillId
import de.bigboot.ggtools.fang.api.mistforge.model.UpgradePath
import de.bigboot.ggtools.fang.components.ComponentSpec
import de.bigboot.ggtools.fang.utils.*
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ComponentInteractionEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import io.github.furstenheim.CopyDown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface MistforgeComponentSpec : ComponentSpec {
    companion object {
        const val ID_PREFIX = "QUEUE_MESSAGE"
    }
}

data class ButtonBuildDetails(val buildId: String): MistforgeComponentSpec {
    override fun id() = "$PREFIX:${buildId}"
    override fun component(): ActionComponent = Button.secondary(id(), "Details")

    companion object {
        private val PREFIX = "${MistforgeComponentSpec.ID_PREFIX}:BUTTON:GUIDE_DETAILS"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (guideId) -> ButtonBuildDetails(guideId) }
    }
}

@OptIn(FlowPreview::class)
class MistforgeService: AutostartService, KoinComponent {
    companion object {
        private val BUILD_URL_REGEX = Regex("""https://mistforge.net/build_viewer\?build_id=(\d+)""")
    }

    private val client: GatewayDiscordClient by inject()
    private val mistforge = MistforgeApi.create()
    private val heroes = mutableListOf<Hero>()
    private val background = ImmutableImage.loader().fromResource("/background.png")
    private val arrow = ImmutableImage.loader().fromResource("/arrow.png")

    init {
        client.eventDispatcher.on<MessageCreateEvent>()
            .flatMapConcat { event ->
                BUILD_URL_REGEX.findAll(event.message.content).map { Pair(event, it) }.asFlow() }
            .onEachSafe { handleMessage(it.first, it.second.groupValues[1]) }
            .launch()


        client.eventDispatcher.on<ComponentInteractionEvent>()
            .filter { it.customId.startsWith(MistforgeComponentSpec.ID_PREFIX) }
            .onEachSafe(this::handleInteraction)
            .launch()

        CoroutineScope(Dispatchers.IO).launch {
            heroes.addAll(mistforge.getHeroes().heroes)
        }
    }

    private suspend fun handleMessage(event: MessageCreateEvent, buildId: String) {
        val guide = mistforge.getGuide(buildId.toInt())

        var img = background.copy()

        for (i in 0 until 5) {

            val x = 7
            val y = 40 + (i*111)

            val name = when (i) {
                0 -> 1
                1 -> 2
                3 -> 3
                4 -> 4
                else -> 5
            }

            val icon = ImmutableImage.loader()
                .fromBytes(mistforge.getHeroImage(guide.heroId, "${name}.png").bytes())
                .scaleTo(96, 96, ScaleMethod.Bicubic)

            img = img.overlay(icon, x, y)
        }

        val talent = ImmutableImage.loader()
            .fromBytes(mistforge.getHeroImage(guide.heroId, "${guide.talent+5}.png").bytes())
            .scaleTo(96, 96, ScaleMethod.Bicubic)

        img = img.overlay(talent, 1117,262)

        for ((i, c) in guide.skills.withIndex())
        {
            val skill = (c-'a')/2
            val flipped = (c-'a') % 2 == 0

            val x = 126 + (i*101)
            val y = 52 + (skill*111)

            img = if(flipped) {
                img.overlay(arrow.flipX(), x, y)
            } else {
                img.overlay(arrow, x, y)
            }
        }

        event.message.channel.awaitSingle().createMessageCompat {
            messageReference(event.message.id)

            addEmbedCompat {
                title(guide.title)
                thumbnail("https://mistforge.net/img/hero_builder/${guide.heroId}/hero.png")
                author(guide.username,  "", "https://mistforge.net/customimg/${guide.userId}.png")
                url("https://mistforge.net/build_viewer?build_id=${buildId}")
                description(CopyDown().convert(guide.guide).take(150).plus("..."))
                image("attachment://build.png")
            }

            addComponent(ActionRow.of(ButtonBuildDetails(buildId).component()))

            addFile("build.png", img.bytes(PngWriter.MinCompression).inputStream())
        }.await()
    }
    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonBuildDetails) {
        event.deferReply().withEphemeral(true).awaitSafe()

        val guide = mistforge.getGuide(button.buildId.toInt())
        val hero = heroes.first { it.heroId == guide.heroId }

        val upgrades = mutableListOf<Pair<SkillId, UpgradePath>>()

        for (c in guide.skills)
        {
            val skill = (c-'a')/2
            val flipped = (c-'a') % 2 == 0

            val skillId = when(skill) {
                0 -> SkillId.LMB
                1 -> SkillId.RMB
                4 -> SkillId.E
                3 -> SkillId.Q
                else -> SkillId.Focus
            }

            upgrades.add(Pair(skillId, if (flipped) UpgradePath.Left else UpgradePath.Right))
        }

        event.editReplyCompat {
            addEmbedCompat {
                title(guide.title)
                author(guide.username,  "", "https://mistforge.net/customimg/${guide.userId}.png")
                thumbnail("https://mistforge.net/img/hero_builder/${guide.heroId}/hero.png")
                url("https://mistforge.net/build_viewer?build_id=${button.buildId}")
                description(CopyDown().convert(guide.guide))

                val previousUpgrades = mutableMapOf<SkillId, UpgradePath>()
                for ((i, pair) in upgrades.withIndex()) {
                    val (skillId, upgradePath) = pair

                    val previous = previousUpgrades[skillId]
                    val upgrade = hero.skills[skillId].upgrades.let { when {
                        previous != null -> it[previous, upgradePath]
                        else -> it[upgradePath]
                    } }

                    previousUpgrades[skillId] = upgradePath

                    val value = upgrade.name.en + "\n" + CopyDown().convert(upgrade.description.en)

                    addField("LEVEL ${i+1}", value, false)
                }
            }
        }.awaitSafe()
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent) {
        ButtonBuildDetails.parse(event.customId)?.also { handleInteraction(event, it); return }
    }
}