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
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.EntityType;

import static org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext.EntityReferenceProcessingState;

/**
 * Represents a {@link Fetch} for an entity association attribute as well as a
 * {@link FetchOwner} of the entity association sub-attribute fetches.

 * @author Steve Ebersole
 */
public class EntityFetch extends AbstractSingularAttributeFetch implements EntityReference, Fetch {
	private final EntityPersister persister;
	private final EntityPersisterBasedSqlSelectFragmentResolver sqlSelectFragmentResolver;

	private IdentifierDescription identifierDescription;

	// todo : remove these
	private final LockMode lockMode;

	/**
	 * Constructs an {@link EntityFetch} object.
	 *
	 * @param sessionFactory - the session factory.
	 * @param lockMode - the lock mode.
	 * @param owner - the fetch owner for this fetch.
	 * @param fetchedAttribute - the attribute being fetched
	 * @param fetchStrategy - the fetch strategy for this fetch.
	 */
	public EntityFetch(
			SessionFactoryImplementor sessionFactory,
			LockMode lockMode,
			FetchOwner owner,
			AttributeDefinition fetchedAttribute,
			FetchStrategy fetchStrategy) {
		super( sessionFactory, owner, fetchedAttribute, fetchStrategy );

		this.persister = sessionFactory.getEntityPersister( getFetchedType().getAssociatedEntityName() );
		if ( persister == null ) {
			throw new WalkingException(
					String.format(
							"Unable to locate EntityPersister [%s] for fetch [%s]",
							getFetchedType().getAssociatedEntityName(),
							fetchedAttribute.getName()
					)
			);
		}
		this.sqlSelectFragmentResolver = new EntityPersisterBasedSqlSelectFragmentResolver( (Queryable) persister );

		this.lockMode = lockMode;
	}

	/**
	 * Copy constructor.
	 *
	 * @param original The original fetch
	 * @param copyContext Access to contextual needs for the copy operation
	 */
	protected EntityFetch(EntityFetch original, CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		super( original, copyContext, fetchOwnerCopy );
		this.persister = original.persister;
		this.sqlSelectFragmentResolver = original.sqlSelectFragmentResolver;

		this.lockMode = original.lockMode;
	}

	@Override
	public EntityType getFetchedType() {
		return (EntityType) super.getFetchedType();
	}

	@Override
	public String[] toSqlSelectFragments(String alias) {
		return getOwner().toSqlSelectFragmentResolver().toSqlSelectFragments( alias, getFetchedAttribute() );
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
	public SqlSelectFragmentResolver toSqlSelectFragmentResolver() {
		return sqlSelectFragmentResolver;
	}

	@Override
	public IdentifierDescription getIdentifierDescription() {
		return identifierDescription;
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return persister;
	}

	@Override
	public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
		this.identifierDescription = identifierDescription;
	}

//	@Override
//	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
//		identifierDescription.hydrate( resultSet, context );
//
//		for ( Fetch fetch : getFetches() ) {
//			fetch.hydrate( resultSet, context );
//		}
//	}
//
//	@Override
//	public EntityKey resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
//		final ResultSetProcessingContext.EntityReferenceProcessingState entityReferenceProcessingState = context.getProcessingState(
//				this
//		);
//		EntityKey entityKey = entityReferenceProcessingState.getEntityKey();
//		if ( entityKey == null ) {
//			entityKey = identifierDescription.resolve( resultSet, context );
//			if ( entityKey == null ) {
//				// register the non-existence (though only for one-to-one associations)
//				if ( getFetchedType().isOneToOne() ) {
//					// first, find our owner's entity-key...
//					final EntityKey ownersEntityKey = context.getProcessingState( (EntityReference) getOwner() ).getEntityKey();
//					if ( ownersEntityKey != null ) {
//						context.getSession().getPersistenceContext()
//								.addNullProperty( ownersEntityKey, getFetchedType().getPropertyName() );
//					}
//				}
//			}
//
//			entityReferenceProcessingState.registerEntityKey( entityKey );
//
//			for ( Fetch fetch : getFetches() ) {
//				fetch.resolve( resultSet, context );
//			}
//		}
//
//		return entityKey;
//	}
//
//	@Override
//	public void read(ResultSet resultSet, ResultSetProcessingContext context, Object owner) throws SQLException {
//		final EntityReferenceProcessingState entityReferenceProcessingState = context.getProcessingState( this );
//		final EntityKey entityKey = entityReferenceProcessingState.getEntityKey();
//		if ( entityKey == null ) {
//			return;
//		}
//		final Object entity = context.resolveEntityKey( entityKey, this );
//		for ( Fetch fetch : getFetches() ) {
//			fetch.read( resultSet, context, entity );
//		}
//	}

//	/**
//	 * Resolve any fetches required to resolve the identifier as well
//	 * as the entity key for this fetch..
//	 *
//	 * @param resultSet - the result set.
//	 * @param context - the result set processing context.
//	 * @return the entity key for this fetch.
//	 *
//	 * @throws SQLException
//	 */
//	public EntityKey resolveInIdentifier(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
//		// todo : may not need to do this if entitykey is already part of the resolution context
//
//		final EntityKey entityKey = resolve( resultSet, context );
//
//		final Object existing = context.getSession().getEntityUsingInterceptor( entityKey );
//
//		if ( existing != null ) {
//			if ( !persister.isInstance( existing ) ) {
//				throw new WrongClassException(
//						"loaded object was of wrong class " + existing.getClass(),
//						entityKey.getIdentifier(),
//						persister.getEntityName()
//				);
//			}
//
//			if ( getLockMode() != null && getLockMode() != LockMode.NONE ) {
//				final boolean isVersionCheckNeeded = persister.isVersioned()
//						&& context.getSession().getPersistenceContext().getEntry( existing ).getLockMode().lessThan( getLockMode() );
//
//				// we don't need to worry about existing version being uninitialized because this block isn't called
//				// by a re-entrant load (re-entrant loads _always_ have lock mode NONE)
//				if ( isVersionCheckNeeded ) {
//					//we only check the version when _upgrading_ lock modes
//					context.checkVersion(
//							resultSet,
//							persister,
//							context.getAliasResolutionContext().resolveAliases( this ).getColumnAliases(),
//							entityKey,
//							existing
//					);
//					//we need to upgrade the lock mode to the mode requested
//					context.getSession().getPersistenceContext().getEntry( existing ).setLockMode( getLockMode() );
//				}
//			}
//		}
//		else {
//			final String concreteEntityTypeName = context.getConcreteEntityTypeName(
//					resultSet,
//					persister,
//					context.getAliasResolutionContext().resolveAliases( this ).getColumnAliases(),
//					entityKey
//			);
//
//			final Object entityInstance = context.getSession().instantiate(
//					concreteEntityTypeName,
//					entityKey.getIdentifier()
//			);
//
//			//need to hydrate it.
//
//			// grab its state from the ResultSet and keep it in the Session
//			// (but don't yet initialize the object itself)
//			// note that we acquire LockMode.READ even if it was not requested
//			LockMode acquiredLockMode = getLockMode() == LockMode.NONE ? LockMode.READ : getLockMode();
//
//			context.loadFromResultSet(
//					resultSet,
//					entityInstance,
//					concreteEntityTypeName,
//					entityKey,
//					context.getAliasResolutionContext().resolveAliases( this ).getColumnAliases(),
//					acquiredLockMode,
//					persister,
//					getFetchStrategy(),
//					true,
//					getFetchedType()
//			);
//
//			// materialize associations (and initialize the object) later
//			context.registerHydratedEntity( this, entityKey, entityInstance );
//		}
//
//		return entityKey;
//	}

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
