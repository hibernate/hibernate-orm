/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import java.util.List;

import org.hibernate.event.spi.EventSource;

/**
 * Loader subtype for loading multiple entities by multiple identifier values.
 */
public interface MultiIdEntityLoader<T> extends EntityMultiLoader<T> {
	/**
	 * Load multiple entities by id.  The exact result depends on the passed options.
	 */
	<K> List<T> load(K[] ids, MultiIdLoadOptions options, EventSource session);
}
