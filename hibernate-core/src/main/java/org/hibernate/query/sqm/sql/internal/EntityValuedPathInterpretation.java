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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.internal.SimpleForeignKeyDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Koen Aers
 */
public class EntityValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements SqlTupleContainer {

	public static <T> EntityValuedPathInterpretation<T> from(
			SqmEntityValuedSimplePath<T> sqmPath,
			SqmToSqlAstConverter sqlAstCreationState) {
		final SqlExpressionResolver sqlExprResolver = sqlAstCreationState.getSqlExpressionResolver();
		final SessionFactoryImplementor sessionFactory = sqlAstCreationState.getCreationContext().getSessionFactory();

		final TableGroup tableGroup = sqlAstCreationState
				.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() );

		final EntityValuedModelPart mapping = (EntityValuedModelPart) tableGroup.getModelPart()
				.findSubPart( sqmPath.getReferencedPathSource().getPathName(), null );

		final Expression sqlExpression;

		if ( mapping instanceof EntityAssociationMapping ) {
			final EntityAssociationMapping associationMapping = (EntityAssociationMapping) mapping;
			final ForeignKeyDescriptor keyDescriptor = associationMapping.getForeignKeyDescriptor();
			final TableReference tableReference = tableGroup.resolveTableReference(
					sqmPath.getNavigablePath(),
					keyDescriptor.getKeyTable()
			);

			if ( keyDescriptor instanceof SimpleForeignKeyDescriptor ) {
				final SimpleForeignKeyDescriptor simpleKeyDescriptor = (SimpleForeignKeyDescriptor) keyDescriptor;

				sqlExpression = sqlExprResolver.resolveSqlExpression(
						createColumnReferenceKey( tableReference, simpleKeyDescriptor.getSelectionExpression() ),
						processingState -> new ColumnReference(
								tableReference,
								simpleKeyDescriptor,
								sessionFactory
						)
				);
			}
			else {
				final List<Expression> expressions = new ArrayList<>();
				keyDescriptor.forEachSelectable(
						(selectionIndex, selectableMapping) -> sqlExprResolver.resolveSqlExpression(
								createColumnReferenceKey( tableReference, selectableMapping.getSelectionExpression() ),
								processingState -> new ColumnReference(
										tableReference,
										selectableMapping,
										sessionFactory
								)
						)
				);
				sqlExpression = new SqlTuple( expressions, keyDescriptor );
			}
		}
		else {
			assert mapping instanceof EntityMappingType;

			final EntityMappingType entityMappingType = (EntityMappingType) mapping;
			final EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();
			if ( identifierMapping instanceof BasicEntityIdentifierMapping ) {
				final BasicEntityIdentifierMapping simpleIdMapping = (BasicEntityIdentifierMapping) identifierMapping;

				final TableReference tableReference = tableGroup.resolveTableReference(
						sqmPath.getNavigablePath(),
						simpleIdMapping.getContainingTableExpression()
				);
				assert tableReference != null : "Could not resolve table-group : " + simpleIdMapping.getContainingTableExpression();

				sqlExpression = sqlExprResolver.resolveSqlExpression(
						createColumnReferenceKey( tableReference, simpleIdMapping.getSelectionExpression() ),
						processingState -> new ColumnReference(
								tableReference,
								simpleIdMapping,
								sessionFactory
						)
				);
			}
			else {
				final List<Expression> expressions = new ArrayList<>();
				identifierMapping.forEachSelectable(
						(selectionIndex, selectableMapping) -> {

						}
				);
				sqlExpression = new SqlTuple( expressions, identifierMapping );
			}
		}

		return new EntityValuedPathInterpretation<>(
				sqlExpression,
				sqmPath,
				tableGroup,
				mapping
		);
	}

	private final Expression sqlExpression;

	@SuppressWarnings({ "rawtypes", "unchecked" })
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

	@Override
	public SqlTuple getSqlTuple() {
		return sqlExpression instanceof SqlTuple
				? (SqlTuple) sqlExpression
				: null;
	}
	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				sqlExpression,
				getExpressionType().getJavaTypeDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public EntityValuedModelPart getExpressionType() {
		return (EntityValuedModelPart) super.getExpressionType();
	}

	@Override
	public DomainResult<T> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		final EntityValuedModelPart mappingType = getExpressionType();
		if ( mappingType instanceof EntityAssociationMapping ) {
			final NavigablePath navigablePath = getNavigablePath();

			// for a to-one or to-many we may not have yet joined to the association table,
			// but we need to because the association is a root return and needs to select
			// all of the entity columns

			final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
			final FromClauseAccess fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
			final EntityAssociationMapping associationMapping = (EntityAssociationMapping) mappingType;
			final TableGroup tableGroup = fromClauseAccess.resolveTableGroup(
					navigablePath,
					np -> {
						final TableGroup parentTableGroup;
						if ( getExpressionType() instanceof CollectionPart ) {
							parentTableGroup = fromClauseAccess.findTableGroup( np.getParent().getParent() );
						}
						else {
							parentTableGroup = getTableGroup();
						}

						final TableGroupJoin tableGroupJoin = associationMapping.createTableGroupJoin(
								navigablePath,
								parentTableGroup,
								null,
								SqlAstJoinType.INNER,
								false,
								LockMode.READ,
								sqlAstCreationState
						);

						return tableGroupJoin.getJoinedGroup();
					}
			);

			return associationMapping.createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
		}

		return super.createDomainResult( resultVariable, creationState );
	}
}
