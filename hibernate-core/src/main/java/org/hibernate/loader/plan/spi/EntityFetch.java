/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.LockMode;
import org.hibernate.WrongClassException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.internal.LoadPlanBuildingHelper;
import org.hibernate.loader.plan.spi.build.LoadPlanBuildingContext;
import org.hibernate.loader.spi.ResultSetProcessingContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public class EntityFetch extends AbstractSingularAttributeFetch implements EntityReference {

	private final EntityType associationType;
	private final EntityPersister persister;

	private IdentifierDescription identifierDescription;

	public EntityFetch(
			SessionFactoryImplementor sessionFactory,
			LockMode lockMode,
			FetchOwner owner,
			String ownerProperty,
			FetchStrategy fetchStrategy) {
		super( sessionFactory, lockMode, owner, ownerProperty, fetchStrategy );

		this.associationType = (EntityType) owner.retrieveFetchSourcePersister().getPropertyType( ownerProperty );
		this.persister = sessionFactory.getEntityPersister( associationType.getAssociatedEntityName() );
	}

	/**
	 * Copy constructor.
	 *
	 * @param original The original fetch
	 * @param copyContext Access to contextual needs for the copy operation
	 */
	protected EntityFetch(EntityFetch original, CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		super( original, copyContext, fetchOwnerCopy );
		this.associationType = original.associationType;
		this.persister = original.persister;
	}

	public EntityType getAssociationType() {
		return associationType;
	}

	@Override
	public EntityReference getEntityReference() {
		return this;
	}

	@Override
	public EntityPersister getEntityPersister() {
		return persister;
	}

	@Override
	public IdentifierDescription getIdentifierDescription() {
		return identifierDescription;
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return persister;
	}


	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return LoadPlanBuildingHelper.buildStandardCollectionFetch(
				this,
				attributeDefinition,
				fetchStrategy,
				loadPlanBuildingContext
		);
	}

	@Override
	public EntityFetch buildEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return LoadPlanBuildingHelper.buildStandardEntityFetch(
				this,
				attributeDefinition,
				fetchStrategy,
				loadPlanBuildingContext
		);
	}

	@Override
	public CompositeFetch buildCompositeFetch(
			CompositionDefinition attributeDefinition,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return LoadPlanBuildingHelper.buildStandardCompositeFetch( this, attributeDefinition, loadPlanBuildingContext );
	}

	@Override
	public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
		this.identifierDescription = identifierDescription;
	}

	@Override
	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		EntityKey entityKey = context.getDictatedRootEntityKey();
		if ( entityKey != null ) {
			context.getIdentifierResolutionContext( this ).registerEntityKey( entityKey );
			return;
		}

		identifierDescription.hydrate( resultSet, context );

		for ( Fetch fetch : getFetches() ) {
			fetch.hydrate( resultSet, context );
		}
	}

	@Override
	public EntityKey resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		final ResultSetProcessingContext.IdentifierResolutionContext identifierResolutionContext = context.getIdentifierResolutionContext( this );
		EntityKey entityKey = identifierResolutionContext.getEntityKey();
		if ( entityKey == null ) {
			entityKey = identifierDescription.resolve( resultSet, context );
			if ( entityKey == null ) {
				// register the non-existence (though only for one-to-one associations)
				if ( associationType.isOneToOne() ) {
					// first, find our owner's entity-key...
					final EntityKey ownersEntityKey = context.getIdentifierResolutionContext( (EntityReference) getOwner() ).getEntityKey();
					if ( ownersEntityKey != null ) {
						context.getSession().getPersistenceContext()
								.addNullProperty( ownersEntityKey, associationType.getPropertyName() );
					}
				}
			}

			identifierResolutionContext.registerEntityKey( entityKey );

			for ( Fetch fetch : getFetches() ) {
				fetch.resolve( resultSet, context );
			}
		}

		return entityKey;
	}

	public EntityKey resolveInIdentifier(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		// todo : may not need to do this if entitykey is already part of the resolution context

		final EntityKey entityKey = resolve( resultSet, context );

		final Object existing = context.getSession().getEntityUsingInterceptor( entityKey );

		if ( existing != null ) {
			if ( !persister.isInstance( existing ) ) {
				throw new WrongClassException(
						"loaded object was of wrong class " + existing.getClass(),
						entityKey.getIdentifier(),
						persister.getEntityName()
				);
			}

			if ( getLockMode() != null && getLockMode() != LockMode.NONE ) {
				final boolean isVersionCheckNeeded = persister.isVersioned()
						&& context.getSession().getPersistenceContext().getEntry( existing ).getLockMode().lessThan( getLockMode() );

				// we don't need to worry about existing version being uninitialized because this block isn't called
				// by a re-entrant load (re-entrant loads _always_ have lock mode NONE)
				if ( isVersionCheckNeeded ) {
					//we only check the version when _upgrading_ lock modes
					context.checkVersion(
							resultSet,
							persister,
							context.getLoadQueryAliasResolutionContext().resolveEntityColumnAliases( this ),
							entityKey,
							existing
					);
					//we need to upgrade the lock mode to the mode requested
					context.getSession().getPersistenceContext().getEntry( existing ).setLockMode( getLockMode() );
				}
			}
		}
		else {
			final String concreteEntityTypeName = context.getConcreteEntityTypeName(
					resultSet,
					persister,
					context.getLoadQueryAliasResolutionContext().resolveEntityColumnAliases( this ),
					entityKey
			);

			final Object entityInstance = context.getSession().instantiate(
					concreteEntityTypeName,
					entityKey.getIdentifier()
			);

			//need to hydrate it.

			// grab its state from the ResultSet and keep it in the Session
			// (but don't yet initialize the object itself)
			// note that we acquire LockMode.READ even if it was not requested
			LockMode acquiredLockMode = getLockMode() == LockMode.NONE ? LockMode.READ : getLockMode();

			context.loadFromResultSet(
					resultSet,
					entityInstance,
					concreteEntityTypeName,
					entityKey,
					context.getLoadQueryAliasResolutionContext().resolveEntityColumnAliases( this ),
					acquiredLockMode,
					persister,
					getFetchStrategy().getTiming() == FetchTiming.IMMEDIATE,
					associationType
			);

			// materialize associations (and initialize the object) later
			context.registerHydratedEntity( persister, entityKey, entityInstance );
		}

		return entityKey;
	}

	@Override
	public String toString() {
		return "EntityFetch(" + getPropertyPath().getFullPath() + " -> " + persister.getEntityName() + ")";
	}

	@Override
	public EntityFetch makeCopy(CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		copyContext.getReturnGraphVisitationStrategy().startingEntityFetch( this );
		final EntityFetch copy = new EntityFetch( this, copyContext, fetchOwnerCopy );
		copyContext.getReturnGraphVisitationStrategy().finishingEntityFetch( this );
		return copy;
	}
}
