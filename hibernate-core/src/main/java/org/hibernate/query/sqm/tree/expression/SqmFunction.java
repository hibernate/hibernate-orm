/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.List;

import org.hibernate.query.criteria.JpaFunction;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmFunctionPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.tree.expression.Expression;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A SQM function
 *
 * @author Steve Ebersole
 */
public abstract class SqmFunction<T> extends AbstractSqmExpression<T>
		implements JpaFunction<T>, SemanticPathPart {
	// this function-name is the one used to resolve the descriptor from
	// the function registry (which may or may not be a db function name)
	private final String functionName;
	private final SqmFunctionDescriptor functionDescriptor;

	private final List<? extends SqmTypedNode<?>> arguments;

	public SqmFunction(
			String functionName,
			SqmFunctionDescriptor functionDescriptor,
			@Nullable SqmExpressible<T> type,
			List<? extends SqmTypedNode<?>> arguments,
			NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.functionName = functionName;
		this.functionDescriptor = functionDescriptor;
		this.arguments = arguments;
	}

	public SqmFunctionDescriptor getFunctionDescriptor() {
		return functionDescriptor;
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}

	public List<? extends SqmTypedNode<?>> getArguments() {
		return arguments;
	}

	public abstract Expression convertToSqlAst(SqmToSqlAstConverter walker);

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFunction( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		// Special case a few functions with special syntax for rendering...
		// Unless we introduce dedicated SqmXXX classes that override this method, we have to render it this way
		switch ( functionName ) {
			case "cast": {
				hql.append( "cast(" );
				arguments.get( 0 ).appendHqlString( hql );
				hql.append( " as " );
				arguments.get( 1 ).appendHqlString( hql );
				hql.append( ')' );
				break;
			}
			case "extract": {
				hql.append( "extract(" );
				arguments.get( 0 ).appendHqlString( hql );
				hql.append( " from " );
				arguments.get( 1 ).appendHqlString( hql );
				hql.append( ')' );
				break;
			}
			case "format": {
				hql.append( "format(" );
				arguments.get( 0 ).appendHqlString( hql );
				hql.append( " as " );
				arguments.get( 1 ).appendHqlString( hql );
				hql.append( ')' );
				break;
			}
			case "overlay": {
				hql.append( "overlay(" );
				arguments.get( 0 ).appendHqlString( hql );
				hql.append( " placing " );
				arguments.get( 1 ).appendHqlString( hql );
				hql.append( " from " );
				arguments.get( 2 ).appendHqlString( hql );
				if ( arguments.size() == 4 ) {
					hql.append( " for " );
					arguments.get( 3 ).appendHqlString( hql );
				}
				hql.append( ')' );
				break;
			}
			case "trim": {
				hql.append( "trim(" );
				switch ( arguments.size() ) {
					case 1:
						arguments.get( 0 ).appendHqlString( hql );
						break;
					case 2:
						arguments.get( 0 ).appendHqlString( hql );
						hql.append( " from " );
						arguments.get( 1 ).appendHqlString( hql );
						break;
					case 3:
						arguments.get( 0 ).appendHqlString( hql );
						hql.append( ' ' );
						arguments.get( 1 ).appendHqlString( hql );
						hql.append( " from " );
						arguments.get( 3 ).appendHqlString( hql );
						break;
				}
				hql.append( ')' );
				break;
			}
			case "pad": {
				hql.append( "pad(" );
				arguments.get( 0 ).appendHqlString( hql );
				hql.append( " with" );
				for ( int i = 1; i < arguments.size(); i++ ) {
					hql.append( ' ' );
					arguments.get( i ).appendHqlString( hql );
				}
				hql.append( ')' );
				break;
			}
			case "position": {
				hql.append( "position(" );
				arguments.get( 0 ).appendHqlString( hql );
				hql.append( " in " );
				arguments.get( 1 ).appendHqlString( hql );
				hql.append( ')' );
				break;
			}
			default: {
				hql.append( functionName );
				if ( arguments.isEmpty() ) {
					if ( functionDescriptor.alwaysIncludesParentheses() ) {
						hql.append( "()" );
					}
					return;
				}
				hql.append( '(' );
				arguments.get( 0 ).appendHqlString( hql );
				for ( int i = 1; i < arguments.size(); i++ ) {
					hql.append( ", " );
					arguments.get( i ).appendHqlString( hql );
				}

				hql.append( ')' );
				break;
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SemanticPathPart

	private SqmFunctionPath<T> functionPath;

	private SqmFunctionPath<T> getFunctionPath() {
		SqmFunctionPath<T> path = functionPath;
		if ( path == null ) {
			path = functionPath = new SqmFunctionPath<T>( this );
		}
		return path;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		return getFunctionPath().resolvePathPart( name, isTerminal, creationState );
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		return getFunctionPath().resolveIndexedAccess( selector, isTerminal, creationState );
	}
}
