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

/**
 * Represents a function used in an order-by fragment
 *
 * @author Steve Ebersole
 */
public class FunctionExpression implements SortExpression {
	private final String name;
	private final List<SortExpression> arguments;

	public FunctionExpression(String name, int numberOfArguments) {
		this.name = name;
		this.arguments = numberOfArguments == 0
				? Collections.emptyList()
				: new ArrayList<>( numberOfArguments );
	}

	public String getName() {
		return name;
	}

	public List<SortExpression> getArguments() {
		return arguments;
	}

	public void addArgument(SortExpression argument) {
		arguments.add( argument );
	}
}
