/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test.listener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity(name = "ListenerEntity")
@EntityListeners(EntityLevelListener.class)
public class ListenerEntity {
	@Id
	public Long id;

	public ListenerEntity() {
	}

	public ListenerEntity(Long id) {
		this.id = id;
	}

	@PrePersist
	void prePersist() {
		EventTracker.events.add( "entity-callback:" + getClass().getSimpleName() );
	}
}
