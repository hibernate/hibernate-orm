/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

public final class OrphanRemovalAction extends EntityDeleteAction {

	public OrphanRemovalAction(Serializable id, Object[] state, Object version, Object instance,
			EntityPersister persister, boolean isCascadeDeleteEnabled, SessionImplementor session) {
		super( id, state, version, instance, persister, isCascadeDeleteEnabled, session );
	}
}
