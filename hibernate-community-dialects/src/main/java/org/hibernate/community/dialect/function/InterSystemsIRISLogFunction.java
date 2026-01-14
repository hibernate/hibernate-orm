/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

public class InterSystemsIRISLogFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public InterSystemsIRISLogFunction(TypeConfiguration typeConfiguration) {
		super(
				"log",
				StandardArgumentsValidators.between(1, 2),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry()
								.resolve( StandardBasicTypes.DOUBLE)
				),
				null
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {

		if (arguments.size() == 1) {
			// LOG(x) → log(x)
			sqlAppender.appendSql("log(");
			arguments.get(0).accept(walker);
			sqlAppender.appendSql(")");
		}
		else if (arguments.size() == 2) {
			// LOG(base, value) → (log(value) / log(base))
			sqlAppender.appendSql("(log(");
			arguments.get(1).accept(walker);
			sqlAppender.appendSql(")/log(");
			arguments.get(0).accept(walker);
			sqlAppender.appendSql("))");
		}
	}
}
