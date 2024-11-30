/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
