/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

/**
 * Called after inserting an item in the datastore
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface PostInsertEventListener extends Serializable, PostActionEventListener {
	void onPostInsert(PostInsertEvent event);
}
