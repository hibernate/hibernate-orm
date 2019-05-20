/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;

import java.util.List;

import static org.hibernate.query.TemporalUnit.MILLISECOND;

/**
 * MySQL timestampadd() does not support 'millisecond' as an argument.
 *
 * @author Gavin King
 */
public class MySQLTimestampaddEmulation
		extends AbstractSqmFunctionTemplate implements SelfRenderingFunctionSupport {

	public MySQLTimestampaddEmulation() {
		super(
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.useArgType( 3 )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstWalker walker) {
		ExtractUnit field = (ExtractUnit) arguments.get(0);
		Expression magnitude = (Expression) arguments.get(1);
		Expression datetime = (Expression) arguments.get(2);
		TemporalUnit unit = field.getUnit();
		sqlAppender.appendSql("timestampadd(");
		if ( MILLISECOND == unit ) {
			sqlAppender.appendSql("microsecond,1e3*(");

		}
		else {
			sqlAppender.appendSql( unit.toString() );
			sqlAppender.appendSql(",");
		}
		magnitude.accept(walker);
		if ( MILLISECOND == unit ) {
			sqlAppender.appendSql(")");
		}
		sqlAppender.appendSql(",");
		datetime.accept(walker);
		sqlAppender.appendSql(")");
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new SelfRenderingSqmFunction<T>(
				this,
				arguments,
				impliedResultType,
				queryEngine.getCriteriaBuilder(),
				"timestampdiff"
		);
	}

}
