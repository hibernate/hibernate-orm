/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.type.StandardBasicTypes;

/**
 * Used to define a restriction on a collection property based on its size.
 *
 * @author Gavin King
 * @author Steve Ebersole
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

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String entityName =criteriaQuery.getEntityName( criteria, propertyName );
		final String role = entityName + '.' + criteriaQuery.getPropertyName( propertyName );
		final QueryableCollection cp = (QueryableCollection) criteriaQuery.getFactory().getCollectionPersister( role );

		final String[] fk = cp.getKeyColumnNames();
		final String[] pk = ( (Loadable) cp.getOwnerEntityPersister() ).getIdentifierColumnNames();

		final ConditionFragment subQueryRestriction = new ConditionFragment()
				.setTableAlias( criteriaQuery.getSQLAlias( criteria, propertyName ) )
				.setCondition( pk, fk );

		return String.format(
				"? %s (select count(*) from %s where %s)",
				op,
				cp.getTableName(),
				subQueryRestriction.toFragmentString()
		);
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new TypedValue[] { new TypedValue( StandardBasicTypes.INTEGER, size ) };
	}

	@Override
	public String toString() {
		return propertyName + ".size" + op + size;
	}

}
