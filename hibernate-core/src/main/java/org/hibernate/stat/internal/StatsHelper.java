/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.internal;

import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Utilities useful when dealing with stats.
 *
 * @author Steve Ebersole
 */
public class StatsHelper {

	public static NavigableRole getRootEntityRole(EntityPersister entityDescriptor) {
		final String rootEntityName = entityDescriptor.getRootEntityName();
		if ( entityDescriptor.getEntityName().equals( rootEntityName ) ) {
			return entityDescriptor.getNavigableRole();
		}
		else {
			return entityDescriptor.getFactory().getMappingMetamodel()
					.getEntityDescriptor( rootEntityName )
					.getNavigableRole();
		}
	}

	private StatsHelper() {
	}
}
