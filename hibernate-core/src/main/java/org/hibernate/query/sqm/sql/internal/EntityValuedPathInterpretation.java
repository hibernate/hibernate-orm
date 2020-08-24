/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;

/**
 * @author Koen Aers
 */
public class EntityValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> {

	public static <T> EntityValuedPathInterpretation<T> from(
			SqmEntityValuedSimplePath<T> sqmPath,
			SqmToSqlAstConverter sqlAstCreationState) {
		final TableGroup tableGroup = sqlAstCreationState
				.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() );
		final EntityValuedModelPart mapping = (EntityValuedModelPart) tableGroup.getModelPart()
				.findSubPart( sqmPath.getReferencedPathSource().getPathName(), null );

		SqlTuple sqlExpression = resolveSqlExpression(
				sqmPath,
				sqlAstCreationState,
				tableGroup,
				mapping
		);
		return new EntityValuedPathInterpretation<>(
				sqlExpression,
				sqmPath,
				tableGroup,
				mapping
		);
	}

	private final Expression sqlExpression;

	private EntityValuedPathInterpretation(
			Expression sqlExpression,
			SqmEntityValuedSimplePath sqmPath,
			TableGroup tableGroup,
			EntityValuedModelPart mapping) {
		super( sqmPath, mapping, tableGroup );
		this.sqlExpression = sqlExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlExpression.accept( sqlTreeWalker );
	}

	private static <T> SqlTuple resolveSqlExpression(
			SqmEntityValuedSimplePath<T> sqmPath,
			SqmToSqlAstConverter sqlAstCreationState,
			TableGroup tableGroup,
			EntityValuedModelPart mapping) {
		final List<ColumnReference> columnReferences = new ArrayList<>();

		assert mapping instanceof ToOneAttributeMapping;

		final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) mapping;
		final ModelPart modelPart = getModelPart(
				sqlAstCreationState,
				toOneAttributeMapping
		);

		modelPart.visitColumns(
				(containingTableExpression, columnExpression, isFormula, customReadExpression, customWriteExpression, jdbcMapping) -> {
					final TableReference tf;
					final TableReference tableReference = getTableReference(
							sqmPath,
							sqlAstCreationState,
							tableGroup,
							toOneAttributeMapping,
							containingTableExpression
					);

					final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver()
							.resolveSqlExpression(
									SqlExpressionResolver.createColumnReferenceKey(
											tableReference,
											columnExpression
									),
									sqlAstProcessingState -> new ColumnReference(
											tableReference.getIdentificationVariable(),
											columnExpression,
											isFormula,
											customReadExpression,
											customWriteExpression,
											jdbcMapping,
											sqlAstCreationState.getCreationContext().getSessionFactory()
									)
							);

					columnReferences.add( (ColumnReference) columnReference );
				}
		);

		SqlTuple sqlExpression = new SqlTuple( columnReferences, mapping );
		return sqlExpression;
	}

	private static ModelPart getModelPart(
			SqmToSqlAstConverter sqlAstCreationState,
			ToOneAttributeMapping toOneAttributeMapping) {
		final Clause current = sqlAstCreationState.getCurrentClauseStack()
				.getCurrent();
		final ModelPart modelPart;
		if ( current == Clause.SELECT ) {
			modelPart = toOneAttributeMapping.getAssociatedEntityMappingType().getIdentifierMapping();
		}
		else {
			modelPart = toOneAttributeMapping.getForeignKeyDescriptor();
		}
		return modelPart;
	}

	private static <T> TableReference getTableReference(
			SqmEntityValuedSimplePath<T> sqmPath,
			SqlAstCreationState sqlAstCreationState,
			TableGroup tableGroup,
			ToOneAttributeMapping toOneAttributeMapping,
			String containingTableExpression) {
		TableReference tableReference = tableGroup.getTableReference( containingTableExpression );
		if ( tableReference == null ) {
			final TableGroupJoin tableGroupJoin = toOneAttributeMapping.createTableGroupJoin(
					sqmPath.getNavigablePath(),
					tableGroup,
					null,
					toOneAttributeMapping.isNullable() ? SqlAstJoinType.INNER : SqlAstJoinType.LEFT,
					LockMode.NONE,
					sqlAstCreationState
			);
			sqlAstCreationState.getFromClauseAccess().registerTableGroup(
					sqmPath.getNavigablePath(),
					tableGroupJoin.getJoinedGroup()
			);
			return tableGroupJoin.getJoinedGroup().getTableReference( containingTableExpression );
		}
		return tableReference;
	}
}
