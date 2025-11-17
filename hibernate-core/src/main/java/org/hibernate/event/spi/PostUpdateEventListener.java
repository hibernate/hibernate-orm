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
public interface PostUpdateEventListener extends PostActionEventListener {
	void onPostUpdate(PostUpdateEvent event);
}
