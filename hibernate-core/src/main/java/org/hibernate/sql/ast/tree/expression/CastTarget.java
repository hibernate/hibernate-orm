/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Gavin King
 */
public class CastTarget implements Expression, SqlAstNode {
	private BasicValuedMapping type;
	private Long length;
	private Integer precision;
	private Integer scale;

	public CastTarget(BasicValuedMapping type, Long length, Integer precision, Integer scale) {
		this.type = type;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
	}

	public Long getLength() {
		return length;
	}

	public Integer getPrecision() {
		return precision;
	}

	public Integer getScale() {
		return scale;
	}

	@Override
	public BasicValuedMapping getExpressionType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCastTarget(this);
	}
}
