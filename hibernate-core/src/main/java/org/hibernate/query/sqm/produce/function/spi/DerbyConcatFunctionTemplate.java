/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.tree.expression.ConcatSqmExpression;
import org.hibernate.query.sqm.tree.expression.ParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

import static org.hibernate.type.spi.StandardSpiBasicTypes.STRING;

/**
 * A specialized concat() function definition in which:<ol>
 *     <li>we translate to use the concat operator ('||')</li>
 *     <li>wrap dynamic parameters in CASTs to VARCHAR</li>
 * </ol>
 * <p/>
 * This last spec is to deal with a limitation on DB2 and variants (e.g. Derby)
 * where dynamic parameters cannot be used in concatenation unless they are being
 * concatenated with at least one non-dynamic operand.  And even then, the rules
 * are so convoluted as to what is allowed and when the CAST is needed and when
 * it is not that we just go ahead and do the CASTing.
 *
 * @author Steve Ebersole
 */
public class DerbyConcatFunctionTemplate implements SqmFunctionTemplate {

	@Override
	public SqmExpression makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		if ( arguments.size() < 2 ) {
			throw new org.hibernate.query.sqm.QueryException( "concat function must have 2 or more arguments" );
		}

		// check if all arguments are parameters...
		//		- if not, simply use the Derby concat operator - e.g. `arg1 || arg2`
		//		- if so, wrap the individual args in `cast` function and wrap the
		//				entire expression in Derby's `varchar` function (specialized
		// 				`cast` function)
		boolean areAllArgumentsDynamic = true;
		for ( SqmExpression argument : arguments ) {
			if ( ParameterSqmExpression.class.isInstance( argument ) ) {
				areAllArgumentsDynamic = false;
				break;
			}
		}

		if ( areAllArgumentsDynamic ) {
			return buildAllParameterExpression( arguments, impliedResultType );
		}
		else {
			return buildNotAllParameterExpression( arguments, impliedResultType );
		}
	}

	private SqmExpression buildAllParameterExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		// all args where parameters - wrap the individual args in `cast`
		// 		function and wrap the entire expression in Derby's `varchar`
		// 		function (specialized `cast` function)
		final Optional<SqmExpression> concat = arguments.stream().reduce(
				(argument1, argument2) -> concat(
						cast( argument1 ),
						cast( argument2 ),
						impliedResultType
				)
		);

		if ( !concat.isPresent() ) {
			throw new AssertionFailure( "Was unable to build Derby concat chain" );
		}

		return varchar( concat.get() );
	}

	private SqmExpression buildNotAllParameterExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		// simply use the Derby concat operator - e.g. `arg1 || arg2 (|| argX)?`
		final Optional<SqmExpression> concat = arguments.stream().reduce(
				(argument1, argument2) -> concat(
						cast( argument1 ),
						cast( argument2 ),
						impliedResultType
				)
		);

		if ( !concat.isPresent() ) {
			throw new AssertionFailure( "Was unable to build Derby concat chain" );
		}

		return concat.get();
	}

	private SqmExpression varchar(SqmExpression argument) {
		return new PatternBasedSqmFunctionTemplate( STRING, "varchar(?1)" )
				.makeSqmFunctionExpression( Collections.singletonList( argument ), null );
	}

	private ConcatSqmExpression concat(
			SqmExpression expression1,
			SqmExpression expression2,
			AllowableFunctionReturnType impliedResultType) {
		return new ConcatSqmExpression(
				expression1,
				expression2,
				(BasicValuedExpressableType) impliedResultType
		);
	}

	private SqmExpression cast(SqmExpression argument) {
		return new SqmCastFunction( argument, STRING, "varchar(32672)" );
	}
}
