/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmFunction;

import java.util.List;

public abstract class AbstractFunctionArgumentTypeResolver implements FunctionArgumentTypeResolver {
	@Override
	@SuppressWarnings("removal")
	public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(SqmFunction<?> function, int argumentIndex, SqmToSqlAstConverter converter) {
		return resolveFunctionArgumentType( function.getArguments(), argumentIndex, converter );
	}

	@Override
	public abstract @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter);
}
