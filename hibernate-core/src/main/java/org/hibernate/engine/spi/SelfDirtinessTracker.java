/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

/**
 * Contract for an entity to report that it tracks the dirtiness of its own state,
 * as opposed to needing Hibernate to perform state-diff dirty calculations.
 * <p/>
 * Entity classes are free to implement this contract themselves.  This contract is
 * also introduced into the entity when using bytecode enhancement and requesting
 * that entities track there own dirtiness.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface SelfDirtinessTracker {
	/**
	 * Have any of the entity's persistent attributes changed?
	 *
	 * @return {@code true} indicates one or more persistent attributes have changed; {@code false}
	 * indicates none have changed.
	 */
	boolean $$_hibernate_hasDirtyAttributes();

	/**
	 * Retrieve the names of all the persistent attributes whose values have changed.
	 *
	 * @return An array of changed persistent attribute names
	 */
	String[] $$_hibernate_getDirtyAttributes();

	/**
	 * Clear the stored dirty attributes
	 */
	void $$_hibernate_clearDirtyAttributes();
}
