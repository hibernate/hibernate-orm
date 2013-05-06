/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.criterion;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Base expression implementation for (not) emptiness checking of collection properties
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEmptinessExpression implements Criterion {

	private static final TypedValue[] NO_VALUES = new TypedValue[0];

	protected final String propertyName;

	protected AbstractEmptinessExpression(String propertyName) {
		this.propertyName = propertyName;
	}

	/**
	 * Should empty rows be excluded?
	 *
	 * @return {@code true} Indicates the expression should be 'exists'; {@code false} indicates 'not exists'
	 */
	protected abstract boolean excludeEmpty();

	@Override
	public final String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String entityName = criteriaQuery.getEntityName( criteria, propertyName );
		final String actualPropertyName = criteriaQuery.getPropertyName( propertyName );
		final String sqlAlias = criteriaQuery.getSQLAlias( criteria, propertyName );

		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		final QueryableCollection collectionPersister = getQueryableCollection( entityName, actualPropertyName, factory );

		final String[] collectionKeys = collectionPersister.getKeyColumnNames();
		final String[] ownerKeys = ( (Loadable) factory.getEntityPersister( entityName ) ).getIdentifierColumnNames();

		final String innerSelect = "(select 1 from " + collectionPersister.getTableName() + " where "
				+ new ConditionFragment().setTableAlias( sqlAlias ).setCondition( ownerKeys, collectionKeys ).toFragmentString()
				+ ")";

		return excludeEmpty()
				? "exists " + innerSelect
				: "not exists " + innerSelect;
	}


	protected QueryableCollection getQueryableCollection(
			String entityName,
			String propertyName,
			SessionFactoryImplementor factory) throws HibernateException {
		final PropertyMapping ownerMapping = (PropertyMapping) factory.getEntityPersister( entityName );
		final Type type = ownerMapping.toType( propertyName );
		if ( !type.isCollectionType() ) {
			throw new MappingException(
					"Property path [" + entityName + "." + propertyName + "] does not reference a collection"
			);
		}

		final String role = ( (CollectionType) type ).getRole();
		try {
			return (QueryableCollection) factory.getCollectionPersister( role );
		}
		catch ( ClassCastException cce ) {
			throw new QueryException( "collection role is not queryable: " + role );
		}
		catch ( Exception e ) {
			throw new QueryException( "collection role not found: " + role );
		}
	}

	@Override
	public final TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
			throws HibernateException {
		return NO_VALUES;
	}

	@Override
	public final String toString() {
		return propertyName + ( excludeEmpty() ? " is not empty" : " is empty" );
	}
}
