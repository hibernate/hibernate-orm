/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.results.graph.DomainResultCreationState;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Koen Aers
 */
public class EntityValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements SqlTupleContainer,
		Assignable {

	public static <T> EntityValuedPathInterpretation<T> from(
			SqmEntityValuedSimplePath<T> sqmPath,
			MappingModelExpressible<?> inferredMapping,
			SqmToSqlAstConverter sqlAstCreationState) {
		final TableGroup tableGroup = sqlAstCreationState
				.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() );
		final EntityValuedModelPart pathMapping = (EntityValuedModelPart) sqlAstCreationState
				.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() )
				.getModelPart()
				.findSubPart( sqmPath.getReferencedPathSource().getPathName(), null );
		final EntityValuedModelPart mapping;
		if ( inferredMapping instanceof EntityAssociationMapping ) {
			final EntityAssociationMapping inferredAssociation = (EntityAssociationMapping) inferredMapping;
			if ( pathMapping instanceof EntityAssociationMapping && inferredMapping != pathMapping ) {
				// In here, the inferred mapping and the actual path mapping are association mappings,
				// but for different associations, so we have to check if both associations point to the same target
				final EntityAssociationMapping pathAssociation = (EntityAssociationMapping) pathMapping;
				final ModelPart pathTargetPart = pathAssociation.getForeignKeyDescriptor()
						.getPart( pathAssociation.getSideNature().inverse() );
				final ModelPart inferredTargetPart = inferredAssociation.getForeignKeyDescriptor()
						.getPart( inferredAssociation.getSideNature().inverse() );
				// If the inferred association and path association targets are the same, we can use the path mapping type
				// which will render the FK of the path association
				if ( pathTargetPart == inferredTargetPart ) {
					mapping = pathMapping;
				}
				else {
					// Otherwise, we need to use the entity mapping type to force rendering the PK
					// for e.g. `a.assoc1 = a.assoc2` when both associations have different target join columns
					mapping = pathMapping.getEntityMappingType();
				}
			}
			else {
				// This is the case when the inferred mapping is an association, but the path mapping is not,
				// or the path mapping and the inferred mapping are for the same association
				mapping = (EntityValuedModelPart) inferredMapping;
			}
		}
		else {
			mapping = pathMapping;
		}
		final ModelPart resultModelPart;
		if ( mapping instanceof EntityAssociationMapping ) {
			final EntityAssociationMapping associationMapping = (EntityAssociationMapping) mapping;
			final ModelPart keyTargetMatchPart = associationMapping.getKeyTargetMatchPart();
			if ( keyTargetMatchPart instanceof ToOneAttributeMapping ) {
				resultModelPart = ( (ToOneAttributeMapping) keyTargetMatchPart ).getKeyTargetMatchPart();
			}
			else {
				resultModelPart = keyTargetMatchPart;
			}
		}
		else {
			resultModelPart = mapping.getEntityMappingType().getIdentifierMapping();
		}
		return from(
				sqmPath.getNavigablePath(),
				tableGroup,
				resultModelPart,
				mapping,
				mapping,
				sqlAstCreationState
		);
	}

	public static <T> EntityValuedPathInterpretation<T> from(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			ModelPart resultModelPart,
			EntityValuedModelPart mapping,
			EntityValuedModelPart treatedMapping,
			SqmToSqlAstConverter sqlAstCreationState) {
		final SqlExpressionResolver sqlExprResolver = sqlAstCreationState.getSqlExpressionResolver();
		final SessionFactoryImplementor sessionFactory = sqlAstCreationState.getCreationContext().getSessionFactory();
		final Expression sqlExpression;

		if ( resultModelPart == null ) {
			final EntityMappingType entityMappingType = mapping.getEntityMappingType();
			final EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();
			final EntityDiscriminatorMapping discriminatorMapping = entityMappingType.getDiscriminatorMapping();
			final List<Expression> expressions = new ArrayList<>(
					entityMappingType.getJdbcTypeCount() + identifierMapping.getJdbcTypeCount()
							+ ( discriminatorMapping == null ? 0 : 1 )
			);
			final TableGroup parentTableGroup = tableGroup;
			final SelectableConsumer selectableConsumer = (selectionIndex, selectableMapping) -> {
				final TableReference tableReference = parentTableGroup.resolveTableReference(
						navigablePath,
						selectableMapping.getContainingTableExpression(),
						false
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
			entityMappingType.forEachSelectable( selectableConsumer );
			sqlExpression = new SqlTuple( expressions, entityMappingType );
		}
		else {
			if ( resultModelPart instanceof BasicValuedModelPart ) {
				final BasicValuedModelPart basicValuedModelPart = (BasicValuedModelPart) resultModelPart;
				final TableReference tableReference = tableGroup.resolveTableReference(
						navigablePath,
						basicValuedModelPart.getContainingTableExpression()
				);
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
				final List<Expression> expressions = new ArrayList<>( resultModelPart.getJdbcTypeCount() );
				resultModelPart.forEachSelectable(
						(selectionIndex, selectableMapping) -> {
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
						}
				);
				sqlExpression = new SqlTuple( expressions, resultModelPart );
			}
		}
		return new EntityValuedPathInterpretation<>(
				sqlExpression,
				navigablePath,
				tableGroup,
				treatedMapping
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
	public List<ColumnReference> getColumnReferences() {
		if ( sqlExpression instanceof SqlTuple ) {
			//noinspection unchecked
			return (List<ColumnReference>) ( (SqlTuple) sqlExpression ).getExpressions();
		}
		return Collections.singletonList( (ColumnReference) sqlExpression );
	}

	@Override
	public void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		if ( sqlExpression instanceof SqlTuple ) {
			for ( Expression e : ( (SqlTuple) sqlExpression ).getExpressions() ) {
				columnReferenceConsumer.accept( (ColumnReference) e );
			}
		}
		else {
			columnReferenceConsumer.accept( (ColumnReference) sqlExpression );
		}
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
				getExpressionType().getJavaType(),
				creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public EntityValuedModelPart getExpressionType() {
		return (EntityValuedModelPart) super.getExpressionType();
	}

}
