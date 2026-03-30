package teluri.mods.jlrays.util;

import java.util.concurrent.atomic.AtomicInteger;

public class ActionWithCooldown {
	private AtomicInteger counter = new AtomicInteger(0);
	private int cooldown;
	private Runnable action;

	public ActionWithCooldown(int ncooldown, Runnable naction) {
		cooldown = ncooldown;
		action = naction;
	}

	public void Do() {
		int val = counter.getAndUpdate((v) -> v <= 0 ? cooldown : v - 1);
		if (val <= 0) {
			action.run();
		}
	}
}
