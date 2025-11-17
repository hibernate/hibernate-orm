/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.event.spi;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityConfiguration;

/**
 * @author Chris Cranford
 */
public abstract class BaseEnversUpdateEventListener extends BaseEnversEventListener {
	public BaseEnversUpdateEventListener(EnversService enversService) {
		super( enversService );
	}

	/**
	 * Returns whether the entity has {@code withModifiedFlag} features and has no old state, most likely implying
	 * it was updated in a detached entity state.
	 *
	 * @param entityName The associated entity name.
	 * @param oldState The event old (likely detached) entity state.
	 * @return {@code true} if the entity is/has been updated in detached state, otherwise {@code false}.
	 */
	protected boolean isDetachedEntityUpdate(String entityName, Object[] oldState) {
		final EntityConfiguration configuration = getEnversService().getEntitiesConfigurations().get( entityName );
		if ( configuration.getPropertyMapper() != null && oldState == null ) {
			return configuration.getPropertyMapper().hasPropertiesWithModifiedFlag();
		}
		return false;
	}
}
