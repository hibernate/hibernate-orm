/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called after recreating a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionRecreateEventListener {
	void onPostRecreateCollection(PostCollectionRecreateEvent event);
}
