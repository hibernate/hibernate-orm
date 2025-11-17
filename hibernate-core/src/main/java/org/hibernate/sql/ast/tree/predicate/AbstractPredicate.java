/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
