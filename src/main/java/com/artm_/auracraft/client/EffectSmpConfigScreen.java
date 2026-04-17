package com.artm_.auracraft.client;

import com.artm_.auracraft.EffectSmpConfig;
import com.artm_.auracraft.EffectSmpMod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class EffectSmpConfigScreen extends Screen {
    private enum Tab {
        GENERAL,
        EFFECTS
    }

    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 330;
    private static final int MAX_VISIBLE_EFFECTS = 8;
    private static final int EFFECT_ROW_SPACING = 22;
    private static final int EFFECT_BUTTON_HEIGHT = 20;
    private static final int EFFECTS_TOP_Y_OFFSET = 96;
    private static final int EFFECTS_NAMES_TOP_Y_OFFSET = 100;

    private final Screen parent;
    private EffectSmpConfig working;
    private final List<String> allEffectIds = new ArrayList<>();
    private final List<Button> effectToggleButtons = new ArrayList<>();
    private final List<String> visibleEffectIds = new ArrayList<>();
    private int effectScrollOffset;

    private int panelX;
    private int panelY;
    private int valueX;

    private EditBox pvpLossBox;
    private EditBox duplicateBonusBox;
    private Button generalTabButton;
    private Button effectsTabButton;
    private Tab activeTab = Tab.GENERAL;
    private Component statusText = Component.empty();

    public EffectSmpConfigScreen(Screen parent) {
        super(Component.literal("AuraCraft Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.working = copyConfig(EffectSmpConfig.get());
        this.allEffectIds.clear();
        this.effectScrollOffset = 0;

        BuiltInRegistries.MOB_EFFECT.keySet().stream()
            .map(Identifier::toString)
            .sorted(Comparator.naturalOrder())
            .forEach(this.allEffectIds::add);

        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;
        this.valueX = this.panelX + PANEL_WIDTH - 170;

        buildStaticControls();
        buildTabContents();
        refreshTabVisibility();
    }

    private void buildStaticControls() {
        int left = this.panelX + 14;
        int tabY = this.panelY + 30;
        this.generalTabButton = this.addRenderableWidget(
            Button.builder(Component.literal("General"), b -> {
                this.activeTab = Tab.GENERAL;
                refreshTabVisibility();
            }).bounds(left, tabY, 100, 20).build()
        );
        this.effectsTabButton = this.addRenderableWidget(
            Button.builder(Component.literal("Effects"), b -> {
                this.activeTab = Tab.EFFECTS;
                refreshTabVisibility();
            }).bounds(left + 106, tabY, 100, 20).build()
        );

        int footerY = this.panelY + PANEL_HEIGHT - 28;
        int buttonWidth = (PANEL_WIDTH - 38) / 2;
        this.addRenderableWidget(
            Button.builder(Component.literal("Save"), b -> saveAndClose())
                .bounds(this.panelX + 16, footerY, buttonWidth, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(this.panelX + 16 + buttonWidth + 6, footerY, buttonWidth, 20)
                .build()
        );
    }

    private void buildTabContents() {
        int rowY = this.panelY + 78;
        this.pvpLossBox = new EditBox(this.font, this.valueX, rowY - 2, 120, 20, Component.literal("PvPEffectsLostOnDeath"));
        this.pvpLossBox.setMaxLength(3);
        this.pvpLossBox.setValue(Integer.toString(this.working.pvpEffectsLostOnDeath));
        this.addRenderableWidget(this.pvpLossBox);

        rowY += 30;
        this.duplicateBonusBox = new EditBox(this.font, this.valueX, rowY - 2, 120, 20, Component.literal("maxDuplicateAmplifierBonus"));
        this.duplicateBonusBox.setMaxLength(3);
        this.duplicateBonusBox.setValue(Integer.toString(this.working.maxDuplicateAmplifierBonus));
        this.addRenderableWidget(this.duplicateBonusBox);

        int effectsTopY = this.panelY + EFFECTS_TOP_Y_OFFSET;
        this.effectToggleButtons.clear();
        this.visibleEffectIds.clear();
        for (int i = 0; i < MAX_VISIBLE_EFFECTS; i++) {
            this.visibleEffectIds.add(null);
            final int row = i;
            Button toggle = this.addRenderableWidget(
                Button.builder(Component.empty(), b -> {
                    String effectId = this.visibleEffectIds.get(row);
                    if (effectId == null) {
                        return;
                    }
                    boolean current = this.working.isEffectEnabled(effectId);
                    this.working.setEffectEnabled(effectId, !current);
                    refreshEffectToggleLabels();
                }).bounds(this.panelX + PANEL_WIDTH - 145, effectsTopY + (i * EFFECT_ROW_SPACING), 100, EFFECT_BUTTON_HEIGHT).build()
            );
            this.effectToggleButtons.add(toggle);
        }

        refreshEffectToggleLabels();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
            if (this.parent == null) {
                this.minecraft.setScreen(null);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractBackground(graphics, mouseX, mouseY, deltaTicks);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + PANEL_HEIGHT, 0xCC202020);
        graphics.outline(this.panelX, this.panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF4A4A4A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
        int left = this.panelX + 16;
        int rowY = this.panelY + 78;

        graphics.centeredText(this.font, this.title, this.panelX + (PANEL_WIDTH / 2), this.panelY + 10, 0xFFFFFFFF);

        if (this.activeTab == Tab.GENERAL) {
            graphics.text(this.font, Component.literal("PvPEffectsLostOnDeath"), left, rowY + 4, 0xFFE0E0E0);
            rowY += 30;
            graphics.text(this.font, Component.literal("maxDuplicateAmplifierBonus"), left, rowY + 4, 0xFFE0E0E0);
        } else {
            int headerY = this.panelY + 78;
            graphics.text(this.font, Component.literal("Effect Name"), left, headerY, 0xFFE0E0E0);

            int namesTop = this.panelY + EFFECTS_NAMES_TOP_Y_OFFSET;
            for (int i = 0; i < MAX_VISIBLE_EFFECTS; i++) {
                String effectId = this.visibleEffectIds.get(i);
                if (effectId == null) {
                    continue;
                }
                graphics.text(this.font, effectName(effectId), left, namesTop + (i * EFFECT_ROW_SPACING) + 6, 0xFFE0E0E0);
            }

            int trackX = this.panelX + PANEL_WIDTH - 30;
            int trackTop = this.panelY + EFFECTS_TOP_Y_OFFSET;
            int trackHeight = ((MAX_VISIBLE_EFFECTS - 1) * EFFECT_ROW_SPACING) + EFFECT_BUTTON_HEIGHT;
            graphics.fill(trackX, trackTop, trackX + 6, trackTop + trackHeight, 0xFF3A3A3A);

            int maxOffset = maxEffectScrollOffset();
            int thumbHeight = maxOffset == 0 ? trackHeight : Math.max(20, trackHeight / 3);
            int thumbTravel = Math.max(0, trackHeight - thumbHeight);
            int thumbY = trackTop + (maxOffset == 0 ? 0 : (this.effectScrollOffset * thumbTravel) / maxOffset);
            graphics.fill(trackX, thumbY, trackX + 6, thumbY + thumbHeight, 0xFFB8B8B8);

        }

        if (!this.statusText.getString().isEmpty()) {
            graphics.centeredText(this.font, this.statusText, this.panelX + (PANEL_WIDTH / 2), this.panelY + PANEL_HEIGHT - 40, 0xFFFF8080);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.activeTab == Tab.EFFECTS) {
            int maxOffset = maxEffectScrollOffset();
            if (scrollY > 0 && this.effectScrollOffset > 0) {
                this.effectScrollOffset--;
                refreshEffectToggleLabels();
                return true;
            }
            if (scrollY < 0 && this.effectScrollOffset < maxOffset) {
                this.effectScrollOffset++;
                refreshEffectToggleLabels();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void refreshTabVisibility() {
        boolean general = this.activeTab == Tab.GENERAL;
        this.generalTabButton.active = !general;
        this.effectsTabButton.active = general;

        this.pvpLossBox.visible = general;
        this.pvpLossBox.setEditable(general);
        this.duplicateBonusBox.visible = general;
        this.duplicateBonusBox.setEditable(general);

        for (Button toggle : this.effectToggleButtons) {
            toggle.visible = !general;
        }
    }

    private void refreshEffectToggleLabels() {
        for (int i = 0; i < MAX_VISIBLE_EFFECTS; i++) {
            int idx = this.effectScrollOffset + i;
            Button button = this.effectToggleButtons.get(i);
            if (idx >= this.allEffectIds.size()) {
                this.visibleEffectIds.set(i, null);
                button.visible = false;
                button.active = false;
                continue;
            }
            String effectId = this.allEffectIds.get(idx);
            this.visibleEffectIds.set(i, effectId);
            button.visible = true;
            button.active = true;
            boolean enabled = this.working.isEffectEnabled(effectId);
            button.setMessage(Component.literal(enabled ? "Enabled" : "Disabled").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }

    private void saveAndClose() {
        try {
            this.working.pvpEffectsLostOnDeath = Math.max(0, Integer.parseInt(this.pvpLossBox.getValue()));
            this.working.maxDuplicateAmplifierBonus = Math.max(0, Integer.parseInt(this.duplicateBonusBox.getValue()));

            EffectSmpConfig cfg = EffectSmpConfig.get();
            cfg.pvpEffectsLostOnDeath = this.working.pvpEffectsLostOnDeath;
            cfg.maxDuplicateAmplifierBonus = this.working.maxDuplicateAmplifierBonus;
            cfg.enabledEffects = new HashMap<>(this.working.enabledEffects);
            cfg.save();
            EffectSmpConfig.load();
            EffectSmpClientMod.refreshEnabledEffectsFromConfig();

            if (this.minecraft != null && this.minecraft.getSingleplayerServer() != null) {
                for (ServerPlayer player : this.minecraft.getSingleplayerServer().getPlayerList().getPlayers()) {
                    EffectSmpMod.syncChosenEffect(player);
                }
            }
            this.onClose();
        } catch (NumberFormatException e) {
            this.statusText = Component.literal("Invalid number. Check your values.");
        }
    }

    private static Component effectName(String effectId) {
        try {
            Identifier id = Identifier.parse(effectId);
            return Component.translatable("effect." + id.getNamespace() + "." + id.getPath());
        } catch (Exception ignored) {
            return Component.literal(effectId);
        }
    }

    private static EffectSmpConfig copyConfig(EffectSmpConfig source) {
        EffectSmpConfig copy = new EffectSmpConfig();
        copy.maxEffects = source.maxEffects;
        copy.pvpEffectsLostOnDeath = source.pvpEffectsLostOnDeath;
        copy.maxDuplicateAmplifierBonus = source.maxDuplicateAmplifierBonus;
        copy.dropPlusItemOnPvpKill = source.dropPlusItemOnPvpKill;
        copy.enablePlusRecipe = source.enablePlusRecipe;
        copy.enableResetRecipe = source.enableResetRecipe;
        copy.enabledEffects = new HashMap<>(source.enabledEffects);
        return copy;
    }

    private int maxEffectScrollOffset() {
        return Math.max(0, this.allEffectIds.size() - MAX_VISIBLE_EFFECTS);
    }
}
