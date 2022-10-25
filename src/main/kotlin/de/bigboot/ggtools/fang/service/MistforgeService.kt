package de.bigboot.ggtools.fang.service

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.ScaleMethod
import com.sksamuel.scrimage.nio.PngWriter
import de.bigboot.ggtools.fang.api.mistforge.MistforgeApi
import de.bigboot.ggtools.fang.api.mistforge.model.Hero
import de.bigboot.ggtools.fang.api.mistforge.model.Guide
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
import okhttp3.HttpUrl
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
        const val ID_PREFIX = "MISTFORGE"
    }
}

data class ButtonBuildDetailsId(val buildId: String): MistforgeComponentSpec {
    override fun id() = "$PREFIX:${buildId}"
    override fun component(): ActionComponent = Button.secondary(id(), "Details")

    companion object {
        private val PREFIX = "${MistforgeComponentSpec.ID_PREFIX}:BUTTON:GUIDE_DETAILS_ID"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (guideId) -> ButtonBuildDetailsId(guideId) }
    }
}

data class ButtonBuildDetailsParams(val heroId: Int, val skills: String, val summons: String, val talent: Int): MistforgeComponentSpec {
    override fun id() = "$PREFIX:${heroId}:${skills}:${summons}:${talent}"
    override fun component(): ActionComponent = Button.secondary(id(), "Details")

    companion object {
        private val PREFIX = "${MistforgeComponentSpec.ID_PREFIX}:BUTTON:GUIDE_DETAILS_PARAMS"
        private val ID_REGEX = Regex("$PREFIX:([^:]+):([^:]+):([^:]+):([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (hero_id, skills, summons, talent) -> ButtonBuildDetailsParams(hero_id.toInt(), skills, summons, talent.toInt()) }
    }
}

@OptIn(FlowPreview::class)
class MistforgeService: AutostartService, KoinComponent {
    companion object {
        private val BUILD_URL_REGEX = Regex("""https://mistforge.net/build_viewer\?([^ ]+)""")
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

    private suspend fun handleMessage(event: MessageCreateEvent, params: String) {
        var guide: Guide? = null
        var url = HttpUrl.parse("https://mistforge.net/build_viewer?${params}") ?: return
        if (url.queryParameter("build_id") == null && 
                (url.queryParameter("hero_id") == null ||
                 url.queryParameter("skills") == null ||
                 url.queryParameter("summons") == null ||
                 url.queryParameter("talent") == null)
            ) return
        
        if (url.queryParameter("build_id") != null) {
            guide = mistforge.getGuide(url.queryParameter("build_id")!!.toInt())
        }

        var img = background.copy()

        val heroId = when (guide) {
            null -> url.queryParameter("hero_id")!!.toInt()
            else -> guide.heroId
        }

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
                .fromBytes(mistforge.getHeroImage(heroId, "${name}.png").bytes())
                .scaleTo(96, 96, ScaleMethod.Bicubic)
            img = img.overlay(icon, x, y)
        }
        
        val talentId = when (guide) {
            null -> url.queryParameter("talent")!!.toInt()
            else -> guide.talent
        }
        
        val talent = ImmutableImage.loader()
            .fromBytes(mistforge.getHeroImage(heroId, "${talentId+5}.png").bytes())
            .scaleTo(96, 96, ScaleMethod.Bicubic)

        img = img.overlay(talent, 1117,262)

        val skills: String = when (guide) {
            null -> url.queryParameter("skills")!!
            else -> guide.skills
        }

        if (!skills.matches(Regex("^[a-j]{10}$"))) return

        val summons: String = when (guide) {
            null -> url.queryParameter("summons")!!
            else -> guide.creatures
        }

        if (!summons.matches(Regex("^[0a-i]{3}$"))) return

        for ((i, c) in skills.withIndex())
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
                if (guide != null) {
                    title(guide.title)
                    author(guide.username,  "", "https://mistforge.net/customimg/${guide.userId}.png")
                    url("https://mistforge.net/build_viewer?${params}")
                    description(CopyDown().convert(guide.guide).take(150).plus("..."))
                    
                    addComponent(ActionRow.of(ButtonBuildDetailsId(url.queryParameter("build_id")!!).component()))
                }
                else {
                    addComponent(ActionRow.of(ButtonBuildDetailsParams(heroId, skills, url.queryParameter("summons")!!, talentId).component()))
                }
                thumbnail("https://mistforge.net/img/hero_builder/${heroId}/hero.png")
                image("attachment://build.png")
            }


            addFile("build.png", img.bytes(PngWriter.MinCompression).inputStream())
        }.await()
    }
    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonBuildDetailsId) {
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
                description(CopyDown().convert(guide.guide))
                thumbnail("https://mistforge.net/img/hero_builder/${guide.heroId}/hero.png")
                url("https://mistforge.net/build_viewer?build_id=${button.buildId}")

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

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonBuildDetailsParams) {
        event.deferReply().withEphemeral(true).awaitSafe()

        val hero = heroes.first { it.heroId == button.heroId }

        val upgrades = mutableListOf<Pair<SkillId, UpgradePath>>()

        for (c in button.skills)
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
                thumbnail("https://mistforge.net/img/hero_builder/${button.heroId}/hero.png")
                url("https://mistforge.net/build_viewer?hero_id=${button.heroId}&skills=${button.skills}&summons=${button.summons}&talent=${button.talent}")

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
        ButtonBuildDetailsId.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonBuildDetailsParams.parse(event.customId)?.also { handleInteraction(event, it); return }
    }
}
