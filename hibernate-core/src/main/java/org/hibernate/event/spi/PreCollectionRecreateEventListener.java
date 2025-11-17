/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Called before recreating a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionRecreateEventListener {
	void onPreRecreateCollection(PreCollectionRecreateEvent event);
}
