/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called after deleting an item from the datastore
 *
 * @author Gavin King
 */
public interface PostDeleteEventListener extends PostActionEventListener {
	void onPostDelete(PostDeleteEvent event);
}
