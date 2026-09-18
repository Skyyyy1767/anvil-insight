package name.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;

import name.modid.client.gui.AnvilInsightController;

public class AnvilInsightClient implements ClientModInitializer {
	private static boolean panelOpen;

	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof AnvilScreen anvilScreen) {
				AnvilInsightController.attach(client, anvilScreen);
			}
		});
	}

	public static boolean isPanelOpen() {
		return panelOpen;
	}

	public static void togglePanel() {
		panelOpen = !panelOpen;
	}
}
