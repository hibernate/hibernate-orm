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

import org.hibernate.HibernateException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.type.Type;

/**
 * Represents a singular attribute that is both a {@link FetchOwner} and a {@link Fetch}.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractSingularAttributeFetch extends AbstractFetchOwner implements Fetch {
	private final FetchOwner owner;
	private final AttributeDefinition fetchedAttribute;
	private final FetchStrategy fetchStrategy;

	private final PropertyPath propertyPath;

	/**
	 * Constructs an {@link AbstractSingularAttributeFetch} object.
	 *
	 * @param factory - the session factory.
	 * @param owner - the fetch owner for this fetch.
	 * @param fetchedAttribute - the attribute being fetched
	 * @param fetchStrategy - the fetch strategy for this fetch.
	 */
	public AbstractSingularAttributeFetch(
			SessionFactoryImplementor factory,
			FetchOwner owner,
			AttributeDefinition fetchedAttribute,
			FetchStrategy fetchStrategy) {
		super( factory );
		this.owner = owner;
		this.fetchedAttribute = fetchedAttribute;
		this.fetchStrategy = fetchStrategy;

		owner.addFetch( this );

		this.propertyPath = owner.getPropertyPath().append( fetchedAttribute.getName() );
	}

	public AbstractSingularAttributeFetch(
			AbstractSingularAttributeFetch original,
			CopyContext copyContext,
			FetchOwner fetchOwnerCopy) {
		super( original, copyContext );
		this.owner = fetchOwnerCopy;
		this.fetchedAttribute = original.fetchedAttribute;
		this.fetchStrategy = original.fetchStrategy;
		this.propertyPath = original.propertyPath;
	}

	@Override
	public FetchOwner getOwner() {
		return owner;
	}

	public AttributeDefinition getFetchedAttribute() {
		return fetchedAttribute;
	}

	@Override
	public Type getFetchedType() {
		return fetchedAttribute.getType();
	}

	@Override
	public boolean isNullable() {
		return fetchedAttribute.isNullable();
//		return owner.isNullable( this );
	}

	@Override
	public String[] toSqlSelectFragments(String alias) {
		return owner.toSqlSelectFragments( this, alias );
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public String getAdditionalJoinConditions() {
		// only pertinent for HQL...
		return null;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
		if ( fetchStrategy.getStyle() == FetchStyle.JOIN ) {
			if ( this.fetchStrategy.getStyle() != FetchStyle.JOIN ) {

				throw new HibernateException(
						String.format(
								"Cannot specify join fetch from owner [%s] that is a non-joined fetch : %s",
								getPropertyPath().getFullPath(),
								attributeDefinition.getName()
						)
				);
			}
		}
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public String toString() {
		return "Fetch(" + propertyPath.getFullPath() + ")";
	}
}
