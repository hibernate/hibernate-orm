/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;

/**
 * @author Christian Beikov
 */
public abstract class AbstractPredicate implements Predicate {
	private final JdbcMappingContainer expressionType;
	private final boolean negated;

	public AbstractPredicate(JdbcMappingContainer expressionType) {
		this( expressionType, false );
	}

	public AbstractPredicate(JdbcMappingContainer expressionType, boolean negated) {
		this.expressionType = expressionType;
		this.negated = negated;
	}

	public boolean isNegated() {
		return negated;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expressionType;
	}
}
