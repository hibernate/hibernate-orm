/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping.ordering;

import java.util.List;

import org.hibernate.metamodel.mapping.ordering.ast.OrderingSpecification;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * @author Steve Ebersole
 */
public class OrderByFragmentImpl implements OrderByFragment {
	private final List<OrderingSpecification> fragmentSpecs;

	public OrderByFragmentImpl(List<OrderingSpecification> fragmentSpecs) {
		this.fragmentSpecs = fragmentSpecs;
	}

	public List<OrderingSpecification> getFragmentSpecs() {
		return fragmentSpecs;
	}

	@Override
	public void apply(QuerySpec ast, TableGroup tableGroup, SqlAstCreationState creationState) {
		for ( int i = 0; i < fragmentSpecs.size(); i++ ) {
			final OrderingSpecification orderingSpec = fragmentSpecs.get( i );

			orderingSpec.getExpression().apply(
					ast,
					tableGroup,
					orderingSpec.getCollation(),
					orderingSpec.getOrderByValue(),
					orderingSpec.getSortOrder(),
					orderingSpec.getNullPrecedence(),
					creationState
			);
		}
	}
}
