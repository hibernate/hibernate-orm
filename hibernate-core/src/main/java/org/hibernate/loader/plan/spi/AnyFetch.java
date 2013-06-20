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
package org.hibernate.loader.plan.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.persister.walking.spi.AnyMappingDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.type.AnyType;

/**
 * @author Steve Ebersole
 */
public class AnyFetch extends AbstractPlanNode implements Fetch {
	private final FetchOwner owner;
	private final AttributeDefinition fetchedAttribute;
	private final AnyMappingDefinition definition;
	private final FetchStrategy fetchStrategy;

	private final PropertyPath propertyPath;

	public AnyFetch(
			SessionFactoryImplementor sessionFactory,
			FetchOwner owner,
			AttributeDefinition ownerProperty,
			AnyMappingDefinition definition,
			FetchStrategy fetchStrategy) {
		super( sessionFactory );

		this.owner = owner;
		this.fetchedAttribute = ownerProperty;
		this.definition = definition;
		this.fetchStrategy = fetchStrategy;

		this.propertyPath = owner.getPropertyPath().append( ownerProperty.getName() );

		owner.addFetch( this );
	}

	/**
	 * Copy constructor.
	 *
	 * @param original The original fetch
	 * @param copyContext Access to contextual needs for the copy operation
	 */
	protected AnyFetch(AnyFetch original, CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		super( original );
		this.owner = fetchOwnerCopy;
		this.fetchedAttribute = original.fetchedAttribute;
		this.definition = original.definition;
		this.fetchStrategy = original.fetchStrategy;
		this.propertyPath = original.propertyPath;
	}

	@Override
	public FetchOwner getOwner() {
		return owner;
	}

	@Override
	public AnyType getFetchedType() {
		return (AnyType) fetchedAttribute.getType();
	}

	@Override
	public boolean isNullable() {
		return owner.isNullable( this );
	}

	@Override
	public String[] toSqlSelectFragments(String alias) {
		return owner.toSqlSelectFragments( this, alias );
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
	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		throw new NotYetImplementedException();
	}

	@Override
	public Object resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		throw new NotYetImplementedException();
	}

	@Override
	public void read(ResultSet resultSet, ResultSetProcessingContext context, Object owner) throws SQLException {
		throw new NotYetImplementedException();
	}

	@Override
	public AnyFetch makeCopy(CopyContext copyContext, FetchOwner fetchOwnerCopy) {
		copyContext.getReturnGraphVisitationStrategy().startingAnyFetch( this );
		final AnyFetch copy = new AnyFetch( this, copyContext, fetchOwnerCopy );
		copyContext.getReturnGraphVisitationStrategy().startingAnyFetch( this );
		return copy;
	}

}
