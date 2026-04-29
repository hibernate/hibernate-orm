/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostUpdate;

/**
 * @author Steve Ebersole
 */
@Entity
@EntityListeners( PublisherListener.class )
public class Publisher {
	@Id
	private Integer id;
	private String name;

	public Publisher() {
	}

	public Publisher(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	@PostInsert
	public void afterCreation() {
		EventSink.publisherCreationEvents.add( Publisher.class );
	}

	@PostUpdate
	public void afterUpdate() {
		EventSink.publisherUpdateEvents.add( Publisher.class );
	}

	@PostDelete
	public void afterDelete() {
		EventSink.publisherDeleteEvents.add( Publisher.class );
	}
}
