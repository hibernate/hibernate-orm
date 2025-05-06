/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.type.JavaObjectType;

/**
 * A function to pass through a SQL fragment.
 *
 * @author Christian Beikov
 */
public class SqlFunction
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	public SqlFunction() {
		super(
				"sql",
				StandardArgumentsValidators.min( 1 ),
				StandardFunctionReturnTypeResolvers.invariant( JavaObjectType.INSTANCE ),
				null
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final QueryLiteral<String> sqlFragmentLiteral = (QueryLiteral<String>) arguments.get( 0 );
		final String sqlFragment = sqlFragmentLiteral.getLiteralValue();
		if ( arguments.size() != 1 ) {
			int index = 0;
			for ( int i = 1; i < arguments.size(); i++ ) {
				final SqlAstNode argument = arguments.get( i );
				final int paramIndex = sqlFragment.indexOf( '?', index );
				if ( paramIndex == -1 ) {
					throw new IllegalArgumentException( "The SQL function passes an argument at index " + i
							+ " but the fragment contains no placeholder for the argument: " + sqlFragment );
				}
				sqlAppender.append( sqlFragment, index, paramIndex );
				argument.accept( walker );
				index = paramIndex + 1;
			}
			sqlAppender.append( sqlFragment, index, sqlFragment.length() );
		}
		else {
			sqlAppender.appendSql( sqlFragment );
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "";
	}
}
