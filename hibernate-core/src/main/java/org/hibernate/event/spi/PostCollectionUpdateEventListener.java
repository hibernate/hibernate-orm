/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called after updating a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionUpdateEventListener {
	void onPostUpdateCollection(@Nonnull PostCollectionUpdateEvent event);
}
