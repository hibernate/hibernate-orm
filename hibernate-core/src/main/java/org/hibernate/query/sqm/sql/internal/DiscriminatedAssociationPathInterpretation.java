/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;

/**
 * SqmPathInterpretation for discriminated association (ANY) mappings
 */
public class DiscriminatedAssociationPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements SqlTupleContainer, Assignable {

	public static <T> DiscriminatedAssociationPathInterpretation<T> from(
			SqmAnyValuedSimplePath<T> sqmPath,
			SqmToSqlAstConverter converter) {
		final TableGroup tableGroup =
				converter.getFromClauseAccess()
						.findTableGroup( sqmPath.getLhs().getNavigablePath() );
		final var mapping =
				(DiscriminatedAssociationModelPart)
						tableGroup.getModelPart().findSubPart(
								sqmPath.getReferencedPathSource().getPathName(),
								null
						);
		return new DiscriminatedAssociationPathInterpretation<>(
				sqmPath.getNavigablePath(),
				mapping,
				tableGroup,
				createSqlTuple( sqmPath.getNavigablePath(), mapping, tableGroup, converter )
		);
	}

	public static <T> DiscriminatedAssociationPathInterpretation<T> from(
			NavigablePath navigablePath,
			DiscriminatedAssociationModelPart mapping,
			TableGroup tableGroup,
			SqmToSqlAstConverter converter) {
		return new DiscriminatedAssociationPathInterpretation<>(
				navigablePath,
				mapping,
				tableGroup,
				createSqlTuple( navigablePath, mapping, tableGroup, converter )
		);
	}

	private static SqlTuple createSqlTuple(
			NavigablePath navigablePath,
			DiscriminatedAssociationModelPart mapping,
			TableGroup tableGroup,
			SqmToSqlAstConverter converter) {
		final List<Expression> tupleExpressions = new ArrayList<>();
		mapping.forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					final TableReference tableReference =
							tableGroup.resolveTableReference( navigablePath,
									selectableMapping.getContainingTableExpression() );
					tupleExpressions.add( converter.getSqlExpressionResolver()
							.resolveSqlExpression( tableReference, selectableMapping ) );
				}
		);
		return new SqlTuple( tupleExpressions, mapping );
	}


	private final SqlTuple sqlTuple;

	private DiscriminatedAssociationPathInterpretation(
			NavigablePath navigablePath,
			ModelPart mapping,
			TableGroup tableGroup,
			SqlTuple sqlTuple) {
		super( navigablePath, mapping, tableGroup );
		this.sqlTuple = sqlTuple;
	}

	@Override
	public SqlTuple getSqlExpression() {
		return sqlTuple;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTuple.accept( sqlTreeWalker );
	}

	@Override
	public SqlTuple getSqlTuple() {
		return sqlTuple;
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		final List<ColumnReference> results = new ArrayList<>();
		visitColumnReferences( results::add );
		return results;
	}

	@Override
	public void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		for ( Expression expression : sqlTuple.getExpressions() ) {
			if ( !( expression instanceof ColumnReference ) ) {
				throw new IllegalArgumentException( "Expecting ColumnReference, found : " + expression );
			}
			columnReferenceConsumer.accept( (ColumnReference) expression );
		}
	}
}
