package com.github.devil0414.rocket

import com.github.devil0414.rocket.task.RocketTask
import com.github.noonmaru.tap.fake.*
import com.github.noonmaru.tap.math.copy
import com.github.noonmaru.tap.math.normalizeAndLength
import com.google.common.collect.ImmutableList
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Shulker
import kotlin.math.min
import com.github.noonmaru.tap.trail.trail
import org.bukkit.FluidCollisionMode
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import kotlin.random.Random
import kotlin.random.Random.Default.nextFloat

object RocketBlocks {
    val Hopper = Fire()
    val BLOCK = OtherBlock()
    val Endrod = Noshulker()
    val list = ImmutableList.of(
        Hopper,
        BLOCK
    )
    fun getBlock(block: Block) : RocketBlock? {
        val state = block.state
        val data = block.blockData
        val type = data.material
        if(type != Material.AIR) {
            if(type == Material.HOPPER) {
                return Hopper
            } else if(type == Material.END_ROD) {
                return Endrod
            } else if(type == Material.GLASS || type == Material.GLASS_PANE || type == Material.BLACK_STAINED_GLASS || type == Material.BLUE_STAINED_GLASS || type == Material.BROWN_STAINED_GLASS || type == Material.CYAN_STAINED_GLASS || type == Material.GRAY_STAINED_GLASS) {
                return Endrod
            }
            else {
                return BLOCK
            }
        }
        return null
    }
}
abstract class RocketBlock {
    fun createBlockData(block: Block) : RocketBlockData {
        return newBlockData(block).apply {
            this.block = block
            this.rockerBlock = this@RocketBlock
        }
    }
    protected abstract fun newBlockData(block: Block): RocketBlockData
}
abstract class RocketBlockData {
    lateinit var block: Block
    lateinit var rockerBlock: RocketBlock
        internal set
    open fun onInitialize(launch: Launch, location: Location) {}
    open fun destroy() {}
}
class Fire : RocketBlock() {
    override fun newBlockData(block: Block): RocketBlockData {
        return FireData()
    }
    class FireData : RocketBlockData(), Runnable {
        private lateinit var stand: FakeEntity
        private lateinit var standBlock : FakeEntity
        private lateinit var task: RocketTask
        private lateinit var shulker: FakeEntity
        private var rising = false
        private var risingSpeed = 0.05
        override fun onInitialize(launch: Launch, location: Location) {
            val loc = location
            val fakeServer = FileManager.fakeEntityServer
            val rMx = launch.rockets.region.maximumPoint.x
            val rmx = launch.rockets.region.minimumPoint.x
            val ry = launch.rockets.region.minimumY
            val rMz = launch.rockets.region.maximumPoint.z
            val rmz = launch.rockets.region.minimumPoint.z
            standBlock = fakeServer.spawnFallingBlock(loc, block.blockData)
            stand = fakeServer.spawnEntity(loc, ArmorStand::class.java)
            shulker = fakeServer.spawnEntity(loc, Shulker::class.java)
            shulker.updateMetadata<Shulker> {
                invisible = true
                setAI(false)
            }
            stand.updateMetadata<ArmorStand> {
                isMarker = true
                invisible = true
            }
            stand.addPassenger(standBlock)
            stand.addPassenger(shulker)
            val x = block.location.x
            val y = block.location.y
            val z = block.location.z
            val location = Location(block.world, x - (rMx + rmx) / 2 + loc.x, y - ry + loc.y, z - (rMz + rmz) / 2 + loc.z)
            stand.moveTo(location)

            rising = true
            task = launch.runTaskTimer(this, 0L, 1L)
        }
        override fun run() {
            risingSpeed = min(0.06, risingSpeed + 0.01)
            stand.move(0.0, risingSpeed, 0.0)
            val loc = stand.location.subtract(0.0, 1.0, 0.0)
            val locstand = stand.location
            if(loc.y >= 100) {
                task.cancel()
                standBlock.remove()
                stand.remove()
                shulker.remove()
            }
            stand.location.run {
                var dx = 0
                var dy = -0.5
                var dz = 0
                var wiggle = 0.4
                repeat(10) {
                    for (i in 0 until 10) {
                        world.spawnParticle(
                            Particle.FLAME,
                            loc,
                            0,
                            dx + nextFloat() * wiggle - wiggle / 2.0,
                            dy + nextFloat() * wiggle - wiggle / 2.0,
                            dz + nextFloat() * wiggle - wiggle / 2.0,
                            1.0,
                            null,
                            true
                        )
                    }
                }
            }
        }
        override fun destroy() {
            stand.remove()
            shulker.remove()
            standBlock.remove()
        }
        }
    }
class OtherBlock : RocketBlock() {
    override fun newBlockData(block: Block): RocketBlockData {
        return OtherBlockData()
    }
    class OtherBlockData : RocketBlockData(), Runnable {
        private lateinit var stand: FakeEntity
        private lateinit var standBlock: FakeEntity
        private lateinit var shulker: FakeEntity
        private lateinit var player: MutableList<Entity>
        private var rising = false
        private lateinit var task: RocketTask
        private var risingSpeed = 0.05
        override fun onInitialize(launch: Launch, location: Location) {
            val loc = location
            val fakeServer = FileManager.fakeEntityServer
            val rMx = launch.rockets.region.maximumPoint.x
            val rmx = launch.rockets.region.minimumPoint.x
            val ry = launch.rockets.region.minimumY
            val rMz = launch.rockets.region.maximumPoint.z
            val rmz = launch.rockets.region.minimumPoint.z
            standBlock = fakeServer.spawnFallingBlock(loc, block.blockData)
            stand = fakeServer.spawnEntity(loc, ArmorStand::class.java)
            player = stand.bukkitEntity.getNearbyEntities(stand.location.x, stand.location.y + 1.0, stand.location.z)
            for(player in player) {
                val location = Location(player.world, player.location.x, player.location.y + 0.05, player.location.z)
                player.teleport(location)
            }
            shulker = fakeServer.spawnEntity(loc, Shulker::class.java)
            shulker.updateMetadata<Shulker> {
                invisible = true
                setAI(false)
            }
            stand.updateMetadata<ArmorStand> {
                isMarker = true
                invisible = true
            }
            stand.addPassenger(shulker)
            stand.addPassenger(standBlock)
            val x = block.location.x
            val y = block.location.y
            val z = block.location.z
            val location = Location(block.world, x - (rMx + rmx) / 2 + loc.x, y - ry + loc.y, z - (rMz + rmz) / 2 + loc.z)
            stand.moveTo(location)
            rising = true
            if(stand.location.y >= 100) {
                launch.rockets.stopLaunch()
            }
            task = launch.runTaskTimer(this, 0L, 1L)
        }
        override fun run() {
            risingSpeed = min(0.06, risingSpeed + 0.01)
            stand.move(0.0, risingSpeed, 0.0)
            val loc = stand.location.subtract(0.0, 1.0, 0.0)
            if(loc.y >= 100) {
                task.cancel()
                standBlock.remove()
                stand.remove()
                shulker.remove()
            }

        }

        override fun destroy() {
            stand.remove()
            shulker.remove()
            standBlock.remove()
        }
    }
}
class Noshulker : RocketBlock() {
    override fun newBlockData(block: Block): RocketBlockData {
        return NoshulkerData()
    }
    class NoshulkerData : RocketBlockData(), Runnable {
        private lateinit var stand: FakeEntity
        private lateinit var standBlock: FakeEntity
        private var rising = false
        private lateinit var task: RocketTask
        private var risingSpeed = 0.05
        override fun onInitialize(launch: Launch, location: Location) {
            val loc = location
            val fakeServer = FileManager.fakeEntityServer
            val rMx = launch.rockets.region.maximumPoint.x
            val rmx = launch.rockets.region.minimumPoint.x
            val ry = launch.rockets.region.minimumY
            val rMz = launch.rockets.region.maximumPoint.z
            val rmz = launch.rockets.region.minimumPoint.z
            standBlock = fakeServer.spawnFallingBlock(loc, block.blockData)
            stand = fakeServer.spawnEntity(loc, ArmorStand::class.java)
            stand.updateMetadata<ArmorStand> {
                isMarker = true
                invisible = true
            }
            stand.addPassenger(standBlock)
            val x = block.location.x
            val y = block.location.y
            val z = block.location.z
            val location = Location(block.world, x - (rMx + rmx) / 2 + loc.x, y - ry + loc.y, z - (rMz + rmz) / 2 + loc.z)
            stand.moveTo(location)
            rising = true
            if(stand.location.y >= 100) {
                launch.rockets.stopLaunch()
            }
            task = launch.runTaskTimer(this, 0L, 1L)
        }
        override fun run() {
            risingSpeed = min(0.06, risingSpeed + 0.01)
            stand.move(0.0, risingSpeed, 0.0)
            val loc = stand.location.subtract(0.0, 1.0, 0.0)
            if(loc.y >= 100) {
                task.cancel()
                standBlock.remove()
                stand.remove()
            }

        }

        override fun destroy() {
            stand.remove()
            standBlock.remove()
        }
    }
}