package com.simibubi.create;

import java.util.function.BiConsumer;

import org.lwjgl.sdl.SDLMouse;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;

@EventBusSubscriber(Dist.CLIENT)
public enum AllKeys {

	TOOL_MENU("toolmenu", InputConstants.KEY_LALT, "Focus Schematic Overlay"),
	ACTIVATE_TOOL(InputConstants.KEY_LCONTROL),
	TOOLBELT("toolbelt", InputConstants.KEY_LALT, "Access Nearby Toolboxes"),
	ROTATE_MENU("rotate_menu", InputConstants.UNKNOWN.getValue(), "Open Block Rotation Menu"),

	SHIFT_MODIFIER("shift_modifier", InputConstants.KEY_LSHIFT, "Shift Modifier", true),
	CTRL_MODIFIER("ctrl_modifier", InputConstants.KEY_LCONTROL, "Ctrl Modifier", true),
	ALT_MODIFIER("alt_modifier", InputConstants.KEY_LALT, "Alt Modifier", true),
	;

	private KeyMapping keybind;
	private final String description;
	private final String translation;
	private final int key;
	private final boolean modifiable;
	private final boolean conflictSafe;

	AllKeys(int defaultKey) {
		this("", defaultKey, "");
	}

	AllKeys(String description, int defaultKey, String translation) {
		this(description, defaultKey, translation, false);
	}

	AllKeys(String description, int defaultKey, String translation, boolean conflictSafe) {
		this.description = Create.ID + ".keyinfo." + description;
		this.key = defaultKey;
		this.modifiable = !description.isEmpty();
		this.translation = translation;
		this.conflictSafe = conflictSafe;
	}

	public static void provideLang(BiConsumer<String, String> consumer) {
		for (AllKeys key : values())
			if (key.modifiable)
				consumer.accept(key.description, key.translation);
	}

	private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Create.asResource("main"));

	@SubscribeEvent
	public static void register(RegisterKeyMappingsEvent event) {
		event.registerCategory(CATEGORY);
		for (AllKeys key : values()) {
			// Modifier pseudo-bindings must not conflict with ordinary key mappings.
			key.keybind = key.conflictSafe ? new KeyMapping(key.description, key.key, CATEGORY) {
				@Override
				public boolean same(KeyMapping other) {
					return false;
				}
			} : new KeyMapping(key.description, key.key, CATEGORY);
			if (!key.modifiable)
				continue;

			event.register(key.keybind);
		}
	}

	public KeyMapping getKeybind() {
		return keybind;
	}

	public boolean isPressed() {
		if (!modifiable)
			return isKeyDown(key);
		return keybind.isDown();
	}

	public String getBoundKey() {
		return keybind.getTranslatedKeyMessage()
			.getString()
			.toUpperCase();
	}

	public boolean doesModifierAndCodeMatch(int code) {
		boolean codeMatches = code == keybind.getKey().getValue();

		boolean modifierMatches;
		KeyModifier modifier = keybind.getKeyModifier();
		if (modifier == KeyModifier.NONE) {
			modifierMatches = true;
		} else {
			modifierMatches = KeyModifier.getActiveModifiers().contains(modifier);
		}

		return codeMatches && modifierMatches;
	}

	public static boolean isKeyDown(int key) {
		return InputConstants.isKeyDown(key);
	}

	public static boolean isMouseButtonDown(int button) {
		return (SDLMouse.SDL_GetMouseState(null, null) & (1 << (button - 1))) != 0;
	}

	public static boolean ctrlDown() {
		return isKeyDown(CTRL_MODIFIER.keybind.getKey().getValue());
	}

	public static boolean shiftDown() {
		return isKeyDown(SHIFT_MODIFIER.keybind.getKey().getValue());
	}

	public static boolean altDown() {
		return isKeyDown(ALT_MODIFIER.keybind.getKey().getValue());
	}

}
