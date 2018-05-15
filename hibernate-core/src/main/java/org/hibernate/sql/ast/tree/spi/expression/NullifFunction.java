/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class NullifFunction extends AbstractFunction {
	private final Expression first;
	private final Expression second;
	private final SqlExpressableType type;

	public NullifFunction(
			Expression first,
			Expression second,
			SqlExpressableType type) {
		this.first = first;
		this.second = second;
		this.type = type;
	}

	public Expression getFirstArgument() {
		return first;
	}

	public Expression getSecondArgument() {
		return second;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlExpressableType getType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitNullifFunction( this );
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				getExpressableType()
		);
	}

}
