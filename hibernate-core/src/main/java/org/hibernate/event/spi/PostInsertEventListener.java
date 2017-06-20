/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Called after insterting an item in the datastore
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface PostInsertEventListener extends Serializable {
	public void onPostInsert(PostInsertEvent event);

	/**
	 * Does this listener require that after transaction hooks be registered?   Typically this is {@code true}
	 * for post-insert event listeners, but may not be, for example, in JPA cases where there are no callbacks defined
	 * for the particular entity.
	 *
	 * @param persister The persister for the entity in question.
	 *
	 * @return {@code true} if after transaction callbacks should be added.
	 */
	public boolean requiresPostCommitHanding(EntityPersister persister);
}
