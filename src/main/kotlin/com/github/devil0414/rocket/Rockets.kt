package com.github.devil0414.rocket

import com.github.devil0414.rocket.plugin.RocketPlugin
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operation
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.Configuration
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import java.io.File

class Rockets {
    val name: String
    private lateinit var plugin: RocketPlugin
    val rocketFile : File
        get() = File(FileManager.rocketFolder, "$name.yml")
    private val file: File
    private var clipboard : Clipboard? = null
    val region: CuboidRegion
    private var valid = true
    var launch: Launch? = null
    constructor(name: String, region: CuboidRegion) {
        checkNotNull(region.world) { "Region must have region" }
        this.name = name
        this.region = region.clone()
        file = File(FileManager.rocketFolder, "$name.yml")
    }
    constructor(file: File) {
        name = file.name.removeSuffix(".yml")
        this.file = file
        YamlConfiguration.loadConfiguration(file).run {
            region = CuboidRegion(
                BukkitAdapter.adapt(Bukkit.getWorlds()[0]),
                getBlockVector3("min"),
                getBlockVector3("max")
            )
        }
    }
    internal fun initialize() {
        val launch: Launch? = null
        launch?.initialize(plugin)
    }
    fun startLaunch(location: Location) : Launch {
        checkState()
        copyRocket()
        val launch = Launch(this).apply {
            parseBlock(location)
        }
        this.launch = launch
        return launch
    }
    internal fun copyRocket() {
        this.clipboard = BlockArrayClipboard(region).apply {
            Operations.complete(
                ForwardExtentCopy(
                    WorldEdit.getInstance().editSessionFactory.getEditSession(
                        region.world,
                        -1
                    ), region, this, region.minimumPoint
                ).apply {
                    isCopyingEntities = true
                }.apply {
                    save()
                }
            )
        }
    }
    fun save() {
        checkState()
        val config = YamlConfiguration()
        region.let { region ->
            config.set("world", region.world!!.name)
            config.setPoint("min", region.minimumPoint)
            config.setPoint("max", region.maximumPoint)
        }
        file.parentFile.mkdirs()
        config.save(file)
    }
    fun stopLaunch() {
        checkState()
        launch.let { launch ->
            checkNotNull(launch)
            this.launch = null
            launch.destroy()
            val world = BukkitAdapter.asBukkitWorld(region.world).world
            val min = region.minimumPoint.run { world.getBlockAt(x, y, z) }
            val max = region.maximumPoint.run { world.getBlockAt(x, y, z) }
            val box = BoundingBox.of(min, max)
            world.getNearbyEntities(box) { it !is Player }.forEach { it.remove() }
        }
    }
    private fun Clipboard.paste() {
        val mp = region.minimumPoint

        WorldEdit.getInstance().editSessionFactory.getEditSession(region.world, -1).use { editSession ->
            val operation: Operation = ClipboardHolder(this)
                .createPaste(editSession)
                .to(BlockVector3.at(mp.x, mp.y, mp.z))
                .ignoreAirBlocks(false)
                .build()
            Operations.complete(operation)
        }
    }
    fun remove() {
        launch?.run { stopLaunch() }
        valid = false
        file.delete()
        FileManager.removeRocket(this)
    }
    private fun checkState() {
        require(valid) { "Invalid $this" }
    }
    private fun Configuration.getBlockVector3(path: String): BlockVector3 {
        return getConfigurationSection(path)!!.run {
            BlockVector3.at(
                getInt("x"),
                getInt("y"),
                getInt("x")
            )
        }
    }
    private fun ConfigurationSection.setPoint(path: String, point: BlockVector3) {
        createSection(path).apply {
            this["x"] = point.blockX
            this["y"] = point.blockY
            this["z"] = point.blockZ
        }
    }
}