/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;

/**
 * Support for {@link SqmSetReturningFunctionDescriptor}s that ultimately want
 * to perform SQL rendering themselves. This is a protocol passed
 * from the {@link AbstractSqmSelfRenderingSetReturningFunctionDescriptor}
 * along to its {@link SelfRenderingSqmSetReturningFunction} and ultimately to
 * the {@link FunctionTableGroup} which calls it
 * to finally render SQL.
 *
 * @since 7.0
 */
@FunctionalInterface
public interface SetReturningFunctionRenderer {

	void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker);

	default boolean rendersIdentifierVariable(List<SqlAstNode> arguments, SessionFactoryImplementor sessionFactory) {
		return false;
	}

}
