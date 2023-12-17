/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;
import java.util.Locale;

import org.hibernate.QueryException;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;

/**
 * A chr implementation that translates integer literals to string literals.
 *
 * @author Christian Beikov
 */
public class ChrLiteralEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	public ChrLiteralEmulation(TypeConfiguration typeConfiguration) {
		super(
				"chr",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.composite(
								StandardArgumentsValidators.exactly(1),
								new ArgumentsValidator() {
									@Override
									public void validate(List<? extends SqmTypedNode<?>> arguments, String functionName, TypeConfiguration typeConfiguration) {
										if ( !( arguments.get( 0 ) instanceof SqmLiteral<?> ) ) {
											throw new QueryException(
													String.format(
															Locale.ROOT,
															"Emulation of function chr() supports only integer literals, but %s argument given",
															arguments.get( 0 ).getClass().getName()
													)
											);
										}
									}
								}
						),
						INTEGER
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.CHARACTER )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, INTEGER )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		@SuppressWarnings("unchecked")
		final QueryLiteral<Number> literal = (QueryLiteral<Number>) arguments.get( 0 );
		sqlAppender.appendSql( '\'' );
		sqlAppender.appendSql( (char) literal.getLiteralValue().intValue() );
		sqlAppender.appendSql( '\'' );
	}

}
