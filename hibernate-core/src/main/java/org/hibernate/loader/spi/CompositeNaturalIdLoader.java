/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface CompositeNaturalIdLoader extends NaturalIdLoader {
	/**
	 * Given the values of the entity's composite natural id, resolve the
	 * matching identifier (PK) value
	 */
	Object resolveIdentifier(Object[] naturalIdValues, SharedSessionContractImplementor session);

	/**
	 * Given the values of the entity's composite natural id, load the
	 * matching entity instance
	 */
	Object loadEntity(Object[] naturalIdValues, SharedSessionContractImplementor session);
}
