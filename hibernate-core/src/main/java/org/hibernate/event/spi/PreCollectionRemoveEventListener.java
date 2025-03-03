/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called before removing a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionRemoveEventListener {
	void onPreRemoveCollection(PreCollectionRemoveEvent event);
}
