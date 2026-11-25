package dev.brian.snapdrift;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared constants. Snapdrift is client-only; the pure benchmark logic lives in {@code core}. */
public final class Snapdrift {
	public static final String MOD_ID = "snapdrift";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Snapdrift() {}
}
