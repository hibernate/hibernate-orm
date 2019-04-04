/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromClauseContainer;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClauseContainer;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Defines the commonality between a root query and a subquery.
 *
 * @author Steve Ebersole
 */
public class SqmQuerySpec implements SqmFromClauseContainer, SqmWhereClauseContainer {
	private SqmFromClause fromClause;
	private SqmSelectClause selectClause;
	private SqmWhereClause whereClause;

	private SqmGroupByClause groupByClause;
	private SqmHavingClause havingClause;

	private SqmOrderByClause orderByClause;

	private SqmExpression limitExpression;
	private SqmExpression offsetExpression;

	public SqmQuerySpec() {
	}

	@Override
	public SqmFromClause getFromClause() {
		return fromClause;
	}

	public void setFromClause(SqmFromClause fromClause) {
		this.fromClause = fromClause;
	}

	public SqmSelectClause getSelectClause() {
		return selectClause;
	}

	public void setSelectClause(SqmSelectClause selectClause) {
		this.selectClause = selectClause;
	}

	@Override
	public SqmWhereClause getWhereClause() {
		return whereClause;
	}

	public void setWhereClause(SqmWhereClause whereClause) {
		this.whereClause = whereClause;
	}

	public SqmGroupByClause getGroupByClause() {
		return groupByClause;
	}

	public void setGroupByClause(SqmGroupByClause groupByClause) {
		this.groupByClause = groupByClause;
	}

	public SqmHavingClause getHavingClause() {
		return havingClause;
	}

	public void setHavingClause(SqmHavingClause havingClause) {
		this.havingClause = havingClause;
	}

	public SqmOrderByClause getOrderByClause() {
		return orderByClause;
	}

	public void setOrderByClause(SqmOrderByClause orderByClause) {
		this.orderByClause = orderByClause;
	}

	public SqmExpression getLimitExpression() {
		return limitExpression;
	}

	public void setLimitExpression(SqmExpression limitExpression) {
		if ( limitExpression != null ) {
			limitExpression.applyInferableType( StandardSpiBasicTypes.INTEGER );
		}
		this.limitExpression = limitExpression;
	}

	public SqmExpression getOffsetExpression() {
		return offsetExpression;
	}

	public void setOffsetExpression(SqmExpression offsetExpression) {
		if ( offsetExpression != null ) {
			offsetExpression.applyInferableType( StandardSpiBasicTypes.INTEGER );
		}
		this.offsetExpression = offsetExpression;
	}
}
