/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.NullPrecedence;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.sql.ConditionFragment;

import java.io.Serializable;

/**
 * Represents an ordering imposed upon the results of a Criteria based on the size of a collection property
 *
 * @author Peter Wright
 * @see org.hibernate.criterion.SizeExpression
 */
public class SizeOrder extends Order implements Serializable
{
	/**
	 * Ascending order
	 *
	 * @param propertyName
	 * 		The property to order on
	 *
	 * @return The build Order instance
	 */
	public static SizeOrder asc(String propertyName)
	{
		return new SizeOrder(propertyName, true);
	}


	/**
	 * Descending order.
	 *
	 * @param propertyName
	 * 		The property to order on
	 *
	 * @return The build Order instance
	 */
	public static SizeOrder desc(String propertyName)
	{
		return new SizeOrder(propertyName, false);
	}


	/**
	 * Constructor for Order.  Order instances are generally created by factory methods.
	 *
	 * @see #asc
	 * @see #desc
	 */
	protected SizeOrder(String propertyName, boolean ascending)
	{
		super(propertyName, ascending);
	}


	/**
	 * Render the SQL fragment
	 *
	 * @param criteria
	 * 		The criteria
	 * @param criteriaQuery
	 * 		The overall query
	 *
	 * @return The ORDER BY fragment for this ordering
	 *
	 * @see org.hibernate.criterion.SizeExpression#toSqlString(Criteria, CriteriaQuery)
	 */
	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	{
		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		final String propertyName = getPropertyName();

		// The following logic is from SizeExpression
		final String entityName = criteriaQuery.getEntityName(criteria, propertyName);
		final String role = entityName + '.' + criteriaQuery.getPropertyName(propertyName);
		final QueryableCollection cp = (QueryableCollection) criteriaQuery.getFactory().getCollectionPersister(role);

		final String[] fk = cp.getKeyColumnNames();
		final String[] pk = ((Loadable) cp.getOwnerEntityPersister()).getIdentifierColumnNames();

		final ConditionFragment subQueryRestriction = new ConditionFragment().setTableAlias(criteriaQuery.getSQLAlias(criteria,
		                                                                                                              propertyName))
		                                                                     .setCondition(pk, fk);

		final String expression = String.format("(select count(*) from %s where %s)",
		                                        cp.getTableName(),
		                                        subQueryRestriction.toFragmentString());

		// Render an order element based on the size SQL expression
		return factory.getDialect().renderOrderByElement(expression.toString(),
		                                                 null,
		                                                 isAscending() ? "asc" : "desc",
		                                                 NullPrecedence.NONE);
	}


	@Override
	public String toString()
	{
		return getPropertyName() +
		       ".size " +
		       (isAscending() ? "asc" : "desc");
	}
}
