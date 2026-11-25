package dev.bombowy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KillEffects {
    public static final Identifier SHARK_TEXTURE = Identifier.of("bombowyclient", "textures/shark.png");
    public static final Identifier BOMB_TEXTURE = Identifier.of("bombowyclient", "textures/bomb.png");

    public enum EffectType {
        SHARK("Rekin", "shark"),
        BOMB("Bomba", "bomb"),
        LIGHTNING("Piorun", "lightning");

        public final String displayName;
        public final String id;

        EffectType(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }

        public static EffectType fromId(String id) {
            for (EffectType t : values()) {
                if (t.id.equalsIgnoreCase(id)) return t;
            }
            return SHARK;
        }
    }

    public static class ActiveEffect {
        public final EffectType type;
        public final String targetName;
        public final double x;
        public final double y;
        public final double z;
        public int ticksRemaining;
        public final int maxTicks;

        public ActiveEffect(EffectType type, String targetName, double x, double y, double z, int durationTicks) {
            this.type = type;
            this.targetName = targetName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.ticksRemaining = durationTicks;
            this.maxTicks = durationTicks;
        }
    }

    private static final List<ActiveEffect> ACTIVE = new ArrayList<>();

    public static void trigger(String targetName, double x, double y, double z) {
        BombowyConfig cfg = BombowyClient.CONFIG;
        if (!cfg.killEffectsEnabled) return;
        EffectType type = EffectType.fromId(cfg.selectedKillEffect);
        ACTIVE.add(new ActiveEffect(type, targetName, x, y, z, 50));

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            mc.world.playSound(x, y, z, SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.PLAYERS, 1.0f, 1.15f, false);
            for (int i = 0; i < 18; i++) {
                double ox = (mc.world.random.nextDouble() - 0.5) * 1.4;
                double oy = mc.world.random.nextDouble() * 1.6;
                double oz = (mc.world.random.nextDouble() - 0.5) * 1.4;
                mc.world.addParticle(ParticleTypes.BUBBLE_POP, x + ox, y + oy, z + oz, 0, 0.08, 0);
            }
        }
    }

    public static void triggerTest() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (BombowyClient.CONFIG.closeGuiOnKillTest && mc.currentScreen != null) {
            mc.setScreen(null);
        }
        String name = mc.player != null ? mc.player.getName().getString() : "Gracz";
        double x = mc.player != null ? mc.player.getX() : 0.0;
        double y = mc.player != null ? mc.player.getY() : 64.0;
        double z = mc.player != null ? mc.player.getZ() : 0.0;
        trigger(name, x, y, z);
    }

    public static void tick() {
        Iterator<ActiveEffect> it = ACTIVE.iterator();
        while (it.hasNext()) {
            ActiveEffect eff = it.next();
            eff.ticksRemaining--;
            if (eff.ticksRemaining <= 0) {
                it.remove();
            }
        }
    }

    public static void renderHudOverlay(DrawContext ctx, float tickDelta) {
        if (ACTIVE.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        for (ActiveEffect eff : ACTIVE) {
            float progress = 1.0f - (eff.ticksRemaining - tickDelta) / (float) eff.maxTicks;
            progress = Math.max(0.0f, Math.min(1.0f, progress));
            float alpha = progress < 0.75f ? 1.0f : Math.max(0.0f, (1.0f - progress) / 0.25f);
            int a = (int) (alpha * 255);
            if (a <= 5) continue;

            int cx = w / 2;
            int cy = (int) (h * 0.42f - progress * 22.0f);

            MatrixStack matrices = ctx.getMatrices();
            matrices.push();
            matrices.translate(cx, cy, 0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.sin(progress * Math.PI * 2.0) * 6.0f));

            Identifier icon = eff.type == EffectType.BOMB ? BOMB_TEXTURE : SHARK_TEXTURE;
            ctx.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, icon, -16, -24, 0, 0, 32, 32, 32, 32);
            ctx.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            Text label = Text.literal(eff.type.displayName + " §7• §f" + eff.targetName).formatted(Formatting.BOLD);
            int tw = mc.textRenderer.getWidth(label);
            ctx.drawTextWithShadow(mc.textRenderer, label, -tw / 2, 12, (a << 24) | 0x7FD8FF);
            matrices.pop();
        }
    }
}
