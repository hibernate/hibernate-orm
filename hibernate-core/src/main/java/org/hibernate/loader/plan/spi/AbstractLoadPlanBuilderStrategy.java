/*
 * jDocBook, processing of DocBook sources
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import java.util.ArrayDeque;

import org.hibernate.HibernateException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CompositeDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractLoadPlanBuilderStrategy implements LoadPlanBuilderStrategy {
	private final SessionFactoryImplementor sessionFactory;

	private ArrayDeque<FetchOwner> fetchOwnerStack = new ArrayDeque<FetchOwner>();

	protected AbstractLoadPlanBuilderStrategy(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	protected FetchOwner currentFetchOwner() {
		return fetchOwnerStack.peekLast();
	}

	@Override
	public void start() {
		// nothing to do
	}

	@Override
	public void finish() {
		// nothing to do
	}

	@Override
	public void startingEntity(EntityDefinition entityDefinition) {
		if ( fetchOwnerStack.isEmpty() ) {
			// this is a root...
			if ( ! supportsRootEntityReturns() ) {
				throw new HibernateException( "This strategy does not support root entity returns" );
			}
			final EntityReturn entityReturn = buildRootEntityReturn( entityDefinition );
			addRootReturn( entityReturn );
			fetchOwnerStack.push( entityReturn );
		}
		// otherwise this call should represent a fetch which should have been handled in #startingAttribute
	}

	protected boolean supportsRootEntityReturns() {
		return false;
	}

	protected abstract void addRootReturn(Return rootReturn);

	@Override
	public void finishingEntity(EntityDefinition entityDefinition) {
		// nothing to do
	}

	@Override
	public void startingCollection(CollectionDefinition collectionDefinition) {
		if ( fetchOwnerStack.isEmpty() ) {
			// this is a root...
			if ( ! supportsRootCollectionReturns() ) {
				throw new HibernateException( "This strategy does not support root collection returns" );
			}
			final CollectionReturn collectionReturn = buildRootCollectionReturn( collectionDefinition );
			addRootReturn( collectionReturn );
			fetchOwnerStack.push( collectionReturn );
		}
	}

	protected boolean supportsRootCollectionReturns() {
		return false;
	}

	@Override
	public void finishingCollection(CollectionDefinition collectionDefinition) {
		// nothing to do
	}

	@Override
	public void startingComposite(CompositeDefinition compositeDefinition) {
		if ( fetchOwnerStack.isEmpty() ) {
			throw new HibernateException( "A component cannot be the root of a walk nor a graph" );
		}
	}

	@Override
	public void finishingComposite(CompositeDefinition compositeDefinition) {
		// nothing to do
	}

	@Override
	public boolean startingAttribute(AttributeDefinition attributeDefinition) {
		final Type attributeType = attributeDefinition.getType();

		final boolean isComponentType = attributeType.isComponentType();
		final boolean isBasicType = ! ( isComponentType || attributeType.isAssociationType() );

		if ( isBasicType ) {
			return true;
		}
		else if ( isComponentType ) {
			return handleCompositeAttribute( (CompositeDefinition) attributeDefinition );
		}
		else {
			return handleAssociationAttribute( (AssociationAttributeDefinition) attributeDefinition );
		}
	}


	@Override
	public void finishingAttribute(AttributeDefinition attributeDefinition) {
		final Type attributeType = attributeDefinition.getType();

		final boolean isComponentType = attributeType.isComponentType();
		final boolean isBasicType = ! ( isComponentType || attributeType.isAssociationType() );

		if ( ! isBasicType ) {
			fetchOwnerStack.removeLast();
		}
	}

	protected boolean handleCompositeAttribute(CompositeDefinition attributeDefinition) {
		final FetchOwner fetchOwner = fetchOwnerStack.peekLast();
		final CompositeFetch fetch = buildCompositeFetch( fetchOwner, attributeDefinition );
		fetchOwnerStack.addLast( fetch );
		return true;
	}

	protected boolean handleAssociationAttribute(AssociationAttributeDefinition attributeDefinition) {
		final FetchStrategy fetchStrategy = determineFetchPlan( attributeDefinition );
		if ( fetchStrategy.getTiming() != FetchTiming.IMMEDIATE ) {
			return false;
		}

		final FetchOwner fetchOwner = fetchOwnerStack.peekLast();
		fetchOwner.validateFetchPlan( fetchStrategy );

		final Fetch associationFetch;
		if ( attributeDefinition.isCollection() ) {
			associationFetch = buildCollectionFetch( fetchOwner, attributeDefinition, fetchStrategy );
		}
		else {
			associationFetch = buildEntityFetch( fetchOwner, attributeDefinition, fetchStrategy );
		}
		fetchOwnerStack.addLast( associationFetch );

		return true;
	}

	protected abstract FetchStrategy determineFetchPlan(AssociationAttributeDefinition attributeDefinition);

	protected int currentDepth() {
		return fetchOwnerStack.size();
	}

	protected boolean isTooManyCollections() {
		return false;
	}

	protected abstract EntityReturn buildRootEntityReturn(EntityDefinition entityDefinition);

	protected abstract CollectionReturn buildRootCollectionReturn(CollectionDefinition collectionDefinition);

	protected abstract CollectionFetch buildCollectionFetch(
			FetchOwner fetchOwner,
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy);

	protected abstract EntityFetch buildEntityFetch(
			FetchOwner fetchOwner,
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy);

	protected abstract CompositeFetch buildCompositeFetch(FetchOwner fetchOwner, CompositeDefinition attributeDefinition);
}
