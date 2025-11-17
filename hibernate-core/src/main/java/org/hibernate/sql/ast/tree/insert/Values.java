/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.insert;

import org.hibernate.sql.ast.tree.expression.Expression;

import java.util.List;

public class Values {
	private final List<Expression> expressions;

	public Values(List<Expression> expressions) {
		this.expressions = expressions;
	}

	public List<Expression> getExpressions() {
		return expressions;
	}
}
