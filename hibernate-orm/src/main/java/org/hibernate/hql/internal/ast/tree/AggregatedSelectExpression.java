/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.List;

import org.hibernate.transform.ResultTransformer;

/**
 * Contract for a select expression which aggregates other select expressions together into a single return
 *
 * @author Steve Ebersole
 */
public interface AggregatedSelectExpression extends SelectExpression {
	/**
	 * Retrieves a list of the selection {@link org.hibernate.type.Type types} being aggregated
	 *
	 * @return The list of types.
	 */
	public List getAggregatedSelectionTypeList();

	/**
	 * Retrieve the aliases for the columns aggregated here.
	 *
	 * @return The column aliases.
	 */
	public String[] getAggregatedAliases();

	/**
	 * Retrieve the {@link ResultTransformer} responsible for building aggregated select expression results into their
	 * aggregated form.
	 *
	 * @return The appropriate transformer
	 */
	public ResultTransformer getResultTransformer();

	/**
	 * Obtain the java type of the aggregation
	 *
	 * @return The java type.
	 */
	public Class getAggregationResultType();
}
