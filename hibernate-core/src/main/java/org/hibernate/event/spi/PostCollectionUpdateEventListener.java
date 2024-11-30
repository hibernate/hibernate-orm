/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called after updating a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionUpdateEventListener {
	void onPostUpdateCollection(PostCollectionUpdateEvent event);
}
