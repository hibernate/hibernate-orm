package org.hibernate.orm.integrationtest.java.module.test.listener;

import java.util.ArrayList;
import java.util.List;

public class EventTracker {
	public static final List<String> events = new ArrayList<>();

	public static void reset() {
		events.clear();
	}
}
