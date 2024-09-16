/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called after updating the datastore
 *
 * @author Gavin King
 */
public interface PostUpsertEventListener extends PostActionEventListener {
	void onPostUpsert(PostUpsertEvent event);
}
