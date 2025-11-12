package ltown.hev_suit.client.screen;

import ltown.hev_suit.client.managers.SettingsManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class HevSuitConfigScreen extends Screen {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#?([0-9A-Fa-f]{6})$");

    private final Screen parent;
    private final List<ConfigSection> sections;
    private final Map<ClickableWidget, Text> tooltips = new HashMap<>();
    private final Map<TextFieldWidget, Text> colorFieldLabels = new HashMap<>();
    private ConfigSection currentSection;
    private TextFieldWidget primaryColorField;
    private TextFieldWidget secondaryColorField;
    private Text statusMessage;
    private int statusMessageColor = 0xFFFFFFFF;
    private int statusMessageTicks;

    public HevSuitConfigScreen(Screen parent) {
        super(Text.translatable("hev_suit.config.title"));
        this.parent = parent;
        this.sections = List.of(
                new ConfigSection(
                        Text.literal("Suit Systems"),
                        Text.literal("Core HEV functionality and accessibility toggles."),
                        List.of(
                                new ConfigToggle(Text.literal("HEV Suit"), Text.literal("Master switch for all HEV suit features."), () -> SettingsManager.hevSuitEnabled, value -> SettingsManager.hevSuitEnabled = value),
                                new ConfigToggle(Text.literal("PVP Mode"), Text.literal("Allow HEV announcements while fighting other players."), () -> SettingsManager.pvpModeEnabled, value -> SettingsManager.pvpModeEnabled = value),
                                new ConfigToggle(Text.literal("Captions"), Text.literal("Display subtitles for suit callouts and voice lines."), () -> SettingsManager.captionsEnabled, value -> SettingsManager.captionsEnabled = value)
                        ),
                        false
                ),
                new ConfigSection(
                        Text.literal("HUD & Indicators"),
                        Text.literal("Enable or disable in-world overlays and debug helpers."),
                        List.of(
                                new ConfigToggle(Text.literal("HUD (All)"), Text.literal("Toggle every HEV HUD element on screen."), () -> SettingsManager.hudEnabled, value -> SettingsManager.hudEnabled = value),
                                new ConfigToggle(Text.literal("HUD Health"), Text.literal("Show the HEV health display widget."), () -> SettingsManager.hudHealthEnabled, value -> SettingsManager.hudHealthEnabled = value),
                                new ConfigToggle(Text.literal("HUD Armor"), Text.literal("Show the HEV armor readout."), () -> SettingsManager.hudArmorEnabled, value -> SettingsManager.hudArmorEnabled = value),
                                new ConfigToggle(Text.literal("HUD Ammo"), Text.literal("Show the HEV ammo counter."), () -> SettingsManager.hudAmmoEnabled, value -> SettingsManager.hudAmmoEnabled = value),
                                new ConfigToggle(Text.literal("HUD Alignment Mode"), Text.literal("Center HUD elements to help with custom offset tuning."), () -> SettingsManager.hudAlignmentMode, value -> SettingsManager.hudAlignmentMode = value),
                                new ConfigToggle(Text.literal("Damage Indicators"), Text.literal("Render directional hit indicators when you take damage."), () -> SettingsManager.damageIndicatorsEnabled, value -> SettingsManager.damageIndicatorsEnabled = value),
                                new ConfigToggle(Text.literal("Threat Indicators"), Text.literal("Highlight nearby hostile threats on the HUD."), () -> SettingsManager.threatIndicatorsEnabled, value -> SettingsManager.threatIndicatorsEnabled = value)
                        ),
                        false
                ),
                new ConfigSection(
                        Text.literal("HUD Colors"),
                        Text.literal("Adjust HUD accent colors using hex values (e.g. #FFAA00)."),
                        List.of(),
                        true
                ),
                new ConfigSection(
                        Text.literal("Alerts & Audio"),
                        Text.literal("Choose which suit voice alerts are allowed to play."),
                        List.of(
                                new ConfigToggle(Text.literal("Health Alerts"), Text.literal("Enable general health warning lines."), () -> SettingsManager.healthAlertsEnabled, value -> SettingsManager.healthAlertsEnabled = value),
                                new ConfigToggle(Text.literal("Armor Durability Alerts"), Text.literal("Warn when armor pieces are close to breaking."), () -> SettingsManager.armorDurabilityEnabled, value -> SettingsManager.armorDurabilityEnabled = value),
                                new ConfigToggle(Text.literal("Fracture Alerts"), Text.literal("Play fracture detection voice lines."), () -> SettingsManager.fracturesEnabled, value -> SettingsManager.fracturesEnabled = value),
                                new ConfigToggle(Text.literal("Heat Damage Alerts"), Text.literal("Announce when the suit detects fire or lava damage."), () -> SettingsManager.heatDamageEnabled, value -> SettingsManager.heatDamageEnabled = value),
                                new ConfigToggle(Text.literal("Blood Loss Alerts"), Text.literal("Trigger blood-loss specific warnings."), () -> SettingsManager.bloodLossEnabled, value -> SettingsManager.bloodLossEnabled = value),
                                new ConfigToggle(Text.literal("Shock Damage Alerts"), Text.literal("Play electrical hazard warnings."), () -> SettingsManager.shockDamageEnabled, value -> SettingsManager.shockDamageEnabled = value),
                                new ConfigToggle(Text.literal("Chemical Damage Alerts"), Text.literal("Enable chemical exposure notifications."), () -> SettingsManager.chemicalDamageEnabled, value -> SettingsManager.chemicalDamageEnabled = value),
                                new ConfigToggle(Text.literal("Morphine Lines"), Text.literal("Allow morphine administration announcements."), () -> SettingsManager.morphineEnabled, value -> SettingsManager.morphineEnabled = value),
                                new ConfigToggle(Text.literal("Health Critical 2 Lines"), Text.literal("Play the alternate critical health voice lines."), () -> SettingsManager.healthCritical2Enabled, value -> SettingsManager.healthCritical2Enabled = value),
                                new ConfigToggle(Text.literal("Seek Medical Lines"), Text.literal("Remind the player to seek medical attention when needed."), () -> SettingsManager.seekMedicalEnabled, value -> SettingsManager.seekMedicalEnabled = value),
                                new ConfigToggle(Text.literal("Health Critical Lines"), Text.literal("Use the primary critical health announcements."), () -> SettingsManager.healthCriticalEnabled, value -> SettingsManager.healthCriticalEnabled = value),
                                new ConfigToggle(Text.literal("Near Death Lines"), Text.literal("Play near-death warnings when health is almost gone."), () -> SettingsManager.nearDeathEnabled, value -> SettingsManager.nearDeathEnabled = value)
                        ),
                        false
                )
        );
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();
        this.tooltips.clear();
        this.colorFieldLabels.clear();
        this.primaryColorField = null;
        this.secondaryColorField = null;
        this.clearStatus();
        if (this.currentSection == null) {
            initSectionSelection();
        } else {
            initToggleView(this.currentSection);
        }
    }

    private void initSectionSelection() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int verticalSpacing = 26;
        int startY = 70;
        int startX = (this.width - buttonWidth) / 2;

        for (int index = 0; index < this.sections.size(); index++) {
            ConfigSection section = this.sections.get(index);
            int y = startY + index * verticalSpacing;
            ButtonWidget button = ButtonWidget.builder(section.title(), press -> openSection(section))
                    .dimensions(startX, y, buttonWidth, buttonHeight)
                    .build();
            this.addDrawableChild(button);
            registerTooltip(button, section.description());
        }

        ButtonWidget doneButton = ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(this.width / 2 - 80, this.height - 32, 160, 20)
                .build();
        this.addDrawableChild(doneButton);
    }

    private void initToggleView(ConfigSection section) {
        int columns = 2;
        int buttonWidth = 160;
        int buttonHeight = 20;
        int verticalSpacing = 24;
        int horizontalSpacing = 12;
        int totalGridWidth = columns * buttonWidth + (columns - 1) * horizontalSpacing;
        int startX = (this.width - totalGridWidth) / 2;
        int startY = 70;
        int contentBottom = startY;

        List<ConfigToggle> toggles = section.toggles();
        for (int index = 0; index < toggles.size(); index++) {
            ConfigToggle toggle = toggles.get(index);
            int column = index % columns;
            int row = index / columns;
            int x = startX + column * (buttonWidth + horizontalSpacing);
            int y = startY + row * verticalSpacing;

            CyclingButtonWidget<Boolean> button = CyclingButtonWidget.onOffBuilder()
                    .initially(toggle.get())
                    .build(x, y, buttonWidth, buttonHeight, toggle.label(), (widget, value) -> {
                        toggle.set(value);
                        SettingsManager.saveSettings();
                    });
            this.addDrawableChild(button);
            registerTooltip(button, toggle.tooltip());
            contentBottom = Math.max(contentBottom, y + buttonHeight);
        }

        if (section.hasColorControls()) {
            int colorStartY = contentBottom;
            if (!toggles.isEmpty()) {
                colorStartY += 24;
            } else {
                colorStartY += 8;
            }
            initColorControls(colorStartY);
        }

        int navigationY = this.height - 32;
        ButtonWidget backButton = ButtonWidget.builder(Text.translatable("gui.back"), button -> {
            this.currentSection = null;
            this.init();
        }).dimensions(this.width / 2 - 160, navigationY, 120, 20).build();
        this.addDrawableChild(backButton);

        ButtonWidget doneButton = ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(this.width / 2 + 40, navigationY, 120, 20)
                .build();
        this.addDrawableChild(doneButton);
    }

    private void initColorControls(int startY) {
        int fieldWidth = 120;
        int buttonWidth = 90;
        int spacing = 8;
        int rowSpacing = 28;
        int totalWidth = fieldWidth + spacing + buttonWidth;
        int startX = (this.width - totalWidth) / 2;
        int y = startY;

        this.primaryColorField = new TextFieldWidget(this.textRenderer, startX, y, fieldWidth, 20, Text.literal("Primary Color"));
        this.primaryColorField.setMaxLength(7);
        this.primaryColorField.setTextPredicate(this::isColorInput);
        this.primaryColorField.setPlaceholder(Text.literal("#RRGGBB"));
        this.primaryColorField.setText(formatColor(SettingsManager.hudPrimaryColor));
        this.primaryColorField.setChangedListener(value -> clearStatus());
        this.primaryColorField.setEditableColor(0xFFFFFFFF);
        this.addDrawableChild(this.primaryColorField);
        this.colorFieldLabels.put(this.primaryColorField, Text.literal("Primary Color"));
        registerTooltip(this.primaryColorField, Text.literal("Enter a hex color like #FFAA00 to update the HUD's primary accent."));

        ButtonWidget applyPrimary = ButtonWidget.builder(Text.literal("Apply"), button -> applyPrimaryColor())
                .dimensions(startX + fieldWidth + spacing, y, buttonWidth, 20)
                .build();
        this.addDrawableChild(applyPrimary);
        registerTooltip(applyPrimary, Text.literal("Apply the entered primary accent color."));

        y += rowSpacing;

        this.secondaryColorField = new TextFieldWidget(this.textRenderer, startX, y, fieldWidth, 20, Text.literal("Secondary Color"));
        this.secondaryColorField.setMaxLength(7);
        this.secondaryColorField.setTextPredicate(this::isColorInput);
        this.secondaryColorField.setPlaceholder(Text.literal("#RRGGBB"));
        this.secondaryColorField.setText(formatColor(SettingsManager.hudSecondaryColor));
        this.secondaryColorField.setChangedListener(value -> clearStatus());
        this.secondaryColorField.setEditableColor(0xFFFFFFFF);
        this.addDrawableChild(this.secondaryColorField);
        this.colorFieldLabels.put(this.secondaryColorField, Text.literal("Secondary Color"));
        registerTooltip(this.secondaryColorField, Text.literal("Enter a hex color like #D97F00 for the HUD's secondary accent."));

        ButtonWidget applySecondary = ButtonWidget.builder(Text.literal("Apply"), button -> applySecondaryColor())
                .dimensions(startX + fieldWidth + spacing, y, buttonWidth, 20)
                .build();
        this.addDrawableChild(applySecondary);
        registerTooltip(applySecondary, Text.literal("Apply the entered secondary accent color."));

        y += rowSpacing;

        ButtonWidget syncButton = ButtonWidget.builder(Text.literal("Sync Secondary to Primary"), button -> syncColorsFromPrimary())
                .dimensions(startX, y, totalWidth, 20)
                .build();
        this.addDrawableChild(syncButton);
        registerTooltip(syncButton, Text.literal("Set both colors using the primary value and auto-calculate a darker secondary."));
    }

    private void openSection(ConfigSection section) {
        this.currentSection = section;
        this.init();
    }

    private void registerTooltip(ClickableWidget widget, Text tooltip) {
        if (tooltip != null && widget != null) {
            this.tooltips.put(widget, tooltip);
        }
    }

    private void applyPrimaryColor() {
        if (this.primaryColorField == null) {
            return;
        }
        Integer color = parseHexColor(this.primaryColorField.getText());
        if (color == null) {
            this.primaryColorField.setEditableColor(0xFFFF5555);
            showStatus(Text.literal("Invalid primary hex color. Use #RRGGBB."), 0xFF5555);
            return;
        }
        this.primaryColorField.setEditableColor(0xFFFFFFFF);
        SettingsManager.hudPrimaryColor = color;
        SettingsManager.saveSettings();
        this.primaryColorField.setText(formatColor(color));
        showStatus(Text.literal("Primary HUD color updated."), color);
    }

    private void applySecondaryColor() {
        if (this.secondaryColorField == null) {
            return;
        }
        Integer color = parseHexColor(this.secondaryColorField.getText());
        if (color == null) {
            this.secondaryColorField.setEditableColor(0xFFFF5555);
            showStatus(Text.literal("Invalid secondary hex color. Use #RRGGBB."), 0xFF5555);
            return;
        }
        this.secondaryColorField.setEditableColor(0xFFFFFFFF);
        SettingsManager.hudSecondaryColor = color;
        SettingsManager.saveSettings();
        this.secondaryColorField.setText(formatColor(color));
        showStatus(Text.literal("Secondary HUD color updated."), color);
    }

    private void syncColorsFromPrimary() {
        if (this.primaryColorField == null || this.secondaryColorField == null) {
            return;
        }
        Integer color = parseHexColor(this.primaryColorField.getText());
        if (color == null) {
            this.primaryColorField.setEditableColor(0xFFFF5555);
            showStatus(Text.literal("Enter a valid primary hex color before syncing."), 0xFF5555);
            return;
        }
        this.primaryColorField.setEditableColor(0xFFFFFFFF);
        int primary = color;
        int secondary = SettingsManager.calculateDarkerShade(primary);
        SettingsManager.hudPrimaryColor = primary;
        SettingsManager.hudSecondaryColor = secondary;
        SettingsManager.saveSettings();
        this.primaryColorField.setText(formatColor(primary));
        this.secondaryColorField.setText(formatColor(secondary));
        this.secondaryColorField.setEditableColor(0xFFFFFFFF);
        showStatus(Text.literal("HUD colors synced from primary."), primary);
    }

    private boolean isColorInput(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        String trimmed = value.startsWith("#") ? value.substring(1) : value;
        if (trimmed.length() > 6) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean hex = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private Integer parseHexColor(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!HEX_COLOR_PATTERN.matcher(trimmed).matches()) {
            return null;
        }
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        }
        try {
            return 0xFF000000 | Integer.parseInt(trimmed, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String formatColor(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

    private void showStatus(Text message, int rgbColor) {
        this.statusMessage = message;
        this.statusMessageColor = 0xFF000000 | (rgbColor & 0xFFFFFF);
        this.statusMessageTicks = 100;
    }

    private void drawColorPreview(DrawContext context, TextFieldWidget field, int color) {
        int size = 10;
        int x = field.getX() - size - 6;
        int y = field.getY() + (field.getHeight() - size) / 2;
        int argb = 0xFF000000 | (color & 0xFFFFFF);
        context.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        context.fill(x, y, x + size, y + size, argb);
    }

    private void clearStatus() {
        this.statusMessage = null;
        this.statusMessageTicks = 0;
        if (this.primaryColorField != null) {
            this.primaryColorField.setEditableColor(0xFFFFFFFF);
        }
        if (this.secondaryColorField != null) {
            this.secondaryColorField.setEditableColor(0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        SettingsManager.saveSettings();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 25, SettingsManager.hudPrimaryColor);

        if (this.currentSection == null) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("hev_suit.config.subtitle"), this.width / 2, 44, 0xFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, this.currentSection.title(), this.width / 2, 44, 0xFFFFFF);
            Text description = this.currentSection.description();
            if (description != null) {
                context.drawCenteredTextWithShadow(this.textRenderer, description, this.width / 2, 58, 0xA0A0A0);
            }
        }

        if (this.currentSection != null && this.currentSection.hasColorControls()) {
            for (Map.Entry<TextFieldWidget, Text> entry : this.colorFieldLabels.entrySet()) {
                TextFieldWidget field = entry.getKey();
                if (field != null) {
                    context.drawTextWithShadow(this.textRenderer, entry.getValue(), field.getX(), field.getY() - 10, 0xFFFFFF);
                }
            }
            if (this.primaryColorField != null) {
                drawColorPreview(context, this.primaryColorField, SettingsManager.hudPrimaryColor);
            }
            if (this.secondaryColorField != null) {
                drawColorPreview(context, this.secondaryColorField, SettingsManager.hudSecondaryColor);
            }
        }

        for (Map.Entry<ClickableWidget, Text> entry : this.tooltips.entrySet()) {
            ClickableWidget widget = entry.getKey();
            if (widget.isMouseOver(mouseX, mouseY)) {
                context.drawTooltip(this.textRenderer, entry.getValue(), mouseX, mouseY);
                break;
            }
        }

        if (this.statusMessage != null && this.statusMessageTicks > 0) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.statusMessage, this.width / 2, this.height - 52, this.statusMessageColor);
            this.statusMessageTicks--;
            if (this.statusMessageTicks <= 0) {
                this.statusMessage = null;
            }
        }
    }

    private record ConfigToggle(Text label, Text tooltip, BooleanSupplier getter, Consumer<Boolean> setter) {
        boolean get() {
            return getter.getAsBoolean();
        }

        void set(boolean value) {
            setter.accept(value);
        }
    }

    private record ConfigSection(Text title, Text description, List<ConfigToggle> toggles, boolean hasColorControls) { }
}
