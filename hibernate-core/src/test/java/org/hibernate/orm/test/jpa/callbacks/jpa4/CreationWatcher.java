/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import jakarta.persistence.EntityListener;
import jakarta.persistence.PostInsert;

/**
 * @author Steve Ebersole
 */
@EntityListener
public class CreationWatcher {
	@PostInsert
	public void postInsert(Book book) {
		EventSink.bookCreationEvents.add( CreationWatcher.class );
	}

	@PostInsert
	public void postInsert(Person person) {
		EventSink.personCreationEvents.add( CreationWatcher.class );
	}

	@PostInsert
	public void postInsert(Publisher person) {
		EventSink.publisherCreationEvents.add( CreationWatcher.class );
	}
}
