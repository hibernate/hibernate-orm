/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml;

/**
 * An entity listener that keeps a journal of calls
 *
 * @author Steve Ebersole
 */
public class JournalingListener {
	private static int prePersistCount;

	public void onPrePersist(Object entity) {
		prePersistCount++;
	}

	public static int getPrePersistCount() {
		return prePersistCount;
	}

	public static void reset() {
		prePersistCount = 0;
	}
}
