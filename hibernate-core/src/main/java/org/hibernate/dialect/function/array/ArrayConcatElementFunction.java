/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Concatenation function for array and an element.
 */
public class ArrayConcatElementFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final String prefix;
	private final String separator;
	private final String suffix;
	protected final boolean prepend;

	public ArrayConcatElementFunction(String prefix, String separator, String suffix, boolean prepend) {
		super(
				"array_" + ( prepend ? "prepend" : "append" ),
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 2 ),
						prepend ? new ArrayAndElementArgumentValidator( 1, 0 )
								: ArrayAndElementArgumentValidator.DEFAULT_INSTANCE
				),
				prepend ? new ArrayViaArgumentReturnTypeResolver( 1 )
						: ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				prepend ? new ArrayAndElementArgumentTypeResolver( 1, 0 )
						: ArrayAndElementArgumentTypeResolver.DEFAULT_INSTANCE
		);
		this.prefix = prefix;
		this.separator = separator;
		this.suffix = suffix;
		this.prepend = prepend;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final SqlAstNode firstArgument = sqlAstArguments.get( 0 );
		final SqlAstNode secondArgument = sqlAstArguments.get( 1 );
		sqlAppender.append( prefix );
		if ( prepend ) {
			sqlAppender.append( "array[" );
			firstArgument.accept( walker );
			sqlAppender.append( ']' );
		}
		else {
			firstArgument.accept( walker );
		}
		sqlAppender.append( separator );
		if ( prepend ) {
			secondArgument.accept( walker );
		}
		else {
			sqlAppender.append( "array[" );
			secondArgument.accept( walker );
			sqlAppender.append( ']' );
		}
		sqlAppender.append( suffix );
	}
}
