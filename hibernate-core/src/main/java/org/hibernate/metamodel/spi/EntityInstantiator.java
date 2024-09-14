/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contract for instantiating entity values
 */
public interface EntityInstantiator extends Instantiator {
	/**
	 * Create an instance of managed entity
	 */
	Object instantiate(SessionFactoryImplementor sessionFactory);

	/**
	 * Can this entity be instantiated?
	 */
	default boolean canBeInstantiated() {
		return true;
	}
}
