/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostUpdate;

/**
 * @author Steve Ebersole
 */
public class PublisherListener {
	@PostInsert
	public void afterCreation(Publisher entity) {
		EventSink.publisherCreationEvents.add( PublisherListener.class );
	}

	@PostUpdate
	public void afterUpdate(Publisher entity) {
		EventSink.publisherUpdateEvents.add( PublisherListener.class );
	}

	@PostDelete
	public void afterDelete(Publisher entity) {
		EventSink.publisherDeleteEvents.add( PublisherListener.class );
	}
}
