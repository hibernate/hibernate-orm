/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.SqlTreeException;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Adds a JDBC function escape (i.e. `{fn <wrapped-function-call>})  around the wrapped function
 *
 * @author Steve Ebersole
 */
public class SqmJdbcFunctionEscapeWrapper
		extends SelfRenderingSqmFunction
		implements SqmNonStandardFunction, SqlAstFunctionProducer, SelfRenderingFunctionSupport {
	private final SqmFunction wrappedSqmFunction;

	public SqmJdbcFunctionEscapeWrapper(
			SqmFunction wrappedSqmFunction,
			AllowableFunctionReturnType impliedResultType) {
		super( null, Collections.singletonList( wrappedSqmFunction ), impliedResultType );
		this.wrappedSqmFunction = wrappedSqmFunction;
	}

	public SqmJdbcFunctionEscapeWrapper(SqmFunction wrappedSqmFunction) {
		super( null, Collections.singletonList( wrappedSqmFunction ), wrappedSqmFunction.getExpressableType() );
		this.wrappedSqmFunction = wrappedSqmFunction;
	}

	@Override
	public String getFunctionName() {
		return wrappedSqmFunction.getFunctionName();
	}

	@Override
	public boolean hasArguments() {
		return wrappedSqmFunction.hasArguments();
	}

	@Override
	public SelfRenderingFunctionSupport getRenderingSupport() {
		return this;
	}

	@Override
	public String asLoggableText() {
		return "wrapped-function[ " + wrappedSqmFunction.asLoggableText() + " ]";
	}

	@Override
	@SuppressWarnings("unchecked")
	public void render(
			SqlAppender sqlAppender,
			List<Expression> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		// the SQL AST form of wrapped function ought to have exactly one argument,
		// 		which should be the SQL AST form of the wrapped function.  Validate
		// 		that that is the case...
		if ( sqlAstArguments.isEmpty() ) {
			// no SQL AST form of wrapped function, which ought to be the argument here
			throw new SqlTreeException( "JDBC function escape wrapper had no SQL AST form of its wrapped function as argument" );
		}
		if ( sqlAstArguments.size() > 1 ) {
			// no SQL AST form of wrapped function, which ought to be the argument here
			throw new SqlTreeException( "JDBC function escape wrapper had multiple arguments; should have just one - the wrapped function" );
		}

		sqlAppender.appendSql( "{fn " );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( "}" );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}
}
