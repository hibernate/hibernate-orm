package org.hibernate.sql.ast.spi.query.insert;

import org.hibernate.sql.ast.spi.query.expression.Expression;

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
