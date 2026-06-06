package artm.auracraft.client;

import artm.auracraft.AuraCraft;
import artm.auracraft.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class ConfigScreen extends Screen {

    // Layout
    private static final int PANEL_WIDTH  = 520;
    private static final int MAX_VISIBLE  = 8;
    private static final int ROW_HEIGHT   = 20;
    private static final int ROW_SPACING  = 26;
    private static final int TOGGLE_W     = 80;
    private static final int EDIT_W       = 46;

    // Colors
    private static final int COLOR_TITLE    = 0xFFC8D8FF;
    private static final int COLOR_TEXT     = 0xFFA0B4CC;
    private static final int COLOR_DIM      = 0xFF6A7A9A;
    private static final int COLOR_WARN     = 0xFFFFAA00;
    private static final int COLOR_ENABLED  = 0xFF22AA44;
    private static final int COLOR_DISABLED = 0xFFCC3333;

    // State
    private final List<String>        allEffects     = new ArrayList<>();
    private final Map<String, Boolean> pendingEnabled = new HashMap<>();
    private final Map<String, Integer> pendingLevels  = new HashMap<>();
    private final List<String>        visibleIds     = new ArrayList<>();
    private final List<Button>        toggleButtons  = new ArrayList<>();
    private final List<EditBox>       levelBoxes     = new ArrayList<>();
    private int scrollOffset = 0;

    // Layout positions (computed in init)
    private int panelX, panelY, contentTop, listTop, footerY, toggleX, editX;

    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("screen.auracraft.config_title"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    protected void init() {
        super.init();

        // Collect and sort all minecraft: effects
        allEffects.clear();
        for (Identifier id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            if (id.getNamespace().equals("minecraft")) {
                allEffects.add(id.toString());
            }
        }
        allEffects.sort(Comparator.naturalOrder());

        // Snapshot current config into pending maps
        Config cfg = Config.get();
        pendingEnabled.clear();
        pendingLevels.clear();
        for (String id : allEffects) {
            pendingEnabled.put(id, cfg.isAuraEnabled(id));
            pendingLevels.put(id, cfg.getMaxUpgradeLevel(id));
        }

        // Layout — center the content block vertically
        // block = title(16) + col-headers(18) + rows(MAX_VISIBLE*ROW_SPACING) + gap(8) + buttons(20)
        int totalH = 16 + 18 + MAX_VISIBLE * ROW_SPACING + 8 + 20;
        panelX     = (this.width  - PANEL_WIDTH) / 2;
        panelY     = Math.max(8, (this.height - totalH) / 2);
        contentTop = panelY + 18;   // column headers
        listTop    = contentTop + 16;
        footerY    = listTop + MAX_VISIBLE * ROW_SPACING + 8;
        toggleX    = panelX + PANEL_WIDTH - EDIT_W - TOGGLE_W - 16;
        editX      = panelX + PANEL_WIDTH - EDIT_W - 8;

        boolean editable = this.minecraft.getSingleplayerServer() != null;

        // Build MAX_VISIBLE toggle buttons + edit boxes
        toggleButtons.clear();
        levelBoxes.clear();
        visibleIds.clear();

        for (int i = 0; i < MAX_VISIBLE; i++) {
            visibleIds.add(null);
            final int row = i;
            int rowY = listTop + i * ROW_SPACING;

            Button toggle = Button.builder(Component.empty(), btn -> {
                String id = visibleIds.get(row);
                if (id == null) return;
                pendingEnabled.put(id, !pendingEnabled.getOrDefault(id, false));
                refreshList();
            }).bounds(toggleX, rowY, TOGGLE_W, ROW_HEIGHT).build();
            toggle.active = editable;
            toggleButtons.add(this.addRenderableWidget(toggle));

            EditBox box = new EditBox(this.font,
                    editX, rowY, EDIT_W, ROW_HEIGHT, Component.empty());
            box.setMaxLength(3);
            box.setEditable(editable);
            box.setResponder(text -> {
                String id = visibleIds.get(row);
                if (id == null || text.isBlank()) return;
                try {
                    pendingLevels.put(id, Math.max(0, Integer.parseInt(text)));
                } catch (NumberFormatException ignored) {}
            });
            levelBoxes.add(this.addRenderableWidget(box));
        }

        // Footer buttons
        int btnW    = 100;
        int centerX = panelX + PANEL_WIDTH / 2;

        Button saveBtn = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.auracraft.save"), btn -> onSave()
        ).bounds(centerX - btnW - 4, footerY, btnW, 20).build());
        saveBtn.active = editable;

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.auracraft.cancel"), btn -> this.onClose()
        ).bounds(centerX + 4, footerY, btnW, 20).build());

        refreshList();
    }

    // Render
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics,
                                   int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);

        boolean editable = this.minecraft.getSingleplayerServer() != null;

        // Title
        graphics.centeredText(this.font, this.title,
                panelX + PANEL_WIDTH / 2, panelY, COLOR_TITLE);

        // Read-only warning (below title)
        if (!editable) {
            graphics.centeredText(this.font,
                    Component.translatable("screen.auracraft.config_singleplayer_only")
                            .withStyle(s -> s.withColor(COLOR_WARN)),
                    panelX + PANEL_WIDTH / 2, panelY + 10, COLOR_WARN);
        }

        Component colName   = Component.translatable("screen.auracraft.col_header_name");
        Component colToggle = Component.translatable("screen.auracraft.col_header_toggle");
        Component colMax    = Component.translatable("screen.auracraft.col_header_max");
        graphics.text(this.font, colName.copy().withStyle(s -> s.withColor(COLOR_DIM)),
                panelX + 8, contentTop, COLOR_DIM);
        graphics.text(this.font, colToggle.copy().withStyle(s -> s.withColor(COLOR_DIM)),
                toggleX + (TOGGLE_W - this.font.width(colToggle)) / 2,
                contentTop, COLOR_DIM);
        graphics.text(this.font, colMax.copy().withStyle(s -> s.withColor(COLOR_DIM)),
                editX + (EDIT_W - this.font.width(colMax)) / 2,
                contentTop, COLOR_DIM);

        // Aura name labels for each visible row
        for (int i = 0; i < MAX_VISIBLE; i++) {
            String id = visibleIds.get(i);
            if (id == null) continue;
            int rowY = listTop + i * ROW_SPACING;
            Identifier parsed = Identifier.parse(id);
            Component name = Component.translatable(
                    "effect." + parsed.getNamespace() + "." + parsed.getPath());
            graphics.text(this.font, name,
                    panelX + 8,
                    rowY + (ROW_HEIGHT - 8) / 2,
                    COLOR_TEXT);
        }
    }

    // Scroll
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double scrollX, double scrollY) {
        int max = Math.max(0, allEffects.size() - MAX_VISIBLE);
        if (scrollY > 0 && scrollOffset > 0) {
            scrollOffset--;
            refreshList();
            return true;
        }
        if (scrollY < 0 && scrollOffset < max) {
            scrollOffset++;
            refreshList();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // Internal Helpers
    private void refreshList() {
        boolean editable = this.minecraft != null
                && this.minecraft.getSingleplayerServer() != null;

        for (int i = 0; i < MAX_VISIBLE; i++) {
            int     idx    = scrollOffset + i;
            Button  toggle = toggleButtons.get(i);
            EditBox box    = levelBoxes.get(i);

            if (idx >= allEffects.size()) {
                toggle.visible = false;
                toggle.active  = false;
                box.visible    = false;
                visibleIds.set(i, null);
                continue;
            }

            String  id      = allEffects.get(idx);
            boolean enabled = pendingEnabled.getOrDefault(id, false);
            int     level   = pendingLevels.getOrDefault(id, 1);

            visibleIds.set(i, id);

            toggle.visible = true;
            toggle.active  = editable;
            toggle.setMessage(enabled
                    ? Component.translatable("screen.auracraft.col_enabled") .withStyle(s -> s.withColor(COLOR_ENABLED))
                    : Component.translatable("screen.auracraft.col_disabled").withStyle(s -> s.withColor(COLOR_DISABLED)));

            box.visible = true;
            box.setEditable(editable);
            String levelStr = String.valueOf(level);
            if (!box.getValue().equals(levelStr)) {
                box.setValue(levelStr);
            }
        }
    }

    private void onSave() {
        Config cfg = Config.get();
        for (String id : allEffects) {
            cfg.setAuraEnabled(id, pendingEnabled.getOrDefault(id, false));
            cfg.setMaxUpgradeLevel(id, pendingLevels.getOrDefault(id, 1));
        }
        cfg.save();

        MinecraftServer server = this.minecraft.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    AuraCraft.syncAuras(player);
                    AuraCraft.syncUiState(player);
                }
            });
        }
        this.onClose();
    }
}
