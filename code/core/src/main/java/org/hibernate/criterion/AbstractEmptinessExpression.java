// $Id: AbstractEmptinessExpression.java 6670 2005-05-03 22:19:00Z steveebersole $
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Implementation of AbstractEmptinessExpression.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEmptinessExpression implements Criterion {

	private static final TypedValue[] NO_VALUES = new TypedValue[0];

	protected final String propertyName;

	protected AbstractEmptinessExpression(String propertyName) {
		this.propertyName = propertyName;
	}

	protected abstract boolean excludeEmpty();

	public final String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		String entityName = criteriaQuery.getEntityName( criteria, propertyName );
		String actualPropertyName = criteriaQuery.getPropertyName( propertyName );
		String sqlAlias = criteriaQuery.getSQLAlias( criteria, propertyName );

		SessionFactoryImplementor factory = criteriaQuery.getFactory();
		QueryableCollection collectionPersister = getQueryableCollection( entityName, actualPropertyName, factory );

		String[] collectionKeys = collectionPersister.getKeyColumnNames();
		String[] ownerKeys = ( ( Loadable ) factory.getEntityPersister( entityName ) ).getIdentifierColumnNames();

		String innerSelect = "(select 1 from " + collectionPersister.getTableName()
		        + " where "
		        + new ConditionFragment().setTableAlias( sqlAlias ).setCondition( ownerKeys, collectionKeys ).toFragmentString()
		        + ")";

		return excludeEmpty()
		        ? "exists " + innerSelect
		        : "not exists " + innerSelect;
	}


	protected QueryableCollection getQueryableCollection(String entityName, String propertyName, SessionFactoryImplementor factory)
	        throws HibernateException {
		PropertyMapping ownerMapping = ( PropertyMapping ) factory.getEntityPersister( entityName );
		Type type = ownerMapping.toType( propertyName );
		if ( !type.isCollectionType() ) {
			throw new MappingException(
			        "Property path [" + entityName + "." + propertyName + "] does not reference a collection"
			);
		}

		String role = ( ( CollectionType ) type ).getRole();
		try {
			return ( QueryableCollection ) factory.getCollectionPersister( role );
		}
		catch ( ClassCastException cce ) {
			throw new QueryException( "collection role is not queryable: " + role );
		}
		catch ( Exception e ) {
			throw new QueryException( "collection role not found: " + role );
		}
	}

	public final TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
	        throws HibernateException {
		return NO_VALUES;
	}

	public final String toString() {
		return propertyName + ( excludeEmpty() ? " is not empty" : " is empty" );
	}
}
