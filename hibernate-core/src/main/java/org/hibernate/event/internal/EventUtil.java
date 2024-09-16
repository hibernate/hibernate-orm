/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

public class EventUtil {
	public static String getLoggableName(String entityName, Object entity) {
		return entityName == null ? entity.getClass().getName() : entityName;
	}
}
