package net.kaikk.mc.gpp.skyblock;

import java.util.HashMap;
import java.util.Map;

public class Messages {
	static Map<String, String> messages = new HashMap<String, String>();
	
	public static String get(String id) {
		return messages.get(id);
	}
}
