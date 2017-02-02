/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.spi;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.plan.exec.spi.LockModeResolver;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public interface ResultSetProcessingContext extends LockModeResolver {
	SharedSessionContractImplementor getSession();

	QueryParameters getQueryParameters();

	boolean shouldUseOptionalEntityInformation();

	boolean shouldReturnProxies();

	LoadPlan getLoadPlan();

	/**
	 * Holds all pieces of information known about an entity reference in relation to each row as we process the
	 * result set.  Caches these values and makes it easy for access while processing Fetches.
	 */
	interface EntityReferenceProcessingState {
		/**
		 * The EntityReference for which this is collecting process state
		 *
		 * @return The EntityReference
		 */
		EntityReference getEntityReference();

		/**
		 * Register the fact that no identifier was found on attempt to hydrate it from ResultSet
		 */
		void registerMissingIdentifier();

		/**
		 *
		 * @return
		 */
		boolean isMissingIdentifier();

		/**
		 * Register the hydrated form (raw Type-read ResultSet values) of the entity's identifier for the row
		 * currently being processed.
		 *
		 * @param hydratedForm The entity identifier hydrated state
		 */
		void registerIdentifierHydratedForm(Object hydratedForm);

		/**
		 * Obtain the hydrated form (the raw Type-read ResultSet values) of the entity's identifier
		 *
		 * @return The entity identifier hydrated state
		 */
		Object getIdentifierHydratedForm();

		/**
		 * Register the processed EntityKey for this Entity for the row currently being processed.
		 *
		 * @param entityKey The processed EntityKey for this EntityReference
		 */
		void registerEntityKey(EntityKey entityKey);

		/**
		 * Obtain the registered EntityKey for this EntityReference for the row currently being processed.
		 *
		 * @return The registered EntityKey for this EntityReference
		 */
		EntityKey getEntityKey();

		void registerHydratedState(Object[] hydratedState);
		Object[] getHydratedState();

		// usually uninitialized at this point
		void registerEntityInstance(Object instance);

		// may be uninitialized
		Object getEntityInstance();

	}

	EntityReferenceProcessingState getProcessingState(EntityReference entityReference);

	/**
	 * Find the EntityReferenceProcessingState for the FetchOwner of the given Fetch.
	 *
	 * @param fetch The Fetch for which to find the EntityReferenceProcessingState of its FetchOwner.
	 *
	 * @return The FetchOwner's EntityReferenceProcessingState
	 */
	EntityReferenceProcessingState getOwnerProcessingState(Fetch fetch);


	void registerHydratedEntity(EntityReference entityReference, EntityKey entityKey, Object entityInstance);

	interface EntityKeyResolutionContext {
		EntityPersister getEntityPersister();
		LockMode getLockMode();
		EntityReference getEntityReference();
	}

//	public Object resolveEntityKey(EntityKey entityKey, EntityKeyResolutionContext entityKeyContext);


	// should be able to get rid of the methods below here from the interface ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

//	public void checkVersion(
//			ResultSet resultSet,
//			EntityPersister persister,
//			EntityAliases entityAliases,
//			EntityKey entityKey,
//			Object entityInstance) throws SQLException;
//
//	public String getConcreteEntityTypeName(
//			ResultSet resultSet,
//			EntityPersister persister,
//			EntityAliases entityAliases,
//			EntityKey entityKey) throws SQLException;
//
//	public void loadFromResultSet(
//			ResultSet resultSet,
//			Object entityInstance,
//			String concreteEntityTypeName,
//			EntityKey entityKey,
//			EntityAliases entityAliases,
//			LockMode acquiredLockMode,
//			EntityPersister persister,
//			FetchStrategy fetchStrategy,
//			boolean eagerFetch,
//			EntityType associationType) throws SQLException;
}
