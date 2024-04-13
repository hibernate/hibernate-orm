/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
