package com.github.devil0414.rocket

import com.github.devil0414.rocket.plugin.RocketPlugin
import com.github.devil0414.rocket.task.RocketScheduler
import com.github.devil0414.rocket.task.RocketTask
import com.github.noonmaru.tap.fake.FakeEntity
import com.github.noonmaru.tap.fake.FakeProjectileManager
import com.google.common.collect.ImmutableMap
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.regions.Region
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Entity
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class Launch(val rockets: Rockets) {
    lateinit var plugin: RocketPlugin
        private set
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
    private lateinit var projectileManager: FakeProjectileManager
    private var scheduler = RocketScheduler()
    lateinit var dataMap: Map<RocketBlock, Set<RocketBlockData>>
    lateinit var dataByBlock: Map<Block, RocketBlockData>
    private lateinit var fakeEntities: MutableSet<FakeEntity>
    internal fun initialize(plugin: RocketPlugin) {
        this.plugin = plugin
        projectileManager = FakeProjectileManager()
        fakeEntities = Collections.newSetFromMap(WeakHashMap<FakeEntity, Boolean>())
    }
    private fun onEnable() {
        isEnabled = true
        projectileManager = FakeProjectileManager()
        scheduler = RocketScheduler()
        fakeEntities = Collections.newSetFromMap(WeakHashMap<FakeEntity, Boolean>())
    }
    private fun onDisable() {
        scheduler.cancelAll()
        projectileManager.clear()
        fakeEntities.run {
            for(fakeEntity in this) {
                fakeEntity.remove()
            }
            clear()
        }
    }
    internal fun parseBlock(location: Location) {
        checkState()
        val dataMap = HashMap<RocketBlock, HashSet<RocketBlockData>>()
        val dataByBlock = HashMap<Block, RocketBlockData>()
        rockets.region.forEachBlocks { block ->
            RocketBlocks.getBlock(block)?.let { rocketBlock ->
                val location2= Location(block.world, location.x, location.y, location.z)
                val data = rocketBlock.createBlockData(block).apply {
                    onInitialize(this@Launch, location = location2)
                }
                dataMap.computeIfAbsent(rocketBlock) { HashSet() } += data
                dataByBlock[block] = data
            }
        }
        this.dataMap = ImmutableMap.copyOf(dataMap)
        this.dataByBlock = ImmutableMap.copyOf(dataByBlock)
    }
    private fun checkState() {
        check(this.valid) { "Invalid $this" }
    }
    private fun Region.forEachBlocks(action: (Block) -> Unit) {
        val world = BukkitAdapter.asBukkitWorld(world).world
        forEach {
            action.invoke(world.getBlockAt(it.x, it.y, it.z))
        }
    }
    fun launchProjectile(location: Location, projectile: RocketProjectile) {
        projectile.rocket = this

        projectileManager.launch(location, projectile)
    }
    fun spawnFakeEntity(location: Location, entityClass: Class<out Entity>): FakeEntity {
        checkState()
        val fakeEntity = FileManager.fakeEntityServer.spawnEntity(location, entityClass)
        fakeEntities.add(fakeEntity)
        return fakeEntity
    }
    fun spawnFallingBlock(location: Location, blockData: BlockData): FakeEntity {
        checkState()
        val fakeEntity = FileManager.fakeEntityServer.spawnFallingBlock(location, blockData)
        fakeEntities.add(fakeEntity)
        return fakeEntity
    }
    internal fun update() {
        scheduler.run()
        projectileManager.update()
    }
    internal fun runTaskTimer(runnable: Runnable, delay: Long, period: Long): RocketTask {
        checkState()

        return scheduler.runTaskTimer(runnable, delay, period)
    }
    internal fun destroy() {
        checkState()

        scheduler.cancelAll()

        valid = false

        dataByBlock.values.forEach {
            it.destroy()
        }
    }
}