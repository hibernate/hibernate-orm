/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EventSource;

public class CustomEntityCopyObserver implements EntityCopyObserver {

	@Override
	public void entityCopyDetected(
			Object managedEntity,
			Object mergeEntity1,
			Object mergeEntity2,
			EventSource session) {
		if ( Category.class.isInstance( managedEntity ) ) {
			throw new IllegalStateException(
					String.format( "Entity copies of type [%s] not allowed", Category.class.getName() )
			);
		}
	}

	@Override
	public void topLevelMergeComplete(EventSource session) {
	}

	@Override
	public void clear() {
	}
}
