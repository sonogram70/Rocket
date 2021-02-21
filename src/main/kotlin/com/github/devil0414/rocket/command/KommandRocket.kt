package com.github.devil0414.rocket.command

import com.github.devil0414.rocket.FileManager
import com.github.devil0414.rocket.FileManager.rockets
import com.github.devil0414.rocket.RocketBlock
import com.github.devil0414.rocket.Rockets
import com.github.noonmaru.kommand.KommandBuilder
import com.github.noonmaru.kommand.KommandContext
import com.github.noonmaru.kommand.argument.KommandArgument
import com.github.noonmaru.kommand.argument.string
import com.github.noonmaru.kommand.sendFeedback
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.entity.Player
import com.github.devil0414.rocket.util.selection
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender

object KommandRocket {
    fun register(builder: KommandBuilder) {
        builder.apply {
            then("add") {
                then("name" to string()) {
                    require { this is Player}
                    executes {
                        create(it.sender as Player, it.parseArgument("name"))
                    }
                }
            }
            then("remove") {
                then("rocket" to RocketArgument) {
                    executes {
                        remove(it.sender, it.parseArgument("rocket"))
                    }
                }
            }
            then("launch") {
                then("rocket" to RocketArgument) {
                    require {
                        this is Player
                    }
                    executes {
                        start(it.sender, it.parseArgument("rocket"))
                    }
                }
            }
            then("clear") {
                executes {
                    FileManager.fakeEntityServer.clear()
                }
            }
            then("launchpad") {
                require {
                    this is Player
                }
                executes {
                    launchpad(it.sender as Player)
                }
            }
        }
    }
    private fun create(sender: Player, name: String) {
        sender.selection?.let { region ->
            if(region !is CuboidRegion) {
                sender.sendFeedback("지원하지 않는 구역입니다.")
            } else {
                FileManager.runCatching {
                    createRocket(name, region)
                }.onSuccess {
                    sender.sendFeedback("${it.name} 로켓을 생성했습니다.")
                }.onFailure {
                    sender.sendFeedback("$name 로켓 생성을 실패했습니다.")
                }
            }
        } ?: sender.sendFeedback("로켓을 생성하기 위해 worldedit의 wand로 구역을 지정해주세요.")
    }
    private fun remove(sender: CommandSender, rockets: Rockets) {
        rockets.remove()
        sender.sendFeedback("${rockets.name} 로켓을 제거했습니다.")
    }
    private fun start(sender: CommandSender, rockets: Rockets) {
        val rocket = rockets.startLaunch(locationlaunch)
    }
    private lateinit var locationlaunch : Location
    private fun launchpad(sender: Player) {
        val locationlaunchpad = Location(sender.world, sender.location.x, sender.location.y, sender.location.z)
        locationlaunch = locationlaunchpad
    }
}
object RocketArgument : KommandArgument<Rockets> {
    override fun parse(context: KommandContext, param: String): Rockets? {
        return FileManager.rockets[param]
    }

    override fun listSuggestion(context: KommandContext, target: String): Collection<String> {
        return FileManager.rockets.keys.filter { it.startsWith(target, true) }
    }
}