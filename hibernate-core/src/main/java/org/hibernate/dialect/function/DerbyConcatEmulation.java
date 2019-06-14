/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.spi.AbstractSelfRenderingFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.GenericParameter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import java.util.List;

/**
 * Casts query parameters using the Derby varchar() function
 * before concatenating them using the || operator.
 *
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DerbyConcatEmulation
		extends AbstractSelfRenderingFunctionTemplate
		implements SelfRenderingFunctionSupport {

	public DerbyConcatEmulation() {
		super(
				"concat",
				StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.STRING ),
				StandardArgumentsValidators.min( 1 )
		);
	}

	@Override
	protected SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<?> resolvedReturnType,
			QueryEngine queryEngine) {
		return this;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstWalker walker) {
		int numberOfArguments = arguments.size();
		if ( numberOfArguments > 1 ) {
			sqlAppender.appendSql("(");
		}
		for ( int i = 0; i < numberOfArguments; i++ ) {
			SqlAstNode argument = arguments.get( i );
			if ( i > 0 ) {
				sqlAppender.appendSql("||");
			}
			boolean param = argument instanceof GenericParameter;
			if ( param ) {
				sqlAppender.appendSql("cast(");
			}
			argument.accept(walker);
			if ( param ) {
				sqlAppender.appendSql(" as long varchar)");
			}
		}
		if ( numberOfArguments > 1 ) {
			sqlAppender.appendSql(")");
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(string0[, string1[, ...]])";
	}
}
