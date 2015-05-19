/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.Set;

/**
 * Specify if an entity class is instrumented to track field changes
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface SelfDirtinessTracker {
	/**
	 * Return true if any fields has been changed
	 */
	boolean $$_hibernate_hasDirtyAttributes();

	/**
	 * Get the field names of all the fields thats been changed
	 */
	Set<String> $$_hibernate_getDirtyAttributes();

	/**
	 * Clear the stored dirty attributes
	 */
	void $$_hibernate_clearDirtyAttributes();
}
