/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Loader for {@link org.hibernate.annotations.NaturalId} handling
 *
 * @author Steve Ebersole
 */
public interface SimpleNaturalIdLoader extends NaturalIdLoader {
	/**
	 * Given the value of the entity's simple natural id, resolve the matching
	 * identifier (PK) value
	 */
	Object resolveIdentifier(Object naturalIdValue, SharedSessionContractImplementor session);

	/**
	 * Given the value of the entity's simple natural id, load the matching entity
	 * instance
	 */
	Object loadEntity(Object naturalIdValue, SharedSessionContractImplementor session);
}
