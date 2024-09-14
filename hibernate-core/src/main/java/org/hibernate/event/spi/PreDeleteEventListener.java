/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

/**
 * Called before deleting an item from the datastore
 *
 * @author Gavin King
 */
public interface PreDeleteEventListener {
	/**
	 * Return true if the operation should be vetoed
	 */
	boolean onPreDelete(PreDeleteEvent event);
}
