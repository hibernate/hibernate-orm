/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * @author Christian Beikov
 */
public class Summarization implements Expression {

	private final Kind kind;
	private final List<Expression> groupings;

	public Summarization(Kind kind, List<Expression> groupings) {
		this.kind = kind;
		this.groupings = groupings;
	}

	public Kind getKind() {
		return kind;
	}

	public List<Expression> getGroupings() {
		return groupings;
	}

	@Override
	public MappingModelExpressible getExpressionType() {
		return null;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitSummarization( this );
	}

	public enum Kind {
		ROLLUP( "rollup" ),
		CUBE( "cube" );

		private final String sqlText;

		Kind(String sqlText) {
			this.sqlText = sqlText;
		}

		public String sqlText() {
			return sqlText;
		}
	}
}
