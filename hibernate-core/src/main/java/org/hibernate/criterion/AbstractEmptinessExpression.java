/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
