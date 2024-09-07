/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * A {@link TemporalUnit} passed as an argument to the
 * {@link org.hibernate.dialect.function.TimestampaddFunction}
 * or {@link org.hibernate.dialect.function.TimestampdiffFunction}.
 * These are different to {@link ExtractUnit}s because of
 * how the {@link TemporalUnit#WEEK} field is handled on
 * some platforms.
 *
 * @author Gavin King
 */
public class DurationUnit implements Expression, SqlAstNode {
	private final TemporalUnit unit;
	private final BasicValuedMapping type;

	public DurationUnit(TemporalUnit unit, BasicValuedMapping type) {
		this.unit = unit;
		this.type = type;
	}

	public TemporalUnit getUnit() {
		return unit;
	}

	@Override
	public BasicValuedMapping getExpressionType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitDurationUnit(this);
	}
}

