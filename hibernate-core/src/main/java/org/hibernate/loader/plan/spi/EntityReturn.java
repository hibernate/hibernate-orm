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

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.WrongClassException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.internal.LoadPlanBuildingHelper;
import org.hibernate.loader.spi.ResultSetProcessingContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;

import static org.hibernate.loader.spi.ResultSetProcessingContext.IdentifierResolutionContext;

/**
 * @author Steve Ebersole
 */
public class EntityReturn extends AbstractFetchOwner implements Return, FetchOwner, EntityReference {
	private final EntityAliases entityAliases;
	private final String sqlTableAlias;

	private final EntityPersister persister;

	private final PropertyPath propertyPath = new PropertyPath(); // its a root

	private IdentifierDescription identifierDescription;

	public EntityReturn(
			SessionFactoryImplementor sessionFactory,
			String alias,
			LockMode lockMode,
			String entityName,
			String sqlTableAlias,
			EntityAliases entityAliases) {
		super( sessionFactory, alias, lockMode );
		this.entityAliases = entityAliases;
		this.sqlTableAlias = sqlTableAlias;

		this.persister = sessionFactory.getEntityPersister( entityName );
	}

	@Override
	public String getAlias() {
		return super.getAlias();
	}

	@Override
	public LockMode getLockMode() {
		return super.getLockMode();
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
	public EntityAliases getEntityAliases() {
		return entityAliases;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy) {
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return getEntityPersister();
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
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
	public void resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		final IdentifierResolutionContext identifierResolutionContext = context.getIdentifierResolutionContext( this );
		EntityKey entityKey = identifierResolutionContext.getEntityKey();
		if ( entityKey != null ) {
			return;
		}

		entityKey = identifierDescription.resolve( resultSet, context );
		identifierResolutionContext.registerEntityKey( entityKey );

		for ( Fetch fetch : getFetches() ) {
			fetch.resolve( resultSet, context );
		}
	}

	@Override
	public Object read(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		final IdentifierResolutionContext identifierResolutionContext = context.getIdentifierResolutionContext( this );
		EntityKey entityKey = identifierResolutionContext.getEntityKey();
		if ( entityKey == null ) {
			throw new AssertionFailure( "Could not locate resolved EntityKey");
		}

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
							entityAliases,
							entityKey,
							existing
					);
					//we need to upgrade the lock mode to the mode requested
					context.getSession().getPersistenceContext().getEntry( existing ).setLockMode( getLockMode() );
				}
			}

			return existing;
		}
		else {
			final String concreteEntityTypeName = context.getConcreteEntityTypeName(
					resultSet,
					persister,
					entityAliases,
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
					entityAliases,
					acquiredLockMode,
					persister,
					true,
					persister.getEntityMetamodel().getEntityType()
			);

			// materialize associations (and initialize the object) later
			context.registerHydratedEntity( persister, entityKey, entityInstance );

			return entityInstance;
		}
	}

	@Override
	public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
		this.identifierDescription = identifierDescription;
	}

	@Override
	public String toString() {
		return "EntityReturn(" + persister.getEntityName() + ")";
	}
}
