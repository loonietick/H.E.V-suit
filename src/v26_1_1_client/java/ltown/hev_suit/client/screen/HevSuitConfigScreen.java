package ltown.hev_suit.client.screen;

import ltown.hev_suit.client.managers.SettingsManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Deque;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class HevSuitConfigScreen extends Screen {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#?([0-9A-Fa-f]{6})$");

    private final Screen parent;
    private final List<ConfigSection> sections;
    private final Map<AbstractWidget, Component> tooltips = new HashMap<>();
    private final Deque<ConfigSection> sectionStack = new ArrayDeque<>();
    private ConfigSection currentSection;
    private EditBox primaryColorField;
    private EditBox secondaryColorField;
    private Component statusMessage;
    private int statusMessageColor = 0xFFFFFFFF;
    private int statusMessageTicks;

    public HevSuitConfigScreen(Screen parent) {
        super(Component.translatable("hev_suit.config.title"));
        this.parent = parent;
        ConfigSection healthAlertsSubmenu = new ConfigSection(
                Component.literal("Health Alerts"),
                Component.literal("Control which health-related voice lines play."),
                List.of(
                        new ConfigToggle(Component.literal("Health Alerts"), Component.literal("Enable general health warning lines."), () -> SettingsManager.healthAlertsEnabled, value -> SettingsManager.healthAlertsEnabled = value),
                        new ConfigToggle(Component.literal("Health Critical 2 Lines"), Component.literal("Play the alternate critical health voice lines."), () -> SettingsManager.healthCritical2Enabled, value -> SettingsManager.healthCritical2Enabled = value),
                        new ConfigToggle(Component.literal("Seek Medical Lines"), Component.literal("Remind the player to seek medical attention when needed."), () -> SettingsManager.seekMedicalEnabled, value -> SettingsManager.seekMedicalEnabled = value),
                        new ConfigToggle(Component.literal("Health Critical Lines"), Component.literal("Use the primary critical health announcements."), () -> SettingsManager.healthCriticalEnabled, value -> SettingsManager.healthCriticalEnabled = value),
                        new ConfigToggle(Component.literal("Near Death Lines"), Component.literal("Play near-death warnings when health is almost gone."), () -> SettingsManager.nearDeathEnabled, value -> SettingsManager.nearDeathEnabled = value),
                        new ConfigToggle(Component.literal("Insufficient Medical Supplies"), Component.literal("Warn when health is critical and no healing items are available."), () -> SettingsManager.insufficientMedicalEnabled, value -> SettingsManager.insufficientMedicalEnabled = value),
                        new ConfigToggle(Component.literal("Administering Medical Attention"), Component.literal("Play totem-triggered administering medical lines."), () -> SettingsManager.administeringMedicalEnabled, value -> SettingsManager.administeringMedicalEnabled = value),
                        new ConfigToggle(Component.literal("Death SFX"), Component.literal("Play the flatline when you die."), () -> SettingsManager.deathSfxEnabled, value -> SettingsManager.deathSfxEnabled = value),
                        new ConfigToggle(Component.literal("Internal Bleeding Alerts"), Component.literal("Play internal bleeding warnings after severe explosive damage."), () -> SettingsManager.internalBleedingEnabled, value -> SettingsManager.internalBleedingEnabled = value)
                ),
                SectionContent.NONE,
                List.of(),
                SettingsManager::resetHealthAlerts
        );

        ConfigSection weaponAlertsSubmenu = new ConfigSection(
                Component.literal("Weapon Alerts"),
                Component.literal("Configure weapon pickup alerts."),
                List.of(
                        new ConfigToggle(Component.literal("Weapon Acquisition Alerts"), Component.literal("Play a notification when picking up a configured weapon."), () -> SettingsManager.weaponPickupEnabled, value -> SettingsManager.weaponPickupEnabled = value),
                        new ConfigToggle(Component.literal("Ammunition Depletion Alerts"), Component.literal("Warn when your held ammo stack is consumed."), () -> SettingsManager.ammoDepletedEnabled, value -> SettingsManager.ammoDepletedEnabled = value)
                ),
                SectionContent.NONE,
                List.of(),
                SettingsManager::resetWeaponAlerts
        );

        this.sections = List.of(
                new ConfigSection(
                        Component.literal("Main Toggles"),
                        Component.literal("Core HEV functionality and accessibility toggles."),
                        List.of(
                                new ConfigToggle(Component.literal("Enabled"), Component.literal("Turn every single feature on or off."), () -> SettingsManager.hevSuitEnabled, value -> SettingsManager.hevSuitEnabled = value),
                                new ConfigToggle(Component.literal("PVP Mode"), Component.literal("Minimize non essential alerts."), () -> SettingsManager.pvpModeEnabled, value -> SettingsManager.pvpModeEnabled = value),
                                new ConfigToggle(Component.literal("Captions"), Component.literal("Display subtitles."), () -> SettingsManager.captionsEnabled, value -> SettingsManager.captionsEnabled = value)
                        ),
                        SectionContent.NONE,
                        List.of(),
                        SettingsManager::resetMainToggles
                ),
                new ConfigSection(
                        Component.literal("HUD"),
                        Component.literal("Enable or disable in-world overlays and debug helpers."),
                        List.of(
                                new ConfigToggle(Component.literal("Enabled"), Component.literal("Toggle every HEV HUD element on screen."), () -> SettingsManager.hudEnabled, value -> SettingsManager.hudEnabled = value),
                                new ConfigToggle(Component.literal("HUD Health"), Component.literal("Show your health converted to 100 to 0 on screen"), () -> SettingsManager.hudHealthEnabled, value -> SettingsManager.hudHealthEnabled = value),
                                new ConfigToggle(Component.literal("HUD Armor"), Component.literal("Show your armor durability and protective value on screen."), () -> SettingsManager.hudArmorEnabled, value -> SettingsManager.hudArmorEnabled = value),
                                new ConfigToggle(Component.literal("HUD Ammo"), Component.literal("Show the amount of blocks you are holding and how much is in your inventory."), () -> SettingsManager.hudAmmoEnabled, value -> SettingsManager.hudAmmoEnabled = value),
                                new ConfigToggle(Component.literal("Damage Indicators"), Component.literal("Show directional hit indicators when you take damage."), () -> SettingsManager.damageIndicatorsEnabled, value -> SettingsManager.damageIndicatorsEnabled = value),
                                new ConfigToggle(Component.literal("Threat Indicators"), Component.literal("Show directional indicators pointing to where hostile entitys are."), () -> SettingsManager.threatIndicatorsEnabled, value -> SettingsManager.threatIndicatorsEnabled = value)
                        ),
                        SectionContent.NONE,
                        List.of(),
                        SettingsManager::resetHudToggles
                ),
                new ConfigSection(
                        Component.literal("HUD Colors"),
                        Component.literal("Adjust HUD colors using hex values (e.g. #FFAA00)."),
                        List.of(),
                        SectionContent.HUD_COLORS,
                        List.of(),
                        SettingsManager::resetHudColors
                ),
                new ConfigSection(
                        Component.literal("Audible Alerts"),
                        Component.literal("Choose which suit voice alerts are allowed to play."),
                        List.of(
                                new ConfigToggle(Component.literal("Fracture Alerts"), Component.literal("Play fracture detection voice lines."), () -> SettingsManager.fracturesEnabled, value -> SettingsManager.fracturesEnabled = value),
                                new ConfigToggle(Component.literal("Blood Loss Alerts"), Component.literal("Trigger blood-loss specific warnings."), () -> SettingsManager.bloodLossEnabled, value -> SettingsManager.bloodLossEnabled = value),
                                new ConfigToggle(Component.literal("Morphine Lines"), Component.literal("Allow morphine administration announcements."), () -> SettingsManager.morphineEnabled, value -> SettingsManager.morphineEnabled = value),
                                new ConfigToggle(Component.literal("Armor Durability Alerts"), Component.literal("Warn when armor pieces are close to breaking."), () -> SettingsManager.armorDurabilityEnabled, value -> SettingsManager.armorDurabilityEnabled = value),
                                new ConfigToggle(Component.literal("Heat Damage Alerts"), Component.literal("Announce when the suit detects fire or lava damage."), () -> SettingsManager.heatDamageEnabled, value -> SettingsManager.heatDamageEnabled = value),
                                new ConfigToggle(Component.literal("Shock Damage Alerts"), Component.literal("Play electrical hazard warnings."), () -> SettingsManager.shockDamageEnabled, value -> SettingsManager.shockDamageEnabled = value),
                                new ConfigToggle(Component.literal("Chemical Damage Alerts"), Component.literal("Enable chemical exposure notifications."), () -> SettingsManager.chemicalDamageEnabled, value -> SettingsManager.chemicalDamageEnabled = value),
                                new ConfigToggle(Component.literal("HEV Damage Alerts"), Component.literal("Play damage callouts as your armor degrades."), () -> SettingsManager.hevDamageEnabled, value -> SettingsManager.hevDamageEnabled = value),
                                new ConfigToggle(Component.literal("Power Armor Overload"), Component.literal("Alert when elytra armor overloads."), () -> SettingsManager.powerArmorOverloadEnabled, value -> SettingsManager.powerArmorOverloadEnabled = value),
                                new ConfigToggle(Component.literal("HEV Logon"), Component.literal("Play the HEV logon when equipping HEV chestplates."), () -> SettingsManager.hevLogonEnabled, value -> SettingsManager.hevLogonEnabled = value),
                                new ConfigToggle(Component.literal("Elytra Equip SFX"), Component.literal("Play the power-move sound when equipping elytra."), () -> SettingsManager.elytraEquipSfxEnabled, value -> SettingsManager.elytraEquipSfxEnabled = value),
                                new ConfigToggle(Component.literal("Radiation Alerts"), Component.literal("Enable Geiger counter effects inside basalt deltas."), () -> SettingsManager.radiationSfxEnabled, value -> SettingsManager.radiationSfxEnabled = value)
                        ),
                        SectionContent.NONE,
                        List.of(healthAlertsSubmenu, weaponAlertsSubmenu),
                        SettingsManager::resetAudibleAlerts
                )
        );
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.tooltips.clear();
        this.primaryColorField = null;
        this.secondaryColorField = null;
        this.clearStatus();
        if (this.currentSection == null) {
            this.sectionStack.clear();
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
            Button button = Button.builder(section.title(), press -> openRootSection(section))
                    .bounds(startX, y, buttonWidth, buttonHeight)
                    .build();
            this.addRenderableWidget(button);
            registerTooltip(button, section.description());
        }

        Button doneButton = Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 - 80, this.height - 32, 160, 20)
                .build();
        this.addRenderableWidget(doneButton);
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

        int slotIndex = 0;
        List<ConfigToggle> toggles = section.toggles();
        for (ConfigToggle toggle : toggles) {
            int column = slotIndex % columns;
            int row = slotIndex / columns;
            int x = startX + column * (buttonWidth + horizontalSpacing);
            int y = startY + row * verticalSpacing;

            CycleButton<Boolean> button = CycleButton.onOffBuilder(toggle.get())
                    .create(x, y, buttonWidth, buttonHeight, toggle.label(), (widget, value) -> {
                        toggle.set(value);
                        SettingsManager.saveSettings();
                    });
            this.addRenderableWidget(button);
            registerTooltip(button, toggle.tooltip());
            contentBottom = Math.max(contentBottom, y + buttonHeight);
            slotIndex++;
        }

        List<ConfigSection> childSections = section.children();
        if (!childSections.isEmpty()) {
            int childY = contentBottom;
            if (!toggles.isEmpty()) {
                childY += 16;
            }
            for (ConfigSection child : childSections) {
                Button submenuButton = Button.builder(child.title(), button -> openChildSection(section, child))
                        .bounds(startX, childY, totalGridWidth, buttonHeight)
                        .build();
                this.addRenderableWidget(submenuButton);
                registerTooltip(submenuButton, child.description());
                childY += buttonHeight + 6;
                contentBottom = Math.max(contentBottom, childY);
            }
        }

        if (section.content() == SectionContent.HUD_COLORS) {
            int colorStartY = contentBottom;
            if (!toggles.isEmpty()) {
                colorStartY += 24;
            } else {
                colorStartY += 8;
            }
            int colorsBottom = initColorControls(colorStartY);
            contentBottom = Math.max(contentBottom, colorsBottom);
        }

        if (section.resetAction() != null) {
            int resetWidth = Math.min(200, totalGridWidth);
            int resetX = (this.width - resetWidth) / 2;
            int resetY = Math.min(this.height - 64, contentBottom + 24);
            Button resetButton = Button.builder(Component.literal("Reset Section"), button -> {
                        section.resetAction().run();
                        SettingsManager.saveSettings();
                        Component message = Component.literal(section.title().getString() + " reset to defaults.");
                        this.init();
                        showStatus(message, SettingsManager.hudPrimaryColor);
                    })
                    .bounds(resetX, resetY, resetWidth, 20)
                    .build();
            this.addRenderableWidget(resetButton);
            registerTooltip(resetButton, Component.literal("Restore the default values for this section."));
            contentBottom = Math.max(contentBottom, resetY + 20);
        }

        int navigationY = this.height - 32;
        Button backButton = Button.builder(Component.translatable("gui.back"), button -> {
            if (!this.sectionStack.isEmpty()) {
                this.currentSection = this.sectionStack.pop();
            } else {
                this.currentSection = null;
            }
            this.init();
        }).bounds(this.width / 2 - 160, navigationY, 120, 20).build();
        this.addRenderableWidget(backButton);

        Button doneButton = Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 + 40, navigationY, 120, 20)
                .build();
        this.addRenderableWidget(doneButton);
    }

    private int initColorControls(int startY) {
        int fieldWidth = 120;
        int buttonWidth = 90;
        int spacing = 8;
        int rowSpacing = 28;
        int totalWidth = fieldWidth + spacing + buttonWidth;
        int startX = (this.width - totalWidth) / 2;
        int y = startY;

        this.primaryColorField = new EditBox(this.font, startX, y, fieldWidth, 20, Component.literal("Primary Color"));
        this.primaryColorField.setMaxLength(7);
        this.primaryColorField.setHint(Component.literal("#RRGGBB"));
        this.primaryColorField.setValue(formatColor(SettingsManager.hudPrimaryColor));
        this.primaryColorField.setResponder(value -> clearStatus());
        this.primaryColorField.setTextColor(0xFFFFFFFF);
        this.addRenderableWidget(this.primaryColorField);
        registerTooltip(this.primaryColorField, Component.literal("Enter a hex color like #FFAA00 to update the HUD's primary accent."));

        Button applyPrimary = Button.builder(Component.literal("Apply"), button -> applyPrimaryColor())
                .bounds(startX + fieldWidth + spacing, y, buttonWidth, 20)
                .build();
        this.addRenderableWidget(applyPrimary);
        registerTooltip(applyPrimary, Component.literal("Apply the entered primary accent color."));

        y += rowSpacing;

        this.secondaryColorField = new EditBox(this.font, startX, y, fieldWidth, 20, Component.literal("Secondary Color"));
        this.secondaryColorField.setMaxLength(7);
        this.secondaryColorField.setHint(Component.literal("#RRGGBB"));
        this.secondaryColorField.setValue(formatColor(SettingsManager.hudSecondaryColor));
        this.secondaryColorField.setResponder(value -> clearStatus());
        this.secondaryColorField.setTextColor(0xFFFFFFFF);
        this.addRenderableWidget(this.secondaryColorField);
        registerTooltip(this.secondaryColorField, Component.literal("Enter a hex color like #D97F00 for the HUD's secondary accent."));

        Button applySecondary = Button.builder(Component.literal("Apply"), button -> applySecondaryColor())
                .bounds(startX + fieldWidth + spacing, y, buttonWidth, 20)
                .build();
        this.addRenderableWidget(applySecondary);
        registerTooltip(applySecondary, Component.literal("Apply the entered secondary accent color."));

        y += rowSpacing;

        Button syncButton = Button.builder(Component.literal("Sync Secondary to Primary"), button -> syncColorsFromPrimary())
                .bounds(startX, y, totalWidth, 20)
                .build();
        this.addRenderableWidget(syncButton);
        registerTooltip(syncButton, Component.literal("Set both colors using the primary value and auto-calculate a darker secondary."));

        return y + 20;
    }

    private void openSection(ConfigSection section) {
        this.currentSection = section;
        this.init();
    }

    private void openRootSection(ConfigSection section) {
        this.sectionStack.clear();
        openSection(section);
    }

    private void openChildSection(ConfigSection parent, ConfigSection child) {
        this.sectionStack.push(parent);
        openSection(child);
    }

    private void registerTooltip(AbstractWidget widget, Component tooltip) {
        if (tooltip != null && widget != null) {
            this.tooltips.put(widget, tooltip);
        }
    }

    private void applyPrimaryColor() {
        if (this.primaryColorField == null) {
            return;
        }
        Integer color = parseHexColor(this.primaryColorField.getValue());
        if (color == null) {
            this.primaryColorField.setTextColor(0xFFFF5555);
            showStatus(Component.literal("Invalid primary hex color. Use #RRGGBB."), 0xFF5555);
            return;
        }
        this.primaryColorField.setTextColor(0xFFFFFFFF);
        SettingsManager.hudPrimaryColor = color;
        SettingsManager.saveSettings();
        this.primaryColorField.setValue(formatColor(color));
        showStatus(Component.literal("Primary HUD color updated."), color);
    }

    private void applySecondaryColor() {
        if (this.secondaryColorField == null) {
            return;
        }
        Integer color = parseHexColor(this.secondaryColorField.getValue());
        if (color == null) {
            this.secondaryColorField.setTextColor(0xFFFF5555);
            showStatus(Component.literal("Invalid secondary hex color. Use #RRGGBB."), 0xFF5555);
            return;
        }
        this.secondaryColorField.setTextColor(0xFFFFFFFF);
        SettingsManager.hudSecondaryColor = color;
        SettingsManager.saveSettings();
        this.secondaryColorField.setValue(formatColor(color));
        showStatus(Component.literal("Secondary HUD color updated."), color);
    }

    private void syncColorsFromPrimary() {
        if (this.primaryColorField == null || this.secondaryColorField == null) {
            return;
        }
        Integer color = parseHexColor(this.primaryColorField.getValue());
        if (color == null) {
            this.primaryColorField.setTextColor(0xFFFF5555);
            showStatus(Component.literal("Enter a valid primary hex color before syncing."), 0xFF5555);
            return;
        }
        this.primaryColorField.setTextColor(0xFFFFFFFF);
        int primary = color;
        int secondary = SettingsManager.calculateDarkerShade(primary);
        SettingsManager.hudPrimaryColor = primary;
        SettingsManager.hudSecondaryColor = secondary;
        SettingsManager.saveSettings();
        this.primaryColorField.setValue(formatColor(primary));
        this.secondaryColorField.setValue(formatColor(secondary));
        this.secondaryColorField.setTextColor(0xFFFFFFFF);
        showStatus(Component.literal("HUD colors synced from primary."), primary);
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

    private void showStatus(Component message, int rgbColor) {
        this.statusMessage = message;
        this.statusMessageColor = 0xFF000000 | (rgbColor & 0xFFFFFF);
        this.statusMessageTicks = 100;
    }

    private void drawColorPreview(GuiGraphicsExtractor context, EditBox field, int color) {
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
            this.primaryColorField.setTextColor(0xFFFFFFFF);
        }
        if (this.secondaryColorField != null) {
            this.secondaryColorField.setTextColor(0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        SettingsManager.saveSettings();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 25, SettingsManager.hudPrimaryColor);

        if (this.currentSection == null) {
            context.centeredText(this.font, Component.translatable("hev_suit.config.subtitle"), this.width / 2, 44, 0xFFFFFF);
        } else {
            context.centeredText(this.font, this.currentSection.title(), this.width / 2, 44, 0xFFFFFF);
            Component description = this.currentSection.description();
            if (description != null) {
                context.centeredText(this.font, description, this.width / 2, 58, 0xA0A0A0);
            }
        }

        if (this.currentSection != null && this.currentSection.content() == SectionContent.HUD_COLORS) {
            if (this.primaryColorField != null) {
                drawColorPreview(context, this.primaryColorField, SettingsManager.hudPrimaryColor);
            }
            if (this.secondaryColorField != null) {
                drawColorPreview(context, this.secondaryColorField, SettingsManager.hudSecondaryColor);
            }
        }

        for (Map.Entry<AbstractWidget, Component> entry : this.tooltips.entrySet()) {
            AbstractWidget widget = entry.getKey();
            if (widget.isMouseOver(mouseX, mouseY)) {
                context.setTooltipForNextFrame(this.font, entry.getValue(), mouseX, mouseY);
                break;
            }
        }

        if (this.statusMessage != null && this.statusMessageTicks > 0) {
            context.centeredText(this.font, this.statusMessage, this.width / 2, this.height - 52, this.statusMessageColor);
            this.statusMessageTicks--;
            if (this.statusMessageTicks <= 0) {
                this.statusMessage = null;
            }
        }
    }

    private record ConfigToggle(Component label, Component tooltip, BooleanSupplier getter, Consumer<Boolean> setter) {
        boolean get() {
            return getter.getAsBoolean();
        }

        void set(boolean value) {
            setter.accept(value);
        }
    }

    private record ConfigSection(Component title, Component description, List<ConfigToggle> toggles, SectionContent content, List<ConfigSection> children, Runnable resetAction) { }

    private enum SectionContent {
        NONE,
        HUD_COLORS
    }
}
