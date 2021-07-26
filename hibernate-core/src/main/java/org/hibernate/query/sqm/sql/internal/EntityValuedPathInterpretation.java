/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.internal.SimpleForeignKeyDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
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
		final SqmPath<?> realPath;
		if ( CollectionPart.Nature.ELEMENT.getName().equals( sqmPath.getNavigablePath().getUnaliasedLocalName() ) ) {
			realPath = sqmPath.getLhs();
		}
		else {
			realPath = sqmPath;
		}
		final TableGroup tableGroup = sqlAstCreationState
				.getFromClauseAccess()
				.findTableGroup( realPath.getLhs().getNavigablePath() );

		final EntityValuedModelPart mapping = (EntityValuedModelPart) sqlAstCreationState
				.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() )
				.getModelPart()
				.findSubPart( sqmPath.getReferencedPathSource().getPathName(), null );
		return from( sqmPath.getNavigablePath(), tableGroup, mapping, false, sqlAstCreationState );
	}

	public static <T> EntityValuedPathInterpretation<T> from(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			EntityValuedModelPart mapping,
			boolean expandToAllColumns,
			SqmToSqlAstConverter sqlAstCreationState) {
		final SqlExpressionResolver sqlExprResolver = sqlAstCreationState.getSqlExpressionResolver();
		final SessionFactoryImplementor sessionFactory = sqlAstCreationState.getCreationContext().getSessionFactory();
		final Expression sqlExpression;

		if ( expandToAllColumns ) {
			final EntityMappingType entityMappingType = mapping.getEntityMappingType();
			final EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();
			final EntityDiscriminatorMapping discriminatorMapping = entityMappingType.getDiscriminatorMapping();
			final List<Expression> expressions = new ArrayList<>(
					mapping.getJdbcTypeCount() + identifierMapping.getJdbcTypeCount()
							+ ( discriminatorMapping == null ? 0 : 1 )
			);
			final SelectableConsumer selectableConsumer = (selectionIndex, selectableMapping) -> {
				final TableReference tableReference = tableGroup.resolveTableReference(
						navigablePath,
						selectableMapping.getContainingTableExpression()
				);
				expressions.add(
						sqlExprResolver.resolveSqlExpression(
								createColumnReferenceKey(
										tableReference,
										selectableMapping.getSelectionExpression()
								),
								processingState -> new ColumnReference(
										tableReference,
										selectableMapping,
										sessionFactory
								)
						)
				);
			};
			identifierMapping.forEachSelectable( selectableConsumer );
			if ( discriminatorMapping != null ) {
				discriminatorMapping.forEachSelectable( selectableConsumer );
			}
			mapping.forEachSelectable( selectableConsumer );
			sqlExpression = new SqlTuple( expressions, mapping );
		}
		else if ( mapping instanceof EntityAssociationMapping ) {
			final EntityAssociationMapping associationMapping = (EntityAssociationMapping) mapping;
			final ForeignKeyDescriptor fkDescriptor = associationMapping.getForeignKeyDescriptor();
			final String lhsTable;
			final ModelPart lhsPart;
			if ( associationMapping.getSideNature() == ForeignKeyDescriptor.Nature.KEY ) {
				lhsTable = fkDescriptor.getKeyTable();
				lhsPart = fkDescriptor.getKeyPart();
			}
			else {
				lhsTable = fkDescriptor.getTargetTable();
				lhsPart = fkDescriptor.getTargetPart();
			}
			final TableReference tableReference = tableGroup.resolveTableReference(
					navigablePath,
					lhsTable
			);

			if ( fkDescriptor instanceof SimpleForeignKeyDescriptor ) {
				final BasicValuedModelPart basicValuedModelPart = (BasicValuedModelPart) lhsPart;

				sqlExpression = sqlExprResolver.resolveSqlExpression(
						createColumnReferenceKey( tableReference, basicValuedModelPart.getSelectionExpression() ),
						processingState -> new ColumnReference(
								tableReference,
								basicValuedModelPart,
								sessionFactory
						)
				);
			}
			else {
				final List<Expression> expressions = new ArrayList<>( fkDescriptor.getJdbcTypeCount() );
				lhsPart.forEachSelectable(
						(selectionIndex, selectableMapping) -> expressions.add(
								sqlExprResolver.resolveSqlExpression(
										createColumnReferenceKey(
												tableReference,
												selectableMapping.getSelectionExpression()
										),
										processingState -> new ColumnReference(
												tableReference,
												selectableMapping,
												sessionFactory
										)
								)
						)
				);
				sqlExpression = new SqlTuple( expressions, lhsPart );
			}
		}
		else {
			assert mapping instanceof EntityMappingType;

			final EntityMappingType entityMappingType = (EntityMappingType) mapping;
			final EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();
			if ( identifierMapping instanceof BasicEntityIdentifierMapping ) {
				final BasicEntityIdentifierMapping simpleIdMapping = (BasicEntityIdentifierMapping) identifierMapping;

				final TableReference tableReference = tableGroup.resolveTableReference(
						navigablePath,
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
							final TableReference tableReference = tableGroup.resolveTableReference(
									navigablePath, selectableMapping.getContainingTableExpression() );

							expressions.add(
									sqlExprResolver.resolveSqlExpression(
											createColumnReferenceKey(
													tableReference,
													selectableMapping.getSelectionExpression()
											),
											processingState -> new ColumnReference(
													tableReference,
													selectableMapping,
													sessionFactory
											)
									)
							);
						}
				);
				sqlExpression = new SqlTuple( expressions, identifierMapping );
			}
		}

		return new EntityValuedPathInterpretation<>(
				sqlExpression,
				navigablePath,
				tableGroup,
				mapping
		);
	}

	private final Expression sqlExpression;

	public EntityValuedPathInterpretation(
			Expression sqlExpression,
			NavigablePath navigablePath,
			TableGroup tableGroup,
			EntityValuedModelPart mapping) {
		super( navigablePath, mapping, tableGroup );
		this.sqlExpression = sqlExpression;
	}

	@Override
	public Expression getSqlExpression() {
		return sqlExpression;
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
