/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderingSupport;
import org.hibernate.query.sqm.function.SelfRenderingSqlFunctionExpression;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * DB2's varchar_format() can't handle quoted literal strings in
 * the format pattern. So just split the pattern into bits, call
 * varcharformat() on the odd-numbered bits, and concatenate all
 * the nonempty bits at the end.
 *
 * @author Gavin King
 */
public class DB2FormatEmulation
		extends AbstractSqmFunctionDescriptor implements FunctionRenderingSupport {

	public DB2FormatEmulation() {
		super(
				"format",
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.STRING )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstWalker walker) {
		Expression datetime = (Expression) arguments.get(0);
		Format format = (Format) arguments.get(1);

		sqlAppender.appendSql("(");
		String[] bits = OracleDialect.datetimeFormat( format.getFormat(), false ).result().split("\"");
		boolean first = true;
		for ( int i=0; i<bits.length; i++ ) {
			String bit = bits[i];
			if ( !bit.isEmpty() ) {
				if ( first ) {
					first = false;
				}
				else {
					sqlAppender.appendSql("||");
				}
				if ( i % 2 == 0 ) {
					sqlAppender.appendSql("varchar_format(");
					datetime.accept(walker);
					sqlAppender.appendSql(",'");
					sqlAppender.appendSql( bit );
					sqlAppender.appendSql("')");
				}
				else {
					sqlAppender.appendSql("'");
					sqlAppender.appendSql( bit );
					sqlAppender.appendSql("'");
				}
			}
		}
		if ( first ) {
			sqlAppender.appendSql("''");
		}
		sqlAppender.appendSql(")");
	}

	@Override
	protected <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return new SelfRenderingSqlFunctionExpression<T>(
				this, this,
				arguments,
				impliedResultType,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				"format"
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(datetime as pattern)";
	}
}
