/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.tree.expression.Expression;

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
			SqmExpressible<T> type,
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
	public void appendHqlString(StringBuilder sb) {
		// Special case a few functions with special syntax for rendering...
		// Unless we introduce dedicated SqmXXX classes that override this method, we have to render it this way
		switch ( functionName ) {
			case StandardFunctions.CAST: {
				sb.append( "cast(" );
				arguments.get( 0 ).appendHqlString( sb );
				sb.append( " as " );
				arguments.get( 1 ).appendHqlString( sb );
				sb.append( ')' );
				break;
			}
			case StandardFunctions.EXTRACT: {
				sb.append( "extract(" );
				arguments.get( 0 ).appendHqlString( sb );
				sb.append( " from " );
				arguments.get( 1 ).appendHqlString( sb );
				sb.append( ')' );
				break;
			}
			case StandardFunctions.FORMAT: {
				sb.append( "format(" );
				arguments.get( 0 ).appendHqlString( sb );
				sb.append( " as " );
				arguments.get( 1 ).appendHqlString( sb );
				sb.append( ')' );
				break;
			}
			case StandardFunctions.OVERLAY: {
				sb.append( "overlay(" );
				arguments.get( 0 ).appendHqlString( sb );
				sb.append( " placing " );
				arguments.get( 1 ).appendHqlString( sb );
				sb.append( " from " );
				arguments.get( 2 ).appendHqlString( sb );
				if ( arguments.size() == 4 ) {
					sb.append( " for " );
					arguments.get( 3 ).appendHqlString( sb );
				}
				sb.append( ')' );
				break;
			}
			case StandardFunctions.TRIM: {
				sb.append( "trim(" );
				switch ( arguments.size() ) {
					case 1:
						arguments.get( 0 ).appendHqlString( sb );
						break;
					case 2:
						arguments.get( 0 ).appendHqlString( sb );
						sb.append( " from " );
						arguments.get( 1 ).appendHqlString( sb );
						break;
					case 3:
						arguments.get( 0 ).appendHqlString( sb );
						sb.append( ' ' );
						arguments.get( 1 ).appendHqlString( sb );
						sb.append( " from " );
						arguments.get( 3 ).appendHqlString( sb );
						break;
				}
				sb.append( ')' );
				break;
			}
			case StandardFunctions.PAD: {
				sb.append( "pad(" );
				arguments.get( 0 ).appendHqlString( sb );
				sb.append( " with" );
				for ( int i = 1; i < arguments.size(); i++ ) {
					sb.append( ' ' );
					arguments.get( i ).appendHqlString( sb );
				}
				sb.append( ')' );
				break;
			}
			case StandardFunctions.POSITION: {
				sb.append( "position(" );
				arguments.get( 0 ).appendHqlString( sb );
				sb.append( " in " );
				arguments.get( 1 ).appendHqlString( sb );
				sb.append( ')' );
				break;
			}
			default: {
				sb.append( functionName );
				if ( arguments.isEmpty() ) {
					if ( functionDescriptor.alwaysIncludesParentheses() ) {
						sb.append( "()" );
					}
					return;
				}
				sb.append( '(' );
				for ( int i = 1; i < arguments.size(); i++ ) {
					sb.append( ", " );
					arguments.get( i ).appendHqlString( sb );
				}

				sb.append( ')' );
				break;
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SemanticPathPart


	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException();
	}
}
