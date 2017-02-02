/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

/**
 * Callback to allow the built DelayedDropAction, if indicated, to be registered
 * back with the SessionFactory (or the thing that will manage its later execution).
 *
 * @author Steve Ebersole
 */
public interface DelayedDropRegistry {
	/**
	 * Register the built DelayedDropAction
	 *
	 * @param action The delayed schema drop memento
	 */
	void registerOnCloseAction(DelayedDropAction action);
}
