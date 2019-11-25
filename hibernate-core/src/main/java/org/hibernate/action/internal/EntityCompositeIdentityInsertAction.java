/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * The action for performing entity insertions when entity is using IDENTITY column identifier generation
 * and we have a composite key.
 *
 * @see EntityInsertAction
 */
public final class EntityCompositeIdentityInsertAction extends EntityIdentityInsertAction  {

	/**
	 * Constructs an EntityCompositeIdentityInsertAction
	 *
	 * @param state The current (extracted) entity state
	 * @param instance The entity instance
	 * @param persister The entity persister
	 * @param isVersionIncrementDisabled Whether version incrementing is disabled
	 * @param session The session
	 * @param isDelayed Are we in a situation which allows the insertion to be delayed?
	 * @param partialId Optional partial id. Used for composite nested identity generation.
	 *
	 * @throws HibernateException Indicates an illegal state
	 */
	public EntityCompositeIdentityInsertAction(
			Object[] state,
			Object instance,
			EntityPersister persister,
			boolean isVersionIncrementDisabled,
			SharedSessionContractImplementor session,
			boolean isDelayed,
			Serializable partialId) {
		super( state, instance, persister, isVersionIncrementDisabled, session, isDelayed, partialId);
	}

	@Override
	protected Serializable generateId(EntityPersister persister, SharedSessionContractImplementor session, Object instance) {
		return persister.insert( getState(), instance, session, getId() );
	}
}
