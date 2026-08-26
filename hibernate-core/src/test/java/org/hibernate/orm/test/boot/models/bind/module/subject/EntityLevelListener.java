/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module.subject;

import jakarta.persistence.PrePersist;

public class EntityLevelListener {
	@PrePersist
	public void prePersist(Object entity) {
		EventTracker.events.add( "entity-listener:" + entity.getClass().getSimpleName() );
	}
}
