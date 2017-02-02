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
 * Called afterQuery updating the datastore
 * 
 * @author Gavin King
 */
public interface PostUpdateEventListener extends Serializable {
	public void onPostUpdate(PostUpdateEvent event);

	public boolean requiresPostCommitHanding(EntityPersister persister);
}
