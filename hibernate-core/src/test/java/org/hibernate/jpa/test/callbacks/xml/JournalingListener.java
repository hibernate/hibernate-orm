/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.callbacks.xml;

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
