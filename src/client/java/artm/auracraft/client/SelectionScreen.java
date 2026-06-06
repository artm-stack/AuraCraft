package artm.auracraft.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SelectionScreen extends Screen {
    // Layout
    private static final int PANEL_WIDTH   = 440;
    private static final int PANEL_HEIGHT  = 340;
    private static final int HEADER_HEIGHT = 26;
    private static final int FOOTER_HEIGHT = 60;
    private static final int MAX_VISIBLE   = 12;
    private static final int ROW_SPACING   = 18;
    private static final int ROW_HEIGHT    = 16;
    private static final int LEFT_WIDTH    = 180;
    private static final int DIVIDER       = 6;

    // Colors
    private static final int COLOR_BG           = 0xFF0D1117;
    private static final int COLOR_HEADER_BG    = 0xFF080D16;
    private static final int COLOR_ACCENT       = 0xFF1E4FC2;
    private static final int COLOR_SELECTED_ROW = 0x880D2B5C;
    private static final int COLOR_SELECTED_BAR = 0xFF4A90E2;
    private static final int COLOR_SCROLLTRACK  = 0xFF0D1929;
    private static final int COLOR_SCROLLTHUMB  = 0xFF1E4FC2;
    private static final int COLOR_TITLE        = 0xFFC8D8FF;
    private static final int COLOR_TEXT_BRIGHT  = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM     = 0xFF6A7A9A;
    private static final int COLOR_TEXT_BODY    = 0xFFA0B4CC;
    private static final int COLOR_TEXT_MUTED   = 0xFF445566;
    private static final int COLOR_TEXT_BLUE    = 0xFF4A90E2;
    private static final int COLOR_TEXT_GOLD    = 0xFFFFD700;
    private static final int COLOR_TEXT_RED     = 0xFFFF6B6B;
    private static final int COLOR_TEXT_MAX     = 0xFF445566;

    // State
    private final List<String> auras       = new ArrayList<>();
    private final List<Button> auraButtons = new ArrayList<>();
    private final List<String> visibleIds  = new ArrayList<>();
    private String selectedAuraId;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int dragStartMouseY;
    private int dragStartScrollOffset;

    // Layout Position
    private int panelX;
    private int panelY;
    private int rightX;
    private int rightWidth;
    private int contentTop;
    private int footerY;
    private Button chooseButton;

    // Constructor
    public SelectionScreen() { super(Component.translatable("screen.auracraft.title")); }

    // Initialisation
    @Override
    protected void init() {
        super.init();

        this.auras.clear();
        this.auras.addAll(AuraCraftClient.getEnabledAurasForPicker());

        this.panelX     = (this.width - PANEL_WIDTH) / 2;
        this.panelY     = (this.height - PANEL_HEIGHT) / 2;
        this.rightX     = this.panelX + LEFT_WIDTH + DIVIDER;
        this.rightWidth = PANEL_WIDTH - LEFT_WIDTH - DIVIDER;
        this.contentTop = this.panelY + HEADER_HEIGHT;
        this.footerY    = this.panelY + PANEL_HEIGHT - FOOTER_HEIGHT;

        int listTop = this.contentTop + 20;

        this.auraButtons.clear();
        this.visibleIds.clear();
        for (int i = 0; i < MAX_VISIBLE; i++) {
            this.visibleIds.add(null);
            final int row = i;
            AuraButton btn = new AuraButton(
                    this.panelX + 10,
                    listTop + (i * ROW_SPACING),
                    LEFT_WIDTH - 24,
                    ROW_HEIGHT,
                    Component.empty(),
                    _ -> {
                        String id = this.visibleIds.get(row);
                        if (id != null) selectAura(id);
                    }
            );
            this.auraButtons.add(this.addRenderableWidget(btn));
        }

        int btnWidth = 140;
        this.chooseButton = this.addRenderableWidget(
                new AuraButton(
                        this.panelX + (PANEL_WIDTH - btnWidth) / 2,
                        this.footerY + (FOOTER_HEIGHT - 24) / 2,
                        btnWidth,
                        24,
                        Component.translatable("screen.auracraft.choose_button"),
                        _ -> confirmSelection()
                )
        );
        refreshList();
        // Auto-select first aura so the right panel is never blank on open
        if (!this.auras.isEmpty() && this.selectedAuraId == null) {
            selectAura(this.auras.get(0));
        }
    }

    // Background
    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractBackground(graphics, mouseX, mouseY, deltaTicks);
        int listTop = this.contentTop + 20;
        graphics.fill(this.panelX, this.panelY,
                this.panelX + PANEL_WIDTH, this.panelY + PANEL_HEIGHT,
                COLOR_BG);
        graphics.fill(this.panelX, this.panelY,
                this.panelX + PANEL_WIDTH, this.contentTop,
                COLOR_HEADER_BG);
        graphics.fill(this.panelX, this.contentTop,
                this.panelX + PANEL_WIDTH, this.contentTop + 1,
                COLOR_ACCENT);
        graphics.fill(this.panelX, this.footerY,
                this.panelX + PANEL_WIDTH, this.panelY + PANEL_HEIGHT,
                COLOR_HEADER_BG);
        graphics.fill(this.panelX, this.footerY,
                this.panelX + PANEL_WIDTH, this.footerY + 1,
                COLOR_ACCENT);
        graphics.fill(this.panelX + LEFT_WIDTH, this.contentTop,
                this.panelX + LEFT_WIDTH + 1, this.footerY,
                COLOR_ACCENT);
        if (this.selectedAuraId != null) {
            int selectedIdx = this.auras.indexOf(this.selectedAuraId);
            int visibleIdx  = selectedIdx - this.scrollOffset;
            if (visibleIdx >= 0 && visibleIdx < MAX_VISIBLE) {
                int rowY = listTop + (visibleIdx * ROW_SPACING);
                graphics.fill(
                        this.panelX + 10, rowY - 1,
                        this.panelX + LEFT_WIDTH - 14, rowY + ROW_HEIGHT,
                        COLOR_SELECTED_ROW);
                graphics.fill(
                        this.panelX + 8, rowY,
                        this.panelX + 12, rowY + ROW_HEIGHT,
                        COLOR_SELECTED_BAR);
            }
        }
        int trackX   = scrollTrackX();
        int trackH   = scrollTrackHeight();
        int thumbH   = scrollThumbHeight();
        int thumbTopY = scrollThumbY();
        graphics.fill(trackX, listTop, trackX + 5, listTop + trackH, COLOR_SCROLLTRACK);
        graphics.fill(trackX, thumbTopY, trackX + 5, thumbTopY + thumbH, COLOR_SCROLLTHUMB);
        graphics.outline(this.panelX, this.panelY,
                PANEL_WIDTH, PANEL_HEIGHT, COLOR_ACCENT);
    }

    // Render
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
        graphics.centeredText(this.font, this.title,
                this.panelX + (PANEL_WIDTH / 2),
                this.panelY + (HEADER_HEIGHT - 8) / 2,
                COLOR_TITLE);
        Component pointsLabel = Component.translatable("screen.auracraft.points_label")
                .withStyle(s -> s.withColor(COLOR_TEXT_DIM))
                .append(Component.literal(
                                String.valueOf(AuraCraftClient.getAuraPoints()))
                        .withStyle(s -> s.withColor(COLOR_TEXT_BLUE)));
        graphics.text(this.font, pointsLabel,
                this.panelX + PANEL_WIDTH
                        - this.font.width(pointsLabel) - 8,
                this.panelY + (HEADER_HEIGHT - 8) / 2,
                COLOR_TEXT_BRIGHT);
        graphics.text(this.font,
                Component.translatable("screen.auracraft.left_header"),
                this.panelX + 12,
                this.contentTop + 8,
                COLOR_TEXT_DIM);
        if (this.auras.isEmpty()) {
            graphics.centeredText(this.font,
                    Component.translatable("screen.auracraft.no_auras")
                            .withStyle(ChatFormatting.RED),
                    this.rightX + this.rightWidth / 2,
                    this.contentTop + 21,
                    COLOR_TEXT_RED);
            return;
        }
        graphics.centeredText(this.font,
                AuraCraftClient.auraName(this.selectedAuraId),
                this.rightX + this.rightWidth / 2,
                this.contentTop + 8,
                COLOR_TEXT_BRIGHT);
        Identifier icon = getAuraTexture(this.selectedAuraId);
        int iconSize    = 72;
        float scale     = iconSize / 18.0F;
        int iconX       = this.rightX + (this.rightWidth - iconSize) / 2;
        int iconY       = this.contentTop + 24;
        graphics.pose().pushMatrix();
        graphics.pose().translate(iconX, iconY);
        graphics.pose().scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED,
                icon, 0, 0, 0.0F, 0.0F, 18, 18, 18, 18);
        graphics.pose().popMatrix();
        List<String> chosen = AuraCraftClient.getChosenAuras();
        int currentLevel    = Collections.frequency(chosen, this.selectedAuraId);
        int maxLevel        = AuraCraftClient.getMaxUpgradeLevel(this.selectedAuraId) + 1;
        boolean capped      = AuraCraftClient.isAuraCapped(this.selectedAuraId);
        Component levelText = currentLevel == 0
                ? Component.translatable("screen.auracraft.not_chosen")
                .withStyle(s -> s.withColor(COLOR_TEXT_DIM))
                : Component.translatable("screen.auracraft.level", currentLevel, maxLevel)
                .withStyle(s -> s.withColor(
                        capped ? COLOR_TEXT_MAX : COLOR_TEXT_GOLD));
        graphics.centeredText(this.font, levelText,
                this.rightX + this.rightWidth / 2,
                this.contentTop + 104,
                COLOR_TEXT_BRIGHT);
        int sepY = this.contentTop + 118;
        graphics.fill(this.rightX + 4, sepY,
                this.rightX + this.rightWidth - 4, sepY + 1,
                0xFF1A2A3A);
        graphics.text(this.font,
                Component.translatable("screen.auracraft.description_header"),
                this.rightX + 8,
                sepY + 8,
                COLOR_TEXT_DIM);
        graphics.textWithWordWrap(this.font,
                getDescription(this.selectedAuraId),
                this.rightX + 8,
                sepY + 20,
                this.rightWidth - 16,
                COLOR_TEXT_BODY);
        if (!capped) {
            Component tip = currentLevel == 0
                    ? Component.translatable("screen.auracraft.tip_choose")
                    : Component.translatable("screen.auracraft.tip_upgrade");
            graphics.text(this.font, tip,
                    this.rightX + 8,
                    this.footerY - 14,
                    COLOR_TEXT_MUTED);
        }
    }

    // Input
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();
            int tx      = scrollTrackX();
            int listTop = scrollListTop();
            int trackH  = scrollTrackHeight();
            if (mouseX >= tx && mouseX <= tx + 5
                    && mouseY >= listTop && mouseY <= listTop + trackH) {
                int thumbY = scrollThumbY();
                int thumbH = scrollThumbHeight();
                if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                    this.draggingScrollbar     = true;
                    this.dragStartMouseY       = (int) mouseY;
                    this.dragStartScrollOffset = this.scrollOffset;
                } else {
                    int max = maxScrollOffset();
                    if (max > 0) {
                        int relY = (int) mouseY - listTop;
                        this.scrollOffset = Math.max(0, Math.min(max, relY * max / trackH));
                        refreshList();
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && this.draggingScrollbar) {
            int max = maxScrollOffset();
            if (max > 0) {
                int travel = Math.max(0, scrollTrackHeight() - scrollThumbHeight());
                if (travel > 0) {
                    int delta = (int) event.y() - this.dragStartMouseY;
                    this.scrollOffset = Math.max(0, Math.min(max,
                            this.dragStartScrollOffset + (delta * max) / travel));
                    refreshList();
                }
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && this.scrollOffset > 0) {
            this.scrollOffset--;
            refreshList();
            return true;
        }
        if (scrollY < 0 && this.scrollOffset < maxScrollOffset()) {
            this.scrollOffset++;
            refreshList();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Called by AuraCraftClient when aura/UI state is synced from the server. */
    public void extractWidgets() {
        // Refresh the aura list in case enabled auras changed
        this.auras.clear();
        this.auras.addAll(AuraCraftClient.getEnabledAurasForPicker());
        // Discard the current selection if the aura is no longer available
        if (this.selectedAuraId != null && !this.auras.contains(this.selectedAuraId)) {
            this.selectedAuraId = null;
        }
        // Auto-select first entry if nothing is selected
        if (this.selectedAuraId == null && !this.auras.isEmpty()) {
            this.selectedAuraId = this.auras.get(0);
        }
        // Clamp scroll so we don't end up past the last row
        this.scrollOffset = Math.min(this.scrollOffset, maxScrollOffset());
        refreshList();
    }

    // Internal Helpers
    private void refreshList() {
        for (int i = 0; i < this.auraButtons.size(); i++) {
            int idx    = this.scrollOffset + i;
            Button btn = this.auraButtons.get(i);

            if (idx >= this.auras.size()) {
                btn.visible = false;
                btn.active  = false;
                this.visibleIds.set(i, null);
                continue;
            }

            String auraId  = this.auras.get(idx);
            boolean capped = AuraCraftClient.isAuraCapped(auraId);
            boolean sel    = auraId.equals(this.selectedAuraId);

            this.visibleIds.set(i, auraId);
            btn.visible = true;
            btn.active  = !capped;
            btn.setMessage(buildRowLabel(auraId, sel, capped));
        }

        this.chooseButton.active = this.selectedAuraId != null
                && !AuraCraftClient.isAuraCapped(this.selectedAuraId)
                && AuraCraftClient.getAuraPoints() > 0;
    }

    private void selectAura(String auraId) {
        if (auraId == null || auraId.equals(this.selectedAuraId)) return;
        this.selectedAuraId = auraId;
        refreshList();
    }

    private void confirmSelection() {
        if (this.selectedAuraId == null) return;
        if (AuraCraftClient.isAuraCapped(this.selectedAuraId)) return;
        AuraCraftClient.sendChooseAura(this.selectedAuraId);
        this.onClose();
    }

    private static Component buildRowLabel(String auraId, boolean selected, boolean capped) {
        Component name = AuraCraftClient.auraName(auraId);
        if (capped) {
            return name.copy()
                    .append(Component.literal(" ✦")
                            .withStyle(s -> s.withColor(COLOR_TEXT_MAX)));
        }
        if (selected) {
            return Component.literal("▶ ")
                    .withStyle(s -> s.withColor(COLOR_TEXT_BLUE))
                    .append(name);
        }
        return name;
    }

    private static Component getDescription(String auraId) {
        try {
            Identifier id = Identifier.parse(auraId);
            return Component.translatable(
                    "aura.description."
                            + id.getNamespace() + "."
                            + id.getPath());
        } catch (Exception ignored) {
            return Component.translatable("aura.description.unknown");
        }
    }

    private static Identifier getAuraTexture(String auraId) {
        try {
            Identifier id = Identifier.parse(auraId);
            return Identifier.fromNamespaceAndPath(
                    id.getNamespace(),
                    "textures/mob_effect/" + id.getPath() + ".png");
        } catch (Exception ignored) {
            return Identifier.fromNamespaceAndPath(
                    "minecraft", "textures/mob_effect/speed.png");
        }
    }

    private int maxScrollOffset() {
        return Math.max(0, this.auras.size() - MAX_VISIBLE);
    }
    private int scrollTrackX()      { return this.panelX + LEFT_WIDTH - 9; }
    private int scrollListTop()     { return this.contentTop + 20; }
    private int scrollTrackHeight() { return ((MAX_VISIBLE - 1) * ROW_SPACING) + ROW_HEIGHT; }
    private int scrollThumbHeight() {
        int max = maxScrollOffset();
        return max == 0 ? scrollTrackHeight()
                : Math.max(18, scrollTrackHeight() * MAX_VISIBLE / Math.max(1, this.auras.size()));
    }
    private int scrollThumbY() {
        int travel = Math.max(0, scrollTrackHeight() - scrollThumbHeight());
        int max    = maxScrollOffset();
        return scrollListTop() + (max == 0 ? 0 : (this.scrollOffset * travel) / max);
    }

    private static class AuraButton extends Button {

        private static final int COLOR_BTN_BG = 0xFF0D2B5C;
        private static final int COLOR_BTN_BORDER = 0xFF1E4FC2;
        private static final int COLOR_BTN_HOVER_BG = 0xFF1A3D7A;
        private static final int COLOR_BTN_DISABLED = 0xFF1A1A2A;
        private static final int COLOR_BTN_TEXT = 0xFFC8D8FF;
        private static final int COLOR_BTN_TEXT_OFF = 0xFF445566;

        protected AuraButton(int x, int y, int width, int height,
                             Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void extractContents(@NotNull GuiGraphicsExtractor graphics,
                                    int mouseX, int mouseY, float deltaTicks) {

            int bg = !this.active
                    ? COLOR_BTN_DISABLED
                    : this.isHovered()
                      ? COLOR_BTN_HOVER_BG
                      : COLOR_BTN_BG;

            int textColor = this.active
                    ? COLOR_BTN_TEXT
                    : COLOR_BTN_TEXT_OFF;


            graphics.fill(this.getX(), this.getY(),
                    this.getX() + this.width,
                    this.getY() + this.height,
                    bg);


            if (this.active) {
                graphics.outline(this.getX(), this.getY(),
                        this.width, this.height,
                        COLOR_BTN_BORDER);
            }

            graphics.centeredText(
                    Minecraft.getInstance().font,
                    this.getMessage(),
                    this.getX() + this.width / 2,
                    this.getY() + (this.height - 8) / 2,
                    textColor);
        }
    }
}
