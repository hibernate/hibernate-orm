/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.TrimSpec;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * A {@link SqmFunctionTemplate} implementation that emulates
 * the ANSI SQL {@code trim()} function on dialects which do
 * not support the full definition.  However, this function
 * definition does assume the availability of {@code ltrim()},
 * {@code rtrim()}, and {@code replace()} functions, which it
 * uses in various combinations to emulate the desired ANSI
 * functionality.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class LtrimRtrimReplaceTrimEmulation extends AbstractSqmFunctionTemplate {

	private final String ltrimFunctionName;
	private final String rtrimFunctionName;
	private final String replaceFunctionName;

	/**
	 * A new instance using {@code ltrim}, {@code rtrim},
	 * and {@code replace} as the native SQL function
	 * names.
	 *
	 * @see #LtrimRtrimReplaceTrimEmulation(String,String,String)
	 */
	public LtrimRtrimReplaceTrimEmulation() {
		this("replace");
	}

	/**
	 * A new instance using {@code ltrim}, {@code rtrim},
	 * and the given function name as the native SQL
	 * function names.
	 *
	 * @param replaceFunctionName The {@code replace()} function to use.
	 * @see #LtrimRtrimReplaceTrimEmulation(String,String,String)
	 */
	public LtrimRtrimReplaceTrimEmulation(String replaceFunctionName) {
		this("ltrim", "rtrim", replaceFunctionName );
	}

	/**
	 * A new instance using the given SQL function names.
	 *
	 * @param ltrimFunctionName The {@code ltrim()} function to use.
	 * @param rtrimFunctionName The {@code rtrim()} function to use.
	 * @param replaceFunctionName The {@code replace()} function to use.
	 */
	public LtrimRtrimReplaceTrimEmulation(
			String ltrimFunctionName,
			String rtrimFunctionName,
			String replaceFunctionName) {
		super(
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.STRING )
		);
		this.ltrimFunctionName = ltrimFunctionName;
		this.rtrimFunctionName = rtrimFunctionName;
		this.replaceFunctionName = replaceFunctionName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SelfRenderingSqmFunction<T>  generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine) {
		final TrimSpec specification = ( (SqmTrimSpecification) arguments.get( 0 ) ).getSpecification();
		final char trimCharacter = ( (SqmLiteral<Character>) arguments.get( 1 ) ).getLiteralValue();
		final SqmExpression sourceExpr = (SqmExpression) arguments.get( 2 );

		return queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "trim", trimPattern(specification, trimCharacter) )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.template() //TODO: we could cache the 6 variations here
				.makeSqmFunctionExpression(
						Collections.singletonList( sourceExpr ),
						impliedResultType,
						queryEngine
				);
	}

	private String trimPattern(TrimSpec specification, char trimCharacter) {
		if ( trimCharacter == ' ' ) {
			switch ( specification ) {
				case LEADING:
					return oneSidedTrim(ltrimFunctionName);
				case TRAILING:
					return oneSidedTrim(rtrimFunctionName);
				default:
					return bothSidedTrim(ltrimFunctionName, rtrimFunctionName);
			}
		}
		else {
			switch (specification) {
				case LEADING:
					return oneSidedTrim(replaceFunctionName, ltrimFunctionName, trimCharacter);
				case TRAILING:
					return oneSidedTrim(replaceFunctionName, rtrimFunctionName, trimCharacter);
				default:
					return bothSidedTrim(replaceFunctionName, ltrimFunctionName, rtrimFunctionName, trimCharacter);
			}
		}
	}

	private static String oneSidedTrim(String trim) {
		return "trim($1)"
				.replace("trim", trim);
	}

	private static String bothSidedTrim(String ltrim, String rtrim) {
		return "ltrim(rtrim($1))"
				.replace("ltrim", ltrim)
				.replace("rtrim", rtrim);
	}

	private static String oneSidedTrim(String replace, String trim, char trimCharacter) {
		return "replace(replace(trim(replace(replace($1,' ','#%#%'),'char',' ')),' ','char'),'#%#%',' ')"
				.replace("replace", replace)
				.replace("trim", trim)
				.replace("char", String.valueOf(trimCharacter));
	}

	private static String bothSidedTrim(String replace, String ltrim, String rtrim, char trimCharacter) {
		return "replace(replace(ltrim(rtrim(replace(replace($1,' ','#%#%'),'char',' '))),' ','char'),'#%#%',' ')"
				.replace("replace", replace)
				.replace("ltrim", ltrim)
				.replace("rtrim", rtrim)
				.replace("char", String.valueOf(trimCharacter));
	}

	@Override
	public String getArgumentListSignature() {
		return "([[{leading|trailing|both} ][arg0 ]from] arg1)";
	}
}
