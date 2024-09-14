/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.spi;

import org.hibernate.Incubating;

/**
 * Provides access to the values for a managed type (currently just embeddables).
 *
 * @see EmbeddableInstantiator
 *
 * @author Christian Beikov
 */
@Incubating
public interface ValueAccess {
	/**
	 * The complete set of values.
	 */
	Object[] getValues();

	/**
	 * Access to an individual value.
	 *
	 * @apiNote It is important to remember that attributes are
	 * sorted alphabetically.  So the values here will be in alphabetically
	 * order according to the names of the corresponding attribute
	 */
	default <T> T getValue(int i, Class<T> clazz) {
		return clazz.cast( getValues()[i] );
	}

	/**
	 * Access to the owner of the instance being instantiated
	 */
	default Object getOwner() {
		return null;
	}
}
