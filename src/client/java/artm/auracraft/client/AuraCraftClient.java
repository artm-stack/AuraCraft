package artm.auracraft.client;

import artm.auracraft.AuraCraft;
import artm.auracraft.payload.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class AuraCraftClient implements ClientModInitializer {
	// Client Side
	private static KeyMapping PICK_AURA_KEY;
	private static final KeyMapping.Category AURA_CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(AuraCraft.MOD_ID, "auracraft"));

	private static final List<String>         chosenAuras    = new ArrayList<>();
	private static final Set<String>          cappedAuras    = new HashSet<>();
	private static final Set<String>          enabledAuras   = new HashSet<>();
	private static final Map<String, Integer> upgradeLevels  = new HashMap<>();
	private static int     auraPoints  = 0;
	private static boolean uiDisabled  = false;

	// Initialisation
	@Override
	public void onInitializeClient() {
		registerKeybind();
		registerPayloadHandlers();
		registerEvents();

		AuraCraft.LOGGER.info("AuraCraft client initialized");
	}
	// Keybind
	private void registerKeybind() {
		PICK_AURA_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.auracraft.pick_aura",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_Y,
				AURA_CATEGORY
		));
	}
	// Payload Handlers
	private void registerPayloadHandlers() {
		// Server -> Client: open the aura picker screen
		ClientPlayNetworking.registerGlobalReceiver(PromptAuraPayload.TYPE,
				(payload, context) -> context.client().execute(() ->
						showPrompt(context.client())));
		// Server -> Client: sync aura state
		ClientPlayNetworking.registerGlobalReceiver(SyncAuraPayload.TYPE,
				(payload, context) -> context.client().execute(() -> {
					chosenAuras.clear();
					chosenAuras.addAll(payload.chosenAuras());
					cappedAuras.clear();
					cappedAuras.addAll(payload.cappedAuras());
					auraPoints = payload.auraPoints();
					upgradeLevels.clear();
					upgradeLevels.putAll(payload.upgradeLevels());
					// Refresh the open selection screen so button states are current
					if (context.client().screen instanceof SelectionScreen screen) {
						screen.extractWidgets();
					}
				}));
		// Server -> Client: sync UI state + enabled auras
		ClientPlayNetworking.registerGlobalReceiver(UiStatePayload.TYPE,
				(payload, context) -> context.client().execute(() -> {
					uiDisabled = payload.uiDisabled();
					enabledAuras.clear();
					enabledAuras.addAll(payload.enabledAuras());
					// Refresh aura list in case config-enabled auras changed
					if (context.client().screen instanceof SelectionScreen screen) {
						screen.extractWidgets();
					}
				}));
	}
	// Events
	private void registerEvents() {
		// Send handshake when joining a server
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				ClientPlayNetworking.send(
						new ClientHelloPayload(ClientHelloPayload.PROTOCOL_VERSION)));
		// Handle [Y] key press
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (PICK_AURA_KEY.consumeClick()) {
				if (uiDisabled) {
					if (client.player != null) {
						client.player.sendSystemMessage(
								Component.translatable("message.auracraft.ui_disabled"));
					}
					continue;
				}
				if (!(client.screen instanceof SelectionScreen)) {
					client.setScreen(new SelectionScreen());
				}
			}
		});
	}

	// API
	public static void sendChooseAura(String auraId) {
		ClientPlayNetworking.send(new ChooseAuraPayload(auraId));
	}
	public static boolean isAuraCapped(String auraId) {
		return cappedAuras.contains(auraId);
	}

	public static int getMaxUpgradeLevel(String auraId) {
		return upgradeLevels.getOrDefault(auraId, 1);
	}
	public static int getAuraPoints() { return auraPoints; }
	public static List<String> getChosenAuras() {
		return Collections.unmodifiableList(chosenAuras);
	}
	public static List<String> getEnabledAurasForPicker() {
		List<String> list = new ArrayList<>();
		for (String auraId : enabledAuras) {
			try {
				Identifier.parse(auraId);
				list.add(auraId);
			} catch (Exception ignored) {}
		}
		list.sort(Comparator.naturalOrder());
		return list;
	}
	public static Component auraName(String auraId) {
		try {
			Identifier id = Identifier.parse(auraId);
			return Component.translatable(
					"effect." + id.getNamespace() + "." + id.getPath());
		} catch (Exception ignored) {
			return Component.literal(auraId);
		}
	}

	// Helper
	private static void showPrompt(Minecraft client) {
		if (client.player == null || uiDisabled) return;
		client.player.sendSystemMessage(
				Component.translatable("message.auracraft.mod_name").withStyle(ChatFormatting.GREEN)
						.append(Component.translatable("message.auracraft.prompt",
								PICK_AURA_KEY.getTranslatedKeyMessage()
										.copy().withStyle(ChatFormatting.AQUA))
								.withStyle(ChatFormatting.WHITE))
		);
	}
}