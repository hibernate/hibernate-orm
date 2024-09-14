/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
