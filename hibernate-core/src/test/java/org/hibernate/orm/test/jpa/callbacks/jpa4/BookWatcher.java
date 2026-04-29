/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import jakarta.persistence.EntityListener;
import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostUpdate;

/**
 * @author Steve Ebersole
 */
@EntityListener
public class BookWatcher {
	@PostInsert
	public void afterCreation(Book book) {
		EventSink.bookCreationEvents.add( BookWatcher.class );
	}

	@PostUpdate
	public void afterUpdate(Book book) {
		EventSink.bookUpdateEvents.add( BookWatcher.class );
	}

	@PostDelete
	public void afterDelete(Book book) {
		EventSink.bookDeleteEvents.add( BookWatcher.class );
	}
}
