/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NavigablePath;

/**
 * @author Andrea Boriero
 */
public class UnionTableReference extends TableReference {
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
		throw new IllegalStateException( "Could not resolve binding for table `" + tableExpression + "`" );
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
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
