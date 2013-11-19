/*
 * Hibernate, Relational Persistence for Idiomatic Java
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
package org.hibernate.loader.plan2.build.internal.returns;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.spi.AnyFetch;
import org.hibernate.loader.plan2.spi.BidirectionalEntityReference;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.type.AnyType;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class AnyFetchImpl implements AnyFetch {
	/**
	 * Convenient constant for returning no fetches from {@link #getFetches()}
	 */
	private static final Fetch[] NO_FETCHES = new Fetch[0];

	/**
	 * Convenient constant for returning no fetches from {@link #getFetches()}
	 */
	private static final BidirectionalEntityReference[] NO_BIDIRECTIONAL_ENTITY_REFERENCES =
			new BidirectionalEntityReference[0];


	private final FetchSource fetchSource;
	private final AssociationAttributeDefinition fetchedAttribute;
	private final FetchStrategy fetchStrategy;

	private final PropertyPath propertyPath;

	public AnyFetchImpl(
			FetchSource fetchSource,
			AssociationAttributeDefinition fetchedAttribute,
			FetchStrategy fetchStrategy) {

		this.fetchSource = fetchSource;
		this.fetchedAttribute = fetchedAttribute;
		this.fetchStrategy = fetchStrategy;
		this.propertyPath = fetchSource.getPropertyPath().append( fetchedAttribute.getName() );
	}

	@Override
	public FetchSource getSource() {
		return fetchSource;
	}

	@Override
	public AnyType getFetchedType() {
		return (AnyType) fetchedAttribute.getType();
	}

	@Override
	public boolean isNullable() {
		return fetchedAttribute.isNullable();
	}

	@Override
	public String[] toSqlSelectFragments(String alias) {
		return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getAdditionalJoinConditions() {
		// only pertinent for HQL...
		return null;
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public String getQuerySpaceUid() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Fetch[] getFetches() {
		return NO_FETCHES;
	}

	@Override
	public BidirectionalEntityReference[] getBidirectionalEntityReferences() {
		return NO_BIDIRECTIONAL_ENTITY_REFERENCES;
	}

	@Override
	public EntityReference resolveEntityReference() {
		return fetchSource.resolveEntityReference();
	}
}
