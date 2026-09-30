package io.github.kenthejr.infinitycastle.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.kenthejr.infinitycastle.InfinityCastle;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client-only preferences, stored in {@code config/infinitycastle-client.json}. The camera effects can be disorienting,
 * so every one of them can be turned off.
 */
public final class ClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("infinitycastle-client.json");

	private static Values values = new Values();

	/** Serialized form. Field names are the JSON keys. */
	public static final class Values {
		/** Rotate the camera smoothly when gravity flips, instead of snapping. */
		public boolean smoothFlip = true;
		/** Roll the view upside down while standing on a ceiling. With this off, only the physics flips. */
		public boolean rollCameraWhenInverted = true;
		/** Mirror mouse and strafe controls while the view is upside down, so they match what you see. */
		public boolean mirrorControlsWhenInverted = true;
		/** Some floors hold the camera at a slight constant tilt. */
		public boolean floorTilt = true;
		/** The red–amber colour grade and vignette. */
		public boolean colorGrade = true;
	}

	private ClientConfig() {
	}

	public static Values get() {
		return values;
	}

	public static void load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				Values loaded = GSON.fromJson(reader, Values.class);
				if (loaded != null) {
					values = loaded;
				}
			} catch (IOException | JsonParseException e) {
				InfinityCastle.LOGGER.warn("Could not read {}, using defaults", PATH, e);
			}
		}
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(values, writer);
			}
		} catch (IOException e) {
			InfinityCastle.LOGGER.warn("Could not write {}", PATH, e);
		}
	}
}
