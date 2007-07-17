//$Id: SizeExpression.java 7844 2005-08-11 07:26:26Z oneovthafew $
package org.hibernate.criterion;


import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.TypedValue;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.sql.ConditionFragment;

/**
 * @author Gavin King
 */
public class SizeExpression implements Criterion {
	
	private final String propertyName;
	private final int size;
	private final String op;
	
	protected SizeExpression(String propertyName, int size, String op) {
		this.propertyName = propertyName;
		this.size = size;
		this.op = op;
	}

	public String toString() {
		return propertyName + ".size" + op + size;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		String role = criteriaQuery.getEntityName(criteria, propertyName) + 
				'.' +  
				criteriaQuery.getPropertyName(propertyName);
		QueryableCollection cp = (QueryableCollection) criteriaQuery.getFactory()
				.getCollectionPersister(role);
		//String[] fk = StringHelper.qualify( "collection_", cp.getKeyColumnNames() );
		String[] fk = cp.getKeyColumnNames();
		String[] pk = ( (Loadable) cp.getOwnerEntityPersister() ).getIdentifierColumnNames(); //TODO: handle property-ref
		return "? " + 
				op + 
				" (select count(*) from " +
				cp.getTableName() +
				//" collection_ where " +
				" where " +
				new ConditionFragment()
						.setTableAlias( criteriaQuery.getSQLAlias(criteria, propertyName) )
						.setCondition(pk, fk)
						.toFragmentString() +
				")";
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return new TypedValue[] { 
			new TypedValue( Hibernate.INTEGER, new Integer(size), EntityMode.POJO ) 
		};
	}

}
