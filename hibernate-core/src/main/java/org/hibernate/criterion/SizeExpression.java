/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2011, Red Hat Inc. or third-party contributors as
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
