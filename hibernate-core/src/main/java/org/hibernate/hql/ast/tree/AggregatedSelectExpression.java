/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.hql.ast.tree;

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
}
