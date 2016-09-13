/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A base class for entity insert actions.
 *
 * @author Gail Badner
 */
public abstract class AbstractEntityInsertAction extends EntityAction {
	private transient Object[] state;
	private final boolean isVersionIncrementDisabled;
	private boolean isExecuted;
	private boolean areTransientReferencesNullified;

	/**
	 * Constructs an AbstractEntityInsertAction object.
	 *
	 * @param id - the entity ID
	 * @param state - the entity state
	 * @param instance - the entity
	 * @param isVersionIncrementDisabled - true, if version increment should
	 *                                     be disabled; false, otherwise
	 * @param persister - the entity persister
	 * @param session - the session
	 */
	protected AbstractEntityInsertAction(
			Serializable id,
			Object[] state,
			Object instance,
			boolean isVersionIncrementDisabled,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		super( session, id, instance, persister );
		this.state = state;
		this.isVersionIncrementDisabled = isVersionIncrementDisabled;
		this.isExecuted = false;
		this.areTransientReferencesNullified = false;

		if ( id != null ) {
			handleNaturalIdPreSaveNotifications();
		}
	}

	/**
	 * Returns the entity state.
	 *
	 * NOTE: calling {@link #nullifyTransientReferencesIfNotAlready} can modify the
	 *       entity state.
	 * @return the entity state.
	 *
	 * @see {@link #nullifyTransientReferencesIfNotAlready}
	 */
	public Object[] getState() {
		return state;
	}

	/**
	 * Does this insert action need to be executed as soon as possible
	 * (e.g., to generate an ID)?
	 * @return true, if it needs to be executed as soon as possible;
	 *         false, otherwise.
	 */
	public abstract boolean isEarlyInsert();

	/**
	 * Find the transient unsaved entity dependencies that are non-nullable.
	 * @return the transient unsaved entity dependencies that are non-nullable,
	 *         or null if there are none.
	 */
	public NonNullableTransientDependencies findNonNullableTransientEntities() {
		return ForeignKeys.findNonNullableTransientEntities(
				getPersister().getEntityName(),
				getInstance(),
				getState(),
				isEarlyInsert(),
				getSession()
		);
	}

	/**
	 * Nullifies any references to transient entities in the entity state
	 * maintained by this action. References to transient entities
	 * should be nullified when an entity is made "managed" or when this
	 * action is executed, whichever is first.
	 * <p/>
	 * References will only be nullified the first time this method is
	 * called for a this object, so it can safely be called both when
	 * the entity is made "managed" and when this action is executed.
	 *
	 * @see {@link #makeEntityManaged() }
	 */
	protected final void nullifyTransientReferencesIfNotAlready() {
		if ( ! areTransientReferencesNullified ) {
			new ForeignKeys.Nullifier( getInstance(), false, isEarlyInsert(), getSession() )
					.nullifyTransientReferences( getState(), getPersister().getPropertyTypes() );
			new Nullability( getSession() ).checkNullability( getState(), getPersister(), false );
			areTransientReferencesNullified = true;
		}
	}

	/**
	 * Make the entity "managed" by the persistence context.
	 */
	public final void makeEntityManaged() {
		nullifyTransientReferencesIfNotAlready();
		final Object version = Versioning.getVersion( getState(), getPersister() );
		getSession().getPersistenceContext().addEntity(
				getInstance(),
				( getPersister().isMutable() ? Status.MANAGED : Status.READ_ONLY ),
				getState(),
				getEntityKey(),
				version,
				LockMode.WRITE,
				isExecuted,
				getPersister(),
				isVersionIncrementDisabled
		);
	}

	/**
	 * Indicate that the action has executed.
	 */
	protected void markExecuted() {
		this.isExecuted = true;
	}

	/**
	 * Returns the {@link EntityKey}.
	 * @return the {@link EntityKey}.
	 */
	protected abstract EntityKey getEntityKey();

	@Override
	public void afterDeserialize(SharedSessionContractImplementor session) {
		super.afterDeserialize( session );
		// IMPL NOTE: non-flushed changes code calls this method with session == null...
		// guard against NullPointerException
		if ( session != null ) {
			final EntityEntry entityEntry = session.getPersistenceContext().getEntry( getInstance() );
			this.state = entityEntry.getLoadedState();
		}
	}

	/**
	 * Handle sending notifications needed for natural-id beforeQuery saving
	 */
	protected void handleNaturalIdPreSaveNotifications() {
		// beforeQuery save, we need to add a local (transactional) natural id cross-reference
		getSession().getPersistenceContext().getNaturalIdHelper().manageLocalNaturalIdCrossReference(
				getPersister(),
				getId(),
				state,
				null,
				CachedNaturalIdValueSource.INSERT
		);
	}

	/**
	 * Handle sending notifications needed for natural-id afterQuery saving
	 *
	 * @param generatedId The generated entity identifier
	 */
	public void handleNaturalIdPostSaveNotifications(Serializable generatedId) {
		if ( isEarlyInsert() ) {
			// with early insert, we still need to add a local (transactional) natural id cross-reference
			getSession().getPersistenceContext().getNaturalIdHelper().manageLocalNaturalIdCrossReference(
					getPersister(),
					generatedId,
					state,
					null,
					CachedNaturalIdValueSource.INSERT
			);
		}
		// afterQuery save, we need to manage the shared cache entries
		getSession().getPersistenceContext().getNaturalIdHelper().manageSharedNaturalIdCrossReference(
				getPersister(),
				generatedId,
				state,
				null,
				CachedNaturalIdValueSource.INSERT
		);
	}
}
