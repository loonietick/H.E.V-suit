package ltown.hev_suit.client.screen;

import ltown.hev_suit.client.managers.SettingsManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class HevSuitConfigScreen extends Screen {
    private final Screen parent;
    private final List<ToggleEntry> toggleEntries;

    public HevSuitConfigScreen(Screen parent) {
        super(Text.translatable("hev_suit.config.title"));
        this.parent = parent;
        this.toggleEntries = List.of(
                new ToggleEntry(Text.literal("HEV Suit"), () -> SettingsManager.hevSuitEnabled, value -> SettingsManager.hevSuitEnabled = value),
                new ToggleEntry(Text.literal("PVP Mode"), () -> SettingsManager.pvpModeEnabled, value -> SettingsManager.pvpModeEnabled = value),
                new ToggleEntry(Text.literal("HUD (All)"), () -> SettingsManager.hudEnabled, value -> SettingsManager.hudEnabled = value),
                new ToggleEntry(Text.literal("HUD Health"), () -> SettingsManager.hudHealthEnabled, value -> SettingsManager.hudHealthEnabled = value),
                new ToggleEntry(Text.literal("HUD Armor"), () -> SettingsManager.hudArmorEnabled, value -> SettingsManager.hudArmorEnabled = value),
                new ToggleEntry(Text.literal("HUD Ammo"), () -> SettingsManager.hudAmmoEnabled, value -> SettingsManager.hudAmmoEnabled = value),
                new ToggleEntry(Text.literal("HUD Alignment Mode"), () -> SettingsManager.hudAlignmentMode, value -> SettingsManager.hudAlignmentMode = value),
                new ToggleEntry(Text.literal("Damage Indicators"), () -> SettingsManager.damageIndicatorsEnabled, value -> SettingsManager.damageIndicatorsEnabled = value),
                new ToggleEntry(Text.literal("Threat Indicators"), () -> SettingsManager.threatIndicatorsEnabled, value -> SettingsManager.threatIndicatorsEnabled = value),
                new ToggleEntry(Text.literal("Health Alerts"), () -> SettingsManager.healthAlertsEnabled, value -> SettingsManager.healthAlertsEnabled = value),
                new ToggleEntry(Text.literal("Armor Durability Alerts"), () -> SettingsManager.armorDurabilityEnabled, value -> SettingsManager.armorDurabilityEnabled = value),
                new ToggleEntry(Text.literal("Fracture Alerts"), () -> SettingsManager.fracturesEnabled, value -> SettingsManager.fracturesEnabled = value),
                new ToggleEntry(Text.literal("Heat Damage Alerts"), () -> SettingsManager.heatDamageEnabled, value -> SettingsManager.heatDamageEnabled = value),
                new ToggleEntry(Text.literal("Blood Loss Alerts"), () -> SettingsManager.bloodLossEnabled, value -> SettingsManager.bloodLossEnabled = value),
                new ToggleEntry(Text.literal("Shock Damage Alerts"), () -> SettingsManager.shockDamageEnabled, value -> SettingsManager.shockDamageEnabled = value),
                new ToggleEntry(Text.literal("Chemical Damage Alerts"), () -> SettingsManager.chemicalDamageEnabled, value -> SettingsManager.chemicalDamageEnabled = value),
                new ToggleEntry(Text.literal("Morphine Lines"), () -> SettingsManager.morphineEnabled, value -> SettingsManager.morphineEnabled = value),
                new ToggleEntry(Text.literal("Health Critical 2 Lines"), () -> SettingsManager.healthCritical2Enabled, value -> SettingsManager.healthCritical2Enabled = value),
                new ToggleEntry(Text.literal("Seek Medical Lines"), () -> SettingsManager.seekMedicalEnabled, value -> SettingsManager.seekMedicalEnabled = value),
                new ToggleEntry(Text.literal("Health Critical Lines"), () -> SettingsManager.healthCriticalEnabled, value -> SettingsManager.healthCriticalEnabled = value),
                new ToggleEntry(Text.literal("Near Death Lines"), () -> SettingsManager.nearDeathEnabled, value -> SettingsManager.nearDeathEnabled = value),
                new ToggleEntry(Text.literal("Black Mesa SFX"), () -> SettingsManager.useBlackMesaSFX, value -> SettingsManager.useBlackMesaSFX = value),
                new ToggleEntry(Text.literal("Captions"), () -> SettingsManager.captionsEnabled, value -> SettingsManager.captionsEnabled = value)
        );
    }

    @Override
    protected void init() {
        super.init();
        int columns = 2;
        int buttonWidth = 160;
        int buttonHeight = 20;
        int verticalSpacing = 24;
        int horizontalSpacing = 12;
        int totalGridWidth = columns * buttonWidth + (columns - 1) * horizontalSpacing;
        int startX = (this.width - totalGridWidth) / 2;
        int startY = 60;

        for (int index = 0; index < toggleEntries.size(); index++) {
            ToggleEntry entry = toggleEntries.get(index);
            int column = index % columns;
            int row = index / columns;
            int x = startX + column * (buttonWidth + horizontalSpacing);
            int y = startY + row * verticalSpacing;

            CyclingButtonWidget<Boolean> button = CyclingButtonWidget.onOffBuilder()
                    .initially(entry.get())
                    .build(x, y, buttonWidth, buttonHeight, entry.label(), (widget, value) -> {
                        entry.set(value);
                        SettingsManager.saveSettings();
                    });
            this.addDrawableChild(button);
        }

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
                .dimensions(this.width / 2 - 80, this.height - 32, 160, 20)
                .build());
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
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("hev_suit.config.subtitle"), this.width / 2, 40, 0xFFFFFF);
    }

    private record ToggleEntry(Text label, BooleanSupplier getter, Consumer<Boolean> setter) {
        private boolean get() {
            return getter.getAsBoolean();
        }

        private void set(boolean value) {
            setter.accept(value);
        }
    }
}
