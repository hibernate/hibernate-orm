/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * An action to be performed after an entity has been loaded.  E.g. applying locks
 *
* @author Steve Ebersole
*/
public interface AfterLoadAction {
	/**
	 * The action trigger - the {@code entity} is being loaded
	 */
	void afterLoad(Object entity, EntityMappingType entityMappingType, SharedSessionContractImplementor session);

	/**
	 * @deprecated Use the {@linkplain #afterLoad(Object, EntityMappingType, SharedSessionContractImplementor) updated form}
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "6", forRemoval = true)
	default void afterLoad(SharedSessionContractImplementor session, Object entity, org.hibernate.persister.entity.Loadable persister) {
		afterLoad( entity, persister, session );
	}
}
