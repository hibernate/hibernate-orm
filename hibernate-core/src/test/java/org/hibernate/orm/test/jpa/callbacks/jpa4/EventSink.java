/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class EventSink {
	public static List<Class<?>> bookCreationEvents = new ArrayList<>();
	public static List<Class<?>> bookUpdateEvents = new ArrayList<>();
	public static List<Class<?>> bookDeleteEvents = new ArrayList<>();

	public static List<Class<?>> personCreationEvents = new ArrayList<>();
	public static List<Class<?>> personUpdateEvents = new ArrayList<>();
	public static List<Class<?>> personDeleteEvents = new ArrayList<>();

	public static List<Class<?>> publisherCreationEvents = new ArrayList<>();
	public static List<Class<?>> publisherUpdateEvents = new ArrayList<>();
	public static List<Class<?>> publisherDeleteEvents = new ArrayList<>();

	public static void reset() {
		bookCreationEvents.clear();
		bookUpdateEvents.clear();
		bookDeleteEvents.clear();

		personCreationEvents.clear();
		personUpdateEvents.clear();
		personDeleteEvents.clear();

		publisherCreationEvents.clear();
		publisherUpdateEvents.clear();
		publisherDeleteEvents.clear();
	}
}
