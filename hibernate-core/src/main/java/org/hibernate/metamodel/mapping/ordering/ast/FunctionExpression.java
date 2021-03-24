/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.SortOrder;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * Represents a function used in an order-by fragment
 *
 * @author Steve Ebersole
 */
public class FunctionExpression implements OrderingExpression {
	private final String name;
	private final List<OrderingExpression> arguments;

	public FunctionExpression(String name, int numberOfArguments) {
		this.name = name;
		this.arguments = numberOfArguments == 0
				? Collections.emptyList()
				: new ArrayList<>( numberOfArguments );
	}

	public String getName() {
		return name;
	}

	public List<OrderingExpression> getArguments() {
		return arguments;
	}

	public void addArgument(OrderingExpression argument) {
		arguments.add( argument );
	}

	@Override
	public void apply(
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortOrder sortOrder,
			SqlAstCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
