package com.github.devil0414.rocket

import com.github.devil0414.rocket.util.toBoundingBox
import com.github.noonmaru.tap.fake.FakeEntity
import com.github.noonmaru.tap.fake.FakeEntityServer
import com.github.noonmaru.tap.fake.FakeProjectileManager
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.BoundingBox
import java.io.File
import java.util.*

object FileManager {
    lateinit var rocketFolder: File
        private set
    private lateinit var _rockets: MutableMap<String, Rockets>
    val rockets: Map<String, Rockets>
        get() = _rockets
    lateinit var fakeEntityServer: FakeEntityServer
    lateinit var fakeEntities: MutableSet<FakeEntity>
    internal fun initialize(plugin: JavaPlugin) {
        plugin.dataFolder.let { dir->
            rocketFolder = File(dir, "rockets")
        }
        _rockets = TreeMap<String, Rockets>(String.CASE_INSENSITIVE_ORDER).apply {
            rocketFolder.let { dir->
                if(dir.exists()) {
                    rocketFolder.listFiles { _, name -> name.endsWith(".yml")}?.forEach { file ->
                        val rocket = Rockets(file)
                        put(rocket.name, rocket)
                    }
                }
            }
        }
        fakeEntityServer = FakeEntityServer.create(plugin).apply {
            plugin.server.pluginManager.registerEvents(object : Listener {
                @EventHandler
                fun onJoin(event: PlayerJoinEvent) {
                    addPlayer(event.player)
                }
            }, plugin)
            plugin.server.scheduler.runTaskTimer(plugin, this::update, 0L, 1L)
            for(onlinePlayer in Bukkit.getOnlinePlayers()) {
                addPlayer(onlinePlayer)
            }
        }
    }
    var isEnabled = false
        set(value) {
            checkState()
            if(field != value) {
                field = value
                if(value) {
                    onEnable()
                } else {
                    onDisable()
                }
            }
        }
    var valid = true
        private set
    private fun onEnable() {
        isEnabled = true
    }
    private fun onDisable() {
        fakeEntities.run {
            for(fakeEntity in this) {
                fakeEntity.remove()
            }
            clear()
        }
    }
    fun checkState() {
        require(valid) { "Invalid Rocket"}
    }
    fun checkEnabled() {
        require(isEnabled) { "Disabled Rocket"}
    }
    fun createRocket(name: String, region: CuboidRegion) : Rockets {
        _rockets.apply {
            require(name !in this) { "Name is already in use" }
            getOverlappedRockets(region.toBoundingBox()).let { overlaps ->
                require(overlaps.isEmpty()) { "Region overlaps with other rockets ${overlaps.joinToString { it.name }}"}
            }
            return Rockets(name, region).apply {
                copyRocket()
                save()
                _rockets[name] = this
            }
        }
    }
    internal fun removeRocket(rockets: Rockets) {
        _rockets.remove(rockets.name)
    }
    fun getOverlappedRockets(box: BoundingBox): List<Rockets> {
        return rockets.values.filter { box.overlaps(it.region.toBoundingBox()) }
    }
}