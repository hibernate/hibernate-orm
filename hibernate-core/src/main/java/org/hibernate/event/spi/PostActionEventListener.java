/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Andrea Boriero
 */
interface PostActionEventListener {

	/**
	 * Does this listener require that after transaction hooks be registered?
	 *
	 * @param persister The persister for the entity in question.
	 *
	 * @return {@code true} if after transaction callbacks should be added.
	 *
	 * @deprecated use {@link #requiresPostCommitHandling(EntityPersister)}
	 */
	@Deprecated
	boolean requiresPostCommitHanding(EntityPersister persister);

	/**
	 * Does this listener require that after transaction hooks be registered?
	 *
	 * @param persister The persister for the entity in question.
	 *
	 * @return {@code true} if after transaction callbacks should be added.
	 */
	default boolean requiresPostCommitHandling(EntityPersister persister) {
		return requiresPostCommitHanding( persister );
	}
}
