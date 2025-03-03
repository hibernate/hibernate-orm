/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called after removing a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionRemoveEventListener {
	void onPostRemoveCollection(PostCollectionRemoveEvent event);
}
