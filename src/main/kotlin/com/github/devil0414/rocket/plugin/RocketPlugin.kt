package com.github.devil0414.rocket.plugin

import com.github.devil0414.rocket.FileManager
import com.github.devil0414.rocket.Launch
import com.github.devil0414.rocket.RocketPluginScheduler
import com.github.devil0414.rocket.Rockets
import com.github.devil0414.rocket.command.KommandRocket
import com.github.devil0414.rocket.command.KommandRocket.register
import com.github.devil0414.rocket.task.RocketScheduler
import com.github.monun.kommand.kommand
import com.github.monun.tap.event.EntityEventManager
import com.github.monun.tap.fake.FakeEntity
import com.github.monun.tap.fake.FakeEntityServer
import org.bukkit.plugin.java.JavaPlugin

class RocketPlugin : JavaPlugin() {
    lateinit var fakeEntityServer: FakeEntityServer
        private set
    lateinit var entityEventManager : EntityEventManager
        private set
    override fun onEnable() {
        loadModules()
        registerPlayers()
        FileManager.initialize(this@RocketPlugin)
        val rockets : Rockets? = null
        rockets?.initialize()
        server.apply {
            scheduler.runTaskTimer(this@RocketPlugin, RocketPluginScheduler(), 0L, 1L)
        }
        kommand {
            register("rocket") {
                KommandRocket.register(this)
            }
        }
    }
    private fun loadModules() {
        fakeEntityServer = FakeEntityServer.create(this)
        entityEventManager = EntityEventManager(this)
    }
    private fun registerPlayers() {
        for(player in server.onlinePlayers) {
            fakeEntityServer.addPlayer(player)
        }
    }
    override fun onDisable() {
        FileManager.fakeEntityServer.clear()
        FileManager.rockets.values.forEach {
            it.save()
            it.launch?.run {
                it.stopLaunch()
            }
        }
    }
}