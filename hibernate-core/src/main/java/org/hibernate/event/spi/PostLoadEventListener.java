/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

/**
 * Occurs after an entity instance is fully loaded.
 *
 * @author Kabir Khan
 */
public interface PostLoadEventListener {
	void onPostLoad(PostLoadEvent event);
}
