/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Locale;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spi.NavigablePath;

/**
 * @author Andrea Boriero
 */
public class UnionTableReference extends NamedTableReference {
	private final String[] subclassTableSpaceExpressions;

	public UnionTableReference(
			String unionTableExpression,
			String[] subclassTableSpaceExpressions,
			String identificationVariable,
			boolean isOptional,
			SessionFactoryImplementor sessionFactory) {
		super( unionTableExpression, identificationVariable, isOptional, sessionFactory );

		this.subclassTableSpaceExpressions = subclassTableSpaceExpressions;
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
		if ( hasTableExpression( tableExpression ) ) {
			return this;
		}

		throw new UnknownTableReferenceException(
				tableExpression,
				String.format(
						Locale.ROOT,
						"Unable to determine TableReference (`%s`) for `%s`",
						tableExpression,
						navigablePath.getFullPath()
				)
		);
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve) {
		if ( hasTableExpression( tableExpression ) ) {
			return this;
		}
		return null;
	}

	private boolean hasTableExpression(String tableExpression) {
		if ( tableExpression.equals( getTableExpression() ) ) {
			return true;
		}
		for ( String expression : subclassTableSpaceExpressions ) {
			if ( tableExpression.equals( expression ) ) {
				return true;
			}
		}
		return false;
	}
}
