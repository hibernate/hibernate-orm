/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;

/**
 * A table reference for a table valued function.
 *
 * @author Christian Beikov
 */
public class FunctionTableReference extends DerivedTableReference {

	private final FunctionExpression functionExpression;
	private final Set<String> compatibleTableExpressions;
	private final boolean rendersIdentifierVariable;

	public FunctionTableReference(
			FunctionExpression functionExpression,
			String identificationVariable,
			List<String> columnNames,
			boolean lateral,
			boolean rendersIdentifierVariable,
			Set<String> compatibleTableExpressions,
			SessionFactoryImplementor sessionFactory) {
		super( identificationVariable, columnNames, lateral, sessionFactory );
		this.functionExpression = functionExpression;
		this.compatibleTableExpressions = compatibleTableExpressions;
		this.rendersIdentifierVariable = rendersIdentifierVariable;
	}

	public FunctionExpression getFunctionExpression() {
		return functionExpression;
	}

	public Set<String> getCompatibleTableExpressions() {
		return compatibleTableExpressions;
	}

	public boolean rendersIdentifierVariable() {
		return rendersIdentifierVariable;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitFunctionTableReference( this );
	}

	@Override
	public Boolean visitAffectedTableNames(Function<String, Boolean> nameCollector) {
		return null;
	}

	@Override
	public boolean containsAffectedTableName(String requestedName) {
		return compatibleTableExpressions.contains( requestedName );
	}
}
