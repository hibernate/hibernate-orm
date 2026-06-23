/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called before updating a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionUpdateEventListener {
	void onPreUpdateCollection(@Nonnull PreCollectionUpdateEvent event);
}
