package io.github.kenthejr.infinitycastle.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class InfinityCastleClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientConfig.load();
		ClientTickEvents.END_CLIENT_TICK.register(CastleCamera::tick);
	}
}
