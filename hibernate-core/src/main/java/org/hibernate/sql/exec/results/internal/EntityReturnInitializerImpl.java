/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.sql.exec.results.spi.InitializerEntity;

/**
 * @author Steve Ebersole
 */
public class EntityReturnInitializerImpl
		extends AbstractInitializerEntity
		implements InitializerEntity {
	public EntityReturnInitializerImpl(
			EntityDescriptor entityDescriptor,
			EntitySqlSelectionMappings sqlSelectionMappings,
			boolean isShallow) {
		super( entityDescriptor, sqlSelectionMappings, isShallow );
	}

	@Override
	protected boolean isEntityReturn() {
		return true;
	}
}
