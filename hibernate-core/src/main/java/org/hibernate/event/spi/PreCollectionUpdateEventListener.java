/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called before updating a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionUpdateEventListener {
	void onPreUpdateCollection(PreCollectionUpdateEvent event);
}
