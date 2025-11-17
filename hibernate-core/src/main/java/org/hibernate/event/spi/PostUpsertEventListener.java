/*
 * SPDX-License-Identifier: Apache-2.0
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
