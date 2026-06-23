/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called before injecting property values into a newly
 * loaded entity instance.
 *
 * @author Gavin King
 */
public interface PreLoadEventListener {
	void onPreLoad(@Nonnull PreLoadEvent event);
}
