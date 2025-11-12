package ltown.hev_suit.client.managers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.*;

public class SoundManager {
    private static final Logger LOGGER = LogManager.getLogger("SoundManager");
    static final Map<String, SoundEvent> SOUND_EVENTS = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final float MIN_PITCH = 0.98f;
    private static final float MAX_PITCH = 1.05f;
    private static final Set<String> HEALTH_ALERT_SOUNDS = Set.of(
            "health_critical2", "seek_medical", "health_critical", "near_death"
    );
    private static final QueueChannel GENERAL_CHANNEL = new QueueChannel("general", "");
    private static final QueueChannel HEALTH_CHANNEL = new QueueChannel("health", "[health] ");
    private static SoundInstance geigerSoundInstance;
    private static boolean geigerLoopRequested = false;
    private static SoundInstance flatlineSoundInstance;
    private static final Method SOUND_MANAGER_PLAY_METHOD;
    private static final boolean SOUND_MANAGER_PLAY_WITH_SEED;
    private static final int SOUND_STARTUP_GRACE_TICKS = 3; // give the engine a few ticks before we declare a sound dead

    static {
        Method seeded = resolvePlayMethod(true);
        Method basic = resolvePlayMethod(false);
        SOUND_MANAGER_PLAY_METHOD = seeded != null ? seeded : basic;
        SOUND_MANAGER_PLAY_WITH_SEED = seeded != null;
        if (SOUND_MANAGER_PLAY_METHOD == null) {
            LOGGER.warn("Unable to locate SoundManager play method; sounds may fail on some versions.");
        }
    }
    public static void registerSounds() {
        String[] soundNames = {
                "major_laceration", "minor_laceration", "major_fracture", "minor_fracture",
                "blood_loss", "health_critical", "health_critical2", "morphine_administered",
                "seek_medical", "near_death", "heat_damage", "shock_damage", "chemical",
                "armor_gone", "hev_damage", "ammunition_depleted", "hev_general_fail",
                "hev_logon", "weapon_pickup", "ammo_pickup", "powermove_on", "powermove_overload",
                "internal_bleeding", "power", "power_level_is", "percent", "5", "10", "15",
                "20", "25", "30", "40", "50", "60", "70", "80", "90", "100",
                "administering_medical", "insufficient_medical", "medical_repaired",
                "armor_compromised", "radiation_detected", "geiger", "flatline"
        };

        for (String soundName : soundNames) {
            registerSound(soundName);
        }
    }

    private static void registerSound(String name) {
        try {
            Identifier soundId = Identifier.of("hev_suit", name);
            SoundEvent sound = SoundEvent.of(soundId);
            Registry.register(Registries.SOUND_EVENT, soundId, sound);
            SOUND_EVENTS.put(name, sound);
        } catch (Exception e) {
            LOGGER.error("Failed to register sound: {}", name, e);
        }
    }

    public static void processSoundQueue(MinecraftClient client) {
        maintainLoopingSounds(client);
        if (client == null || client.getSoundManager() == null) {
            return;
        }

        // Health alerts are processed first so they do not starve behind other notifications.
        processQueueChannel(client, HEALTH_CHANNEL);
        processQueueChannel(client, GENERAL_CHANNEL);
    }

    private static void maintainLoopingSounds(MinecraftClient client) {
        if (client == null || client.getSoundManager() == null) {
            return;
        }
        if (geigerLoopRequested) {
            ensureGeigerLoop(client);
        }
    }

    private static void processQueueChannel(MinecraftClient client, QueueChannel channel) {
        if (channel.currentSound != null) {
            if (channel.startupCooldown > 0) {
                channel.startupCooldown--;
            }
            if (isChannelActive(client, channel)) {
                return;
            }

            channel.currentSound = null;
            channel.currentSoundName = null;
        }

        if (channel == GENERAL_CHANNEL && isChannelActive(client, HEALTH_CHANNEL)) {
            return;
        }

        if (!channel.pending.isEmpty()) {
            playNextSound(client, channel);
        }
    }

    private static void playNextSound(MinecraftClient client, QueueChannel channel) {
        String soundName = channel.pending.poll();
        if (soundName == null) return;

        SoundEvent sound = SOUND_EVENTS.get(soundName);
        if (sound == null) {
            LOGGER.warn("Sound not found: {}", soundName);
            return;
        }

        if (channel == HEALTH_CHANNEL && isChannelActive(client, GENERAL_CHANNEL)) {
            interruptChannel(client, GENERAL_CHANNEL);
        }

        try {
            LOGGER.debug("Playing {} queue sound: {}", channel.name, soundName);
            SoundInstance instance = createSoundInstance(sound, MIN_PITCH + RANDOM.nextFloat() * (MAX_PITCH - MIN_PITCH));
            channel.currentSound = instance;
            channel.currentSoundName = soundName;
            if (playSoundInstance(client, instance)) {
                channel.startupCooldown = SOUND_STARTUP_GRACE_TICKS;
                LOGGER.debug("Displaying caption for: {} (captions enabled: {})", soundName, SettingsManager.captionsEnabled);
                SubtitleManager.displayCaption(soundName);
            } else {
                LOGGER.warn("Failed to play sound '{}' in {} queue, re-queuing.", soundName, channel.name);
                channel.currentSound = null;
                channel.currentSoundName = null;
                channel.startupCooldown = 0;
                channel.addUniqueToFront(soundName);
            }
        } catch (Exception e) {
            LOGGER.error("Error playing sound: {}", soundName, e);
            channel.currentSound = null;
            channel.currentSoundName = null;
            channel.startupCooldown = 0;
        }
    }

    public static void queueSound(String soundName) {
        if (soundName == null || soundName.isEmpty()) {
            return;
        }
        QueueChannel target = HEALTH_ALERT_SOUNDS.contains(soundName) ? HEALTH_CHANNEL : GENERAL_CHANNEL;
        target.addUniqueToTail(soundName);
    }

    public static void clearSoundQueue() {
        resetChannel(GENERAL_CHANNEL);
        resetChannel(HEALTH_CHANNEL);
    }

    public static void clearQueue() {
        clearSoundQueue();
    }

    // Add this method to peek at the next N sounds in queue
    public static List<String> peekNextSounds(int count) {
        return GENERAL_CHANNEL.pending.stream()
               .limit(count)
               .toList();
    }

    // Add this method to get the complete power level announcement
    public static String getPowerLevelFromQueue() {
        List<String> upcoming = peekNextSounds(5); // Peek at next 5 sounds
        int totalPercent = 0;
        
        for (String sound : upcoming) {
            if (sound.contains("percent")) break;
            try {
                totalPercent += Integer.parseInt(sound); // Add the numbers together
            } catch (NumberFormatException e) {
                continue;
            }
        }
        
        return totalPercent > 0 ? String.valueOf(totalPercent) : null;
    }

    public static List<String> getQueuedSounds() {
        List<String> queued = new ArrayList<>(HEALTH_CHANNEL.pending.size() + GENERAL_CHANNEL.pending.size());
        appendQueuedSounds(queued, HEALTH_CHANNEL);
        appendQueuedSounds(queued, GENERAL_CHANNEL);
        return queued;
    }

    public static void playImmediateSound(String soundName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSoundManager() == null) return;

        SoundEvent sound = SOUND_EVENTS.get(soundName);
        if (sound == null) {
            LOGGER.warn("Immediate sound not found: {}", soundName);
            return;
        }

        try {
            SoundInstance instance = createSoundInstance(sound, 1.0f);
            if (playSoundInstance(client, instance)) {
                SubtitleManager.displayCaption(soundName);
            }
        } catch (Exception e) {
            LOGGER.error("Error playing immediate sound: {}", soundName, e);
        }
    }

    public static void startGeigerLoop() {
        geigerLoopRequested = true;
        HudManager.setRadiationActive(true);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            ensureGeigerLoop(client);
        }
    }

    public static void stopGeigerLoop() {
        geigerLoopRequested = false;
        HudManager.setRadiationActive(false);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && geigerSoundInstance != null) {
            client.getSoundManager().stop(geigerSoundInstance);
        }
        geigerSoundInstance = null;
    }

    private static void ensureGeigerLoop(MinecraftClient client) {
        if (!geigerLoopRequested) return;
        if (geigerSoundInstance != null && client.getSoundManager().isPlaying(geigerSoundInstance)) {
            return;
        }

        boolean firstStart = geigerSoundInstance == null;

        SoundEvent sound = SOUND_EVENTS.get("geiger");
        if (sound == null) {
            LOGGER.warn("Geiger sound not registered");
            return;
        }

        geigerSoundInstance = createSoundInstance(sound, 1.0f);
        if (playSoundInstance(client, geigerSoundInstance) && firstStart) {
            SubtitleManager.displayCaption("geiger");
        }
    }

    public static void playFlatline() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSoundManager() == null) return;

        if (flatlineSoundInstance != null && client.getSoundManager().isPlaying(flatlineSoundInstance)) {
            return;
        }

        SoundEvent sound = SOUND_EVENTS.get("flatline");
        if (sound == null) {
            LOGGER.warn("Flatline sound not registered");
            return;
        }

        flatlineSoundInstance = createSoundInstance(sound, 1.0f);
        if (playSoundInstance(client, flatlineSoundInstance)) {
            SubtitleManager.displayCaption("flatline");
        }
    }

    public static void stopFlatline() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && flatlineSoundInstance != null) {
            client.getSoundManager().stop(flatlineSoundInstance);
        }
        flatlineSoundInstance = null;
    }

    private static boolean playSoundInstance(MinecraftClient client, SoundInstance instance) {
        if (client == null || client.getSoundManager() == null || instance == null) {
            return false;
        }

        boolean succeeded = false;

        try {
            SoundSystem.PlayResult result = client.getSoundManager().play(instance);
            if (result != SoundSystem.PlayResult.NOT_STARTED) {
                return true;
            }
            LOGGER.debug("Sound {} did not start immediately (result: {}), attempting fallback.", instance.getId(), result);
        } catch (NoSuchMethodError ignored) {
            // Older signatures do not return a PlayResult.
        } catch (Throwable throwable) {
            LOGGER.error("Failed to invoke SoundManager#play(instance)", throwable);
        }

        if (SOUND_MANAGER_PLAY_METHOD != null) {
            try {
                if (SOUND_MANAGER_PLAY_WITH_SEED) {
                    SOUND_MANAGER_PLAY_METHOD.invoke(client.getSoundManager(), instance, RANDOM.nextInt());
                } else {
                    SOUND_MANAGER_PLAY_METHOD.invoke(client.getSoundManager(), instance);
                }
                succeeded = true;
            } catch (NoSuchMethodError | IllegalAccessException | IllegalArgumentException e) {
                LOGGER.warn("Preferred SoundManager#play signature unavailable, attempting runtime fallback.", e);
            } catch (java.lang.reflect.InvocationTargetException e) {
                LOGGER.error("SoundManager#play invocation failed.", e.getCause());
                succeeded = false;
            } catch (Throwable throwable) {
                LOGGER.error("Failed to invoke SoundManager#play reflectively", throwable);
            }
        }

        if (!succeeded) {
            try {
                client.getSoundManager().play(instance, RANDOM.nextInt());
                succeeded = true;
            } catch (NoSuchMethodError ignored) {
                // Older signatures do not include the seed parameter.
            } catch (Throwable throwable) {
                LOGGER.error("Failed to invoke SoundManager#play(instance, seed)", throwable);
            }
        }

        return succeeded;
    }

    private static SoundInstance createSoundInstance(SoundEvent sound, float pitch) {
        return new ImmediateSoundInstance(sound, pitch);
    }

    private static boolean isChannelActive(MinecraftClient client, QueueChannel channel) {
        if (channel.currentSound == null) {
            return false;
        }
        if (channel.startupCooldown > 0) {
            return true;
        }
        if (client == null || client.getSoundManager() == null) {
            return false;
        }
        return client.getSoundManager().isPlaying(channel.currentSound);
    }

    private static void interruptChannel(MinecraftClient client, QueueChannel channel) {
        if (channel.currentSound == null || client == null || client.getSoundManager() == null) {
            return;
        }

        if (channel.currentSoundName != null) {
            channel.addUniqueToFront(channel.currentSoundName);
        }

        client.getSoundManager().stop(channel.currentSound);
        channel.currentSound = null;
        channel.currentSoundName = null;
        channel.startupCooldown = 0;
    }

    private static void resetChannel(QueueChannel channel) {
        channel.pending.clear();
        channel.currentSound = null;
        channel.currentSoundName = null;
        channel.startupCooldown = 0;
    }

    private static void appendQueuedSounds(List<String> target, QueueChannel channel) {
        if (channel.pending.isEmpty()) {
            return;
        }
        for (String sound : channel.pending) {
            target.add(channel.listPrefix.isEmpty() ? sound : channel.listPrefix + sound);
        }
    }

    private static Method resolvePlayMethod(boolean requireSeed) {
        Class<?> soundManagerClass = net.minecraft.client.sound.SoundManager.class;
        Class<?>[] signature = requireSeed
                ? new Class<?>[]{SoundInstance.class, int.class}
                : new Class<?>[]{SoundInstance.class};

        Method method = findMethod(soundManagerClass, signature);
        if (method != null) {
            return method;
        }

        Class<?> current = soundManagerClass.getSuperclass();
        while (current != null && method == null) {
            method = findMethod(current, signature);
            current = current.getSuperclass();
        }

        return method;
    }

    private static Method findMethod(Class<?> clazz, Class<?>[] signature) {
        if (clazz == null) return null;

        for (Method method : clazz.getDeclaredMethods()) {
            if (isPlaySignature(method, signature)) {
                makeAccessible(method);
                return method;
            }
        }

        for (Method method : clazz.getMethods()) {
            if (isPlaySignature(method, signature)) {
                makeAccessible(method);
                return method;
            }
        }

        return null;
    }

    private static boolean isPlaySignature(Method method, Class<?>[] signature) {
        if (method.getReturnType() != void.class) return false;
        Class<?>[] params = method.getParameterTypes();
        if (params.length != signature.length) return false;
        for (int i = 0; i < params.length; i++) {
            Class<?> expected = signature[i];
            Class<?> actual = params[i];
            if (!(actual.isAssignableFrom(expected) || expected.isAssignableFrom(actual))) {
                return false;
            }
        }
        return true;
    }

    private static void makeAccessible(Method method) {
        try {
            method.setAccessible(true);
        } catch (Throwable ignored) {
            // Ignored – the method may already be accessible or modules may forbid access.
        }
    }

    private static class ImmediateSoundInstance extends AbstractSoundInstance {
        ImmediateSoundInstance(SoundEvent sound, float pitch) {
            super(sound, SoundCategory.MASTER, SoundInstance.createRandom());
            this.volume = 1.0f;
            this.pitch = pitch;
            this.repeat = false;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true;
        }

        @Override
        public boolean canPlay() {
            return true;
        }
    }

    private static class QueueChannel {
        final String name;
        final String listPrefix;
        final LinkedList<String> pending = new LinkedList<>();
        SoundInstance currentSound;
        String currentSoundName;
        int startupCooldown;

        QueueChannel(String name, String listPrefix) {
            this.name = name;
            this.listPrefix = listPrefix;
        }

        void addUniqueToTail(String soundName) {
            if (soundName == null) {
                return;
            }
            // Eliminate trailing duplicates so the queue only keeps one contiguous copy.
            ListIterator<String> iterator = pending.listIterator(pending.size());
            while (iterator.hasPrevious()) {
                if (!soundName.equals(iterator.previous())) {
                    break;
                }
                iterator.remove();
            }
            pending.addLast(soundName);
        }

        void addUniqueToFront(String soundName) {
            if (soundName == null) {
                return;
            }
            // Prevent duplicate fronts when re-queueing interrupted clips.
            while (!pending.isEmpty() && soundName.equals(pending.peekFirst())) {
                pending.removeFirst();
            }
            pending.addFirst(soundName);
        }
    }
}
