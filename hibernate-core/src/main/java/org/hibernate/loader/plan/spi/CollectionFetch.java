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

import org.jboss.logging.Logger;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.type.CollectionType;

/**
 * @author Steve Ebersole
 */
public class CollectionFetch extends AbstractCollectionReference implements Fetch {
	private static final Logger log = CoreLogging.logger( CollectionFetch.class );

	private final FetchOwner fetchOwner;
	private final AttributeDefinition fetchedAttribute;
	private final FetchStrategy fetchStrategy;

	public CollectionFetch(
			SessionFactoryImplementor sessionFactory,
			LockMode lockMode,
			FetchOwner fetchOwner,
			FetchStrategy fetchStrategy,
			AttributeDefinition fetchedAttribute) {
		super(
				sessionFactory,
				lockMode,
				sessionFactory.getCollectionPersister( ( (CollectionType) fetchedAttribute.getType() ).getRole() ),
				fetchOwner.getPropertyPath().append( fetchedAttribute.getName() ),
				(EntityReference) fetchOwner
		);
		this.fetchOwner = fetchOwner;
		this.fetchedAttribute = fetchedAttribute;
		this.fetchStrategy = fetchStrategy;
		fetchOwner.addFetch( this );
	}

	protected CollectionFetch(CollectionFetch original, CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		super( original, copyContext );
		this.fetchOwner = fetchOwnerCopy;
		this.fetchedAttribute = original.fetchedAttribute;
		this.fetchStrategy = original.fetchStrategy;
	}

	@Override
	public FetchOwner getOwner() {
		return fetchOwner;
	}

	@Override
	public CollectionType getFetchedType() {
		return (CollectionType) fetchedAttribute.getType();
	}

	@Override
	public boolean isNullable() {
		return true;
	}

	@Override
	public String getAdditionalJoinConditions() {
		// only pertinent for HQL...
		return null;
	}

	@Override
	public String[] toSqlSelectFragments(String alias) {
		return getOwner().toSqlSelectFragmentResolver().toSqlSelectFragments( alias, fetchedAttribute );
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

//	@Override
//	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
//		//To change body of implemented methods use File | Settings | File Templates.
//	}
//
//	@Override
//	public Object resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
//		return null;
//	}
//
//	@Override
//	public void read(ResultSet resultSet, ResultSetProcessingContext context, Object owner) throws SQLException {
//		final Serializable collectionRowKey = (Serializable) getCollectionPersister().readKey(
//				resultSet,
//				context.getAliasResolutionContext().resolveAliases( this ).getCollectionColumnAliases().getSuffixedKeyAliases(),
//				context.getSession()
//		);
//
//		final PersistenceContext persistenceContext = context.getSession().getPersistenceContext();
//
//		if ( collectionRowKey == null ) {
//			// we did not find a collection element in the result set, so we
//			// ensure that a collection is created with the owner's identifier,
//			// since what we have is an empty collection
//			final EntityKey ownerEntityKey = findOwnerEntityKey( context );
//			if ( ownerEntityKey == null ) {
//				// should not happen
//				throw new IllegalStateException(
//						"Could not locate owner's EntityKey during attempt to read collection element fro JDBC row : " +
//								getPropertyPath().getFullPath()
//				);
//			}
//
//			if ( log.isDebugEnabled() ) {
//				log.debugf(
//						"Result set contains (possibly empty) collection: %s",
//						MessageHelper.collectionInfoString(
//								getCollectionPersister(),
//								ownerEntityKey,
//								context.getSession().getFactory()
//						)
//				);
//			}
//
//			persistenceContext.getLoadContexts()
//					.getCollectionLoadContext( resultSet )
//					.getLoadingCollection( getCollectionPersister(), ownerEntityKey );
//		}
//		else {
//			// we found a collection element in the result set
//			if ( log.isDebugEnabled() ) {
//				log.debugf(
//						"Found row of collection: %s",
//						MessageHelper.collectionInfoString(
//								getCollectionPersister(),
//								collectionRowKey,
//								context.getSession().getFactory()
//						)
//				);
//			}
//
//			PersistentCollection rowCollection = persistenceContext.getLoadContexts()
//					.getCollectionLoadContext( resultSet )
//					.getLoadingCollection( getCollectionPersister(), collectionRowKey );
//
//			final CollectionAliases descriptor = context.getAliasResolutionContext().resolveAliases( this ).getCollectionColumnAliases();
//
//			if ( rowCollection != null ) {
//				final Object element = rowCollection.readFrom( resultSet, getCollectionPersister(), descriptor, owner );
//
//				if ( getElementGraph() != null ) {
//					for ( Fetch fetch : getElementGraph().getFetches() ) {
//						fetch.read( resultSet, context, element );
//					}
//				}
//			}
//		}
//	}
//
//	private EntityKey findOwnerEntityKey(ResultSetProcessingContext context) {
//		return context.getProcessingState( findOwnerEntityReference( getOwner() ) ).getEntityKey();
//	}
//
//	private EntityReference findOwnerEntityReference(FetchOwner owner) {
//		return Helper.INSTANCE.findOwnerEntityReference( owner );
//	}

	@Override
	public CollectionFetch makeCopy(CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		copyContext.getReturnGraphVisitationStrategy().startingCollectionFetch( this );
		final CollectionFetch copy = new CollectionFetch( this, copyContext, fetchOwnerCopy );
		copyContext.getReturnGraphVisitationStrategy().finishingCollectionFetch( this );
		return copy;
	}
}
