package name.modid.client;

public final class AnvilInsightClient {
	private static boolean panelOpen;

	private AnvilInsightClient() {
	}

	public static boolean isPanelOpen() {
		return panelOpen;
	}

	public static void togglePanel() {
		panelOpen = !panelOpen;
	}
}
