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
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.internal.LoadPlanBuildingHelper;
import org.hibernate.loader.plan.spi.build.LoadPlanBuildingContext;
import org.hibernate.loader.spi.ResultSetProcessingContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;

import static org.hibernate.loader.spi.ResultSetProcessingContext.IdentifierResolutionContext;

/**
 * @author Steve Ebersole
 */
public class EntityReturn extends AbstractFetchOwner implements Return, EntityReference, CopyableReturn {

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

	protected EntityReturn(EntityReturn original, CopyContext copyContext) {
		super( original, copyContext );
		this.entityAliases = original.entityAliases;
		this.sqlTableAlias = original.sqlTableAlias;
		this.persister = original.persister;
	}

	@Override
	public String getAlias() {
		return super.getAlias();
	}

	@Override
	public String getSqlTableAlias() {
		return sqlTableAlias;
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
			String sqlTableAlias,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return LoadPlanBuildingHelper.buildStandardEntityFetch(
				this,
				attributeDefinition,
				fetchStrategy,
				sqlTableAlias,
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
		Object objectForThisEntityReturn = null;
		for ( IdentifierResolutionContext identifierResolutionContext : context.getIdentifierResolutionContexts() ) {
			final EntityReference entityReference = identifierResolutionContext.getEntityReference();
			final EntityKey entityKey = identifierResolutionContext.getEntityKey();
			if ( entityKey == null ) {
				throw new AssertionFailure( "Could not locate resolved EntityKey");
			}
			final Object object =  context.resolveEntityKey( entityKey, entityReference );
			if ( this == entityReference ) {
				objectForThisEntityReturn = object;
			}
		}
		return objectForThisEntityReturn;
	}

	@Override
	public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
		this.identifierDescription = identifierDescription;
	}

	@Override
	public String toString() {
		return "EntityReturn(" + persister.getEntityName() + ")";
	}

	@Override
	public EntityReturn makeCopy(CopyContext copyContext) {
		return new EntityReturn( this, copyContext );
	}
}
