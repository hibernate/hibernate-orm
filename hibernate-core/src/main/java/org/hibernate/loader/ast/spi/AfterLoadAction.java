/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
}
