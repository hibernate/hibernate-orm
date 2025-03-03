/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * @author Gavin King
 */
public class Star implements Expression {
	/**
	 * Singleton access
	 */
	public static final Star INSTANCE = new Star();

	@Override
	public MappingModelExpressible getExpressionType() {
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitStar( this );
	}
}
