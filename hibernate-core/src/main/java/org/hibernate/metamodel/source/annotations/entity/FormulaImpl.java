package org.hibernate.metamodel.source.annotations.entity;

import org.hibernate.metamodel.source.binder.DerivedValueSource;

/**
 * @author Strong Liu
 */
public class FormulaImpl implements DerivedValueSource{
	private String tableName;
	private final String expression;

	FormulaImpl(String tableName, String expression) {
		this.tableName = tableName;
		this.expression = expression;
	}

	@Override
	public String getExpression() {
		return expression;
	}

	@Override
	public String getContainingTableName() {
		return tableName;
	}
}
