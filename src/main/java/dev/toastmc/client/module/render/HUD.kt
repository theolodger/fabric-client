package dev.toastmc.client.module.render

import dev.toastmc.client.ToastClient.Companion.EVENT_BUS
import dev.toastmc.client.ToastClient.Companion.MODULE_MANAGER
import dev.toastmc.client.ToastClient.Companion.MODVER
import dev.toastmc.client.event.OverlayEvent
import dev.toastmc.client.event.PacketEvent
import dev.toastmc.client.module.Category
import dev.toastmc.client.module.Module
import dev.toastmc.client.module.ModuleManifest
import dev.toastmc.client.util.FabricReflect
import dev.toastmc.client.util.TwoDRenderUtils
import dev.toastmc.client.util.getRainbow
import dev.toastmc.client.util.mc
import io.github.fablabsmc.fablabs.api.fiber.v1.annotation.Setting
import me.zero.alpine.listener.EventHandler
import me.zero.alpine.listener.EventHook
import me.zero.alpine.listener.Listener
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt


@ModuleManifest(
        label = "HUD",
        category = Category.RENDER,
        persistent = true
)
class HUD : Module() {
    @Setting(name = "Arraylist") var arraylist = true
    @Setting(name = "Watermark") var watermark = true
    @Setting(name = "Coords") var coords = true
    @Setting(name = "TPS") var tps = false
    @Setting(name = "FPS") var fps = false
    @Setting(name = "Ping") var ping = false
    @Setting(name = "LagNotifier") var lagNotifier = true
    @Setting(name = "Armour") var armour = true

    var infoList: MutableList<String> = ArrayList()
    var lines: MutableList<String> = ArrayList()
    var lastPacket = 0L
    var tpsNum = 20.0
    var prevTime = 0
//    var color = Color(0, 255, 0, 155)

//    var prefixPresent = false

    @EventHandler
    private val onOverlayEvent = Listener(EventHook<OverlayEvent> {
        if (mc.player == null) return@EventHook
//        if (mc.currentScreen is ChatScreen && prefixPresent) {
////            drawHollowRect(it.matrix, 3, mc.currentScreen!!.height - 16, mc.currentScreen!!.width - 8, 10, 1, Color(0, 255, 0, 155).rgb)
//            drawHollowRect(it.matrix, 2, mc.currentScreen!!.height - 14, mc.currentScreen!!.width - 4, 11, 1, color.rgb)
//        }
        infoList.clear()
        lines.clear()
        var arrayCount = 0
        if (watermark && !mc.options.debugEnabled) lines.add(0, "Toast Client $MODVER")
        if (arraylist && !mc.options.debugEnabled) {
            for (m in MODULE_MANAGER.modules) if (m.enabled && !m.hidden) lines.add(m.label)
//            lines.sortWith { a: String?, b: String? ->
//                mc.textRenderer.getWidth(b).compareTo(mc.textRenderer.getWidth(a))
//            }
            lines.sortedDescending().reversed()
//            lines.sortBy { str ->
//                mc.textRenderer.getWidth(str).compareTo(mc.textRenderer.getWidth(str))
//            }
            val color: Int = getRainbow(1f, 1f, 10.0, 0).rgb
            for (s in lines) {
                TwoDRenderUtils.drawText(it.matrix, s, 5, 5 + (arrayCount * 10), color)
                arrayCount++
            }
        }
        if (coords) {
            val direction = when (mc.player!!.horizontalFacing) {
                Direction.NORTH -> "-Z"
                Direction.SOUTH -> "+Z"
                Direction.WEST -> "-X"
                Direction.EAST -> "+X"
                else -> ""
            }
            val direction2 = mc.player!!.horizontalFacing.toString().capitalize()
            val nether = mc.world!!.registryKey.value.path.contains("nether")
            val pos = mc.player!!.blockPos
            val vec: Vec3d = mc.player!!.pos
            val pos2: BlockPos = if (nether) BlockPos(vec.getX() * 8, vec.getY(), vec.getZ() * 8) else BlockPos(vec.getX() / 8, vec.getY(), vec.getZ() / 8)

            infoList.add("[ $direction | $direction2 ] " + (if (nether) "\u00a7c" else "\u00a7a") + pos.x + " " + pos.y + " " + pos.z + " \u00a77[" + (if (nether) "\u00a7a" else "\u00a7c") + pos2.x + " " + pos2.y + " " + pos2.z + "\u00a77]")
        }
        if (tps) {
            var suffix = "\u00a77"
            if (lastPacket + 7500 < System.currentTimeMillis()) suffix += "...." else if (lastPacket + 5000 < System.currentTimeMillis()) suffix += "..." else if (lastPacket + 2500 < System.currentTimeMillis()) suffix += ".." else if (lastPacket + 1200 < System.currentTimeMillis()) suffix += "."
            infoList.add("TPS: " + getColorString(tpsNum.toInt(), 18, 15, 12, 8, 4, false) + tpsNum.toInt() + suffix)
        }
        if (fps) {
            val fps = FabricReflect.getFieldValue(MinecraftClient.getInstance(), "field_1738", "currentFps") as Int
            infoList.add("FPS: " + getColorString(fps, 120, 60, 30, 15, 10, false) + fps)
        }
        if (ping) {
            val playerEntry = mc.player!!.networkHandler.getPlayerListEntry(mc.player!!.gameProfile.id)
            val ping = playerEntry?.latency ?: 0
            infoList.add("Ping: " + getColorString(ping, 10, 50, 100, 300, 600, true) + ping)
        }
        if (lagNotifier) {
            val time = System.currentTimeMillis()
            if (time - lastPacket > 500) {
                val text = "The server has been lagging for " + (time - lastPacket) / 1000.0 + "s"
                TwoDRenderUtils.drawText(it.matrix, text, mc.window.scaledWidth / 2 - mc.textRenderer.getWidth(text) / 2, Math.min((time - lastPacket - 500) / 20 - 20, 10).toInt(), 0xd0d0d0)
            }
        }
        if (armour && !mc.player?.isCreative!! && !mc.player?.isSpectator!!) {
            GL11.glPushMatrix()
            var count = 0
            val x1 = mc.window.scaledWidth / 2
            val y = mc.window.scaledHeight - if (mc.player!!.isSubmergedInWater || mc.player!!.air < mc.player!!.maxAir) 64 else 55
            for (`is` in mc.player!!.inventory.armor) {
                count++
                if (`is`.isEmpty) continue
                val x = x1 - 90 + (9 - count) * 20 + 2
                GL11.glEnable(GL11.GL_DEPTH_TEST)
                mc.itemRenderer.zOffset = 200f
                mc.itemRenderer.renderGuiItemIcon(`is`, x, y)
                mc.itemRenderer.renderGuiItemOverlay(mc.textRenderer, `is`, x, y)
                mc.itemRenderer.zOffset = 0f
                GL11.glDisable(GL11.GL_DEPTH_TEST)
            }
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glPopMatrix()
        }
        for ((i, s) in infoList.withIndex()) {
            TwoDRenderUtils.drawText(it.matrix, s, 10, mc.window.scaledHeight - 20 - (i * 10), 0xa0a0a0)
        }
    })

    @EventHandler
    private val packetEventListener = Listener(EventHook<PacketEvent.Receive> {
        if(mc.player == null) return@EventHook
        lastPacket = System.currentTimeMillis()
        if (it.packet is WorldTimeUpdateS2CPacket) {
            val time = System.currentTimeMillis()
            if (time < 500) return@EventHook
            val timeOffset: Long = abs(1000 - (time - prevTime)) + 1000
            tpsNum = (MathHelper.clamp(20 / (timeOffset.toDouble() / 1000), 0.0, 20.0) * 100.0).roundToInt() / 100.0
            prevTime = time.toInt()
        }
    })

    override fun onEnable() {
        EVENT_BUS.subscribe(onOverlayEvent)
        EVENT_BUS.subscribe(packetEventListener)
    }

    override fun onDisable() {
        EVENT_BUS.unsubscribe(onOverlayEvent)
        EVENT_BUS.unsubscribe(packetEventListener)
    }

    private fun getColorString(value: Int, best: Int, good: Int, mid: Int, bad: Int, worst: Int, rev: Boolean): String? {
        return if (if (!rev) value > best else value < best) "\u00a72" else if (if (!rev) value > good else value < good) "\u00a7a" else if (if (!rev) value > mid else value < mid) "\u00a7e" else if (if (!rev) value > bad else value < bad) "\u00a76" else if (if (!rev) value > worst else value < worst) "\u00a7c" else "\u00a74"
    }
}