package com.artm_.auracraft.client;

import com.artm_.auracraft.payload.ChooseEffectPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class EffectSelectionScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 248;
    private static final int HEADER_HEIGHT = 24;
    private static final int MAX_VISIBLE_EFFECTS = 10;
    private static final int LIST_ROW_SPACING = 18;
    private static final int LIST_BUTTON_HEIGHT = 16;

    private final List<String> effects = new ArrayList<>();
    private final List<Button> effectButtons = new ArrayList<>();
    private final List<String> visibleEffectIds = new ArrayList<>();
    private String selectedEffectId;

    private int panelX;
    private int panelY;
    private int leftWidth;
    private int rightX;
    private int rightWidth;
    private int scrollOffset;
    private Button chooseButton;

    public EffectSelectionScreen() {
        super(Component.translatable("screen.auracraft.title"));
    }

    @Override
    protected void init() {
        super.init();

        this.effects.clear();
        this.effects.addAll(EffectSmpClientMod.getEnabledEffectsForPicker());
        if (this.effects.isEmpty()) {
            this.selectedEffectId = null;
        } else if (this.selectedEffectId == null || !this.effects.contains(this.selectedEffectId)) {
            this.selectedEffectId = this.effects.getFirst();
        }

        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;
        this.leftWidth = 132;
        this.rightX = this.panelX + this.leftWidth + 8;
        this.rightWidth = PANEL_WIDTH - this.leftWidth - 8;
        int contentTop = this.panelY + HEADER_HEIGHT + 6;

        this.effectButtons.clear();
        this.visibleEffectIds.clear();
        for (int i = 0; i < MAX_VISIBLE_EFFECTS; i++) {
            this.visibleEffectIds.add(null);
            final int row = i;
            Button button = Button.builder(Component.empty(), b -> {
                String effectId = this.visibleEffectIds.get(row);
                if (effectId != null) {
                    setSelectedEffect(effectId);
                }
            }).bounds(this.panelX + 8, contentTop + 30 + (i * LIST_ROW_SPACING), this.leftWidth - 22, LIST_BUTTON_HEIGHT).build();
            this.effectButtons.add(this.addRenderableWidget(button));
        }

        this.chooseButton = this.addRenderableWidget(
            Button.builder(Component.translatable("screen.auracraft.choose_button"), b -> chooseSelectedEffect())
                .bounds(this.rightX + 12, this.panelY + PANEL_HEIGHT - 30, this.rightWidth - 24, 20)
                .build()
        );

        refreshButtonLabels();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractBackground(graphics, mouseX, mouseY, deltaTicks);
        int contentTop = this.panelY + HEADER_HEIGHT;

        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + PANEL_HEIGHT, 0xCC202020);
        graphics.outline(this.panelX, this.panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF4A4A4A);
        graphics.fill(this.panelX + 1, this.panelY + 1, this.panelX + PANEL_WIDTH - 1, this.panelY + HEADER_HEIGHT - 1, 0xCC1A1A1A);
        graphics.horizontalLine(this.panelX + 1, this.panelX + PANEL_WIDTH - 2, contentTop, 0xFF5A5A5A);
        graphics.fill(this.panelX + this.leftWidth, contentTop, this.panelX + this.leftWidth + 1, this.panelY + PANEL_HEIGHT, 0xFF5A5A5A);

        int trackX = this.panelX + this.leftWidth - 12;
        int trackTop = contentTop + 30;
        int trackHeight = ((MAX_VISIBLE_EFFECTS - 1) * LIST_ROW_SPACING) + LIST_BUTTON_HEIGHT;
        graphics.fill(trackX, trackTop, trackX + 6, trackTop + trackHeight, 0xFF3A3A3A);

        int maxOffset = maxScrollOffset();
        int thumbHeight = maxOffset == 0 ? trackHeight : Math.max(16, trackHeight / 3);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackTop + (maxOffset == 0 ? 0 : (this.scrollOffset * thumbTravel) / maxOffset);
        graphics.fill(trackX, thumbY, trackX + 6, thumbY + thumbHeight, 0xFFB8B8B8);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
        int contentTop = this.panelY + HEADER_HEIGHT;

        graphics.centeredText(this.font, this.title, this.panelX + (PANEL_WIDTH / 2), this.panelY + 7, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("screen.auracraft.left_header"), this.panelX + 30, contentTop + 6, 0xFFCFCFCF);

        if (this.selectedEffectId == null) {
            graphics.text(this.font, Component.literal("No effects enabled in config."), this.rightX + 14, contentTop + 16, 0xFFFF8080);
            return;
        }

        graphics.text(this.font, EffectSmpClientMod.effectName(this.selectedEffectId), this.rightX + 14, contentTop + 6, 0xFFFFFFFF);

        Identifier icon = getEffectTexture(this.selectedEffectId);
        int iconSize = 72;
        float scale = iconSize / 18.0F;
        int imageX = this.rightX + (this.rightWidth - iconSize) / 2;
        int imageY = contentTop + 56;
        graphics.pose().pushMatrix();
        graphics.pose().translate(imageX, imageY);
        graphics.pose().scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, 0, 0, 0.0F, 0.0F, 18, 18, 18, 18);
        graphics.pose().popMatrix();

        graphics.textWithWordWrap(
            this.font,
            Component.literal("Pick this effect to apply it. Re-picking the same effect upgrades it until the cap."),
            this.rightX + 14,
            contentTop + 150,
            this.rightWidth - 28,
            0xFFE0E0E0
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxOffset = Math.max(0, this.effects.size() - MAX_VISIBLE_EFFECTS);
        if (scrollY > 0 && this.scrollOffset > 0) {
            this.scrollOffset--;
            refreshButtonLabels();
            return true;
        }
        if (scrollY < 0 && this.scrollOffset < maxOffset) {
            this.scrollOffset++;
            refreshButtonLabels();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refreshButtonLabels() {
        for (int i = 0; i < this.effectButtons.size(); i++) {
            int idx = this.scrollOffset + i;
            Button button = this.effectButtons.get(i);
            if (idx >= this.effects.size()) {
                button.visible = false;
                button.active = false;
                this.visibleEffectIds.set(i, null);
                continue;
            }
            button.visible = true;
            String effectId = this.effects.get(idx);
            this.visibleEffectIds.set(i, effectId);
            boolean capped = EffectSmpClientMod.isEffectCapped(effectId);
            button.active = !capped;
            button.setMessage(effectButtonLabel(effectId, effectId.equals(this.selectedEffectId), capped));
        }
        this.chooseButton.active = this.selectedEffectId != null && !EffectSmpClientMod.isEffectCapped(this.selectedEffectId);
    }

    private void setSelectedEffect(String effectId) {
        if (effectId == null || effectId.equals(this.selectedEffectId)) {
            return;
        }
        this.selectedEffectId = effectId;
        refreshButtonLabels();
    }

    private void chooseSelectedEffect() {
        if (this.selectedEffectId == null) {
            return;
        }
        if (EffectSmpClientMod.isEffectCapped(this.selectedEffectId)) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(
                        Component.translatable("message.auracraft.effect_upgrade_cap", EffectSmpClientMod.effectName(this.selectedEffectId))
                        .copy()
                        .withStyle(ChatFormatting.BLUE)
                );
            }
            return;
        }

        ClientPlayNetworking.send(new ChooseEffectPayload(this.selectedEffectId));
        this.onClose();
    }

    private static Component effectButtonLabel(String effectId, boolean selected, boolean capped) {
        Component base = EffectSmpClientMod.effectName(effectId);
        if (selected) {
            base = Component.literal("> ").append(base);
        }
        if (capped) {
            base = base.copy().append(Component.literal(" (MAX)"));
        }
        return base;
    }

    private static Identifier getEffectTexture(String effectId) {
        try {
            Identifier id = Identifier.parse(effectId);
            return Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/mob_effect/" + id.getPath() + ".png");
        } catch (Exception ignored) {
            return Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/speed.png");
        }
    }

    private int maxScrollOffset() {
        return Math.max(0, this.effects.size() - MAX_VISIBLE_EFFECTS);
    }
}
