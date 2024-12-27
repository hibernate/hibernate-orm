/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.BOOLEAN;

/**
 * SQL Server doesn't have a function like {@code every()} or {@code any()}.
 * We emulate the function using {@code min()} or {@code max()} together with
 * {@code iif()}.
 *
 * @see EveryAnyEmulation
 *
 * @author Jan Schatteman
 */
public class SQLServerEveryAnyEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean every;

	public SQLServerEveryAnyEmulation(TypeConfiguration typeConfiguration, boolean every) {
		super(
				every ? "every" : "any",
				FunctionKind.AGGREGATE,
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 1 ), BOOLEAN ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, FunctionParameterType.BOOLEAN )
		);
		this.every = every;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( every ) {
			sqlAppender.appendSql( "min(iif(" );
		}
		else {
			sqlAppender.appendSql( "max(iif(" );
		}
		if ( filter != null ) {
			walker.getCurrentClauseStack().push( Clause.WHERE );
			filter.accept( walker );
			walker.getCurrentClauseStack().pop();
			sqlAppender.appendSql( ",iif(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( ",1,0),null))" );
		}
		else {
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( ",1,0))" );
		}
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		this.render( sqlAppender, sqlAstArguments, null, (ReturnableType<?>) null, walker );
	}
}
