/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.type.BasicType;
import org.hibernate.type.JavaObjectType;

import java.util.List;

/**
 * @author Gavin King
 */
public class SqlColumn extends AbstractSqmSelfRenderingFunctionDescriptor  {
	private final String columnName;

	public SqlColumn(String columnName, BasicType<?> type) {
		super(
				"column",
				StandardArgumentsValidators.min( 1 ),
				StandardFunctionReturnTypeResolvers.invariant( type == null ? JavaObjectType.INSTANCE : type ),
				null
		);
		this.columnName = columnName;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final SqlAstNode sqlAstNode = arguments.get(0);
		final ColumnReference reference;
		if ( sqlAstNode instanceof Assignable assignable ) {
			reference = assignable.getColumnReferences().get(0);
		}
		else if ( sqlAstNode instanceof Expression expression ) {
			reference = expression.getColumnReference();
		}
		else {
			throw new HqlInterpretationException( "path did not map to a column" );
		}
		sqlAppender.appendSql( reference.getQualifier() );
		sqlAppender.appendSql( '.' );
		sqlAppender.appendSql( columnName );
	}

}
