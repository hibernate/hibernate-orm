/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;

/**
 * Models a Bindable's inclusion in the {@code FROM} clause.
 *
 * @author Steve Ebersole
 */
public interface SqmFrom extends TableGroupInfo, SqmVisitableNode, SqmTypedNode {
	/**
	 * Obtain reference to the FromElementSpace that this FromElement belongs to.
	 */
	SqmFromElementSpace getContainingSpace();

	/**
	 * The Navigable reference (SqmExpression) that can be used to represent this
	 * from-element in other clauses.
	 *
	 * E.g. in a query like `select p from Person p` this SqmNavigableReference
	 * would represent the `p` reference in the SELECT clause.
	 */
	SqmNavigableReference getNavigableReference();

	@Override
	default NavigablePath getNavigablePath() {
		return getNavigableReference().getNavigablePath();
	}

	/**
	 * Details about how this SqmFrom is used in the query.
	 */
	UsageDetails getUsageDetails();

	default TableGroup locateMapping(FromClauseIndex fromClauseIndex) {
		// todo (6.0) : re-look at FromClauseIndex and what  it exposes (to avoid recursions here)
		return fromClauseIndex.resolveTableGroup( getUniqueIdentifier() );
	}
}
