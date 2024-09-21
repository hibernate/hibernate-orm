/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called before updating the datastore
 *
 * @author Gavin King
 */
public interface PreUpsertEventListener {
	/**
	 * Return true if the operation should be vetoed
	 */
	boolean onPreUpsert(PreUpsertEvent event);
}
