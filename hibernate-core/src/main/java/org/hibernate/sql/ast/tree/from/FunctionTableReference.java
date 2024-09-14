/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
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

	public FunctionTableReference(
			FunctionExpression functionExpression,
			String identificationVariable,
			List<String> columnNames,
			boolean lateral,
			SessionFactoryImplementor sessionFactory) {
		super( identificationVariable, columnNames, lateral, sessionFactory );
		this.functionExpression = functionExpression;
	}

	public FunctionExpression getFunctionExpression() {
		return functionExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		functionExpression.accept( sqlTreeWalker );
	}

	@Override
	public Boolean visitAffectedTableNames(Function<String, Boolean> nameCollector) {
		return null;
	}
}
