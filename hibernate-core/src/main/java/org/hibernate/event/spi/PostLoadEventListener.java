/*
 * SPDX-License-Identifier: Apache-2.0
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
