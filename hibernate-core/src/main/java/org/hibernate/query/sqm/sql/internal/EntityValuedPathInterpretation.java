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

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.derived.AnonymousTupleEntityValuedModelPart;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.CorrelatedTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;

public class EntityValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements SqlTupleContainer,
		Assignable {

	public static <T> EntityValuedPathInterpretation<T> from(
			SqmEntityValuedSimplePath<T> sqmPath,
			MappingModelExpressible<?> inferredMapping,
			SqmToSqlAstConverter sqlAstCreationState) {
		final TableGroup tableGroup = sqlAstCreationState
				.getFromClauseAccess()
				.findTableGroup( sqmPath.getNavigablePath() );
		final EntityValuedModelPart pathMapping = (EntityValuedModelPart) tableGroup.getModelPart();
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
					return from(
							sqmPath.getNavigablePath(),
							tableGroup,
							pathMapping,
							inferredMapping,
							sqlAstCreationState
					);
				}
				// When the inferred mapping and the path mapping differ,
				// we always render the PK for the inferred and mapping path
				else {
					// If the path mapping refers to the primary key though,
					// we can also render FK as that is equivalent
					if ( pathAssociation.isReferenceToPrimaryKey() ) {
						return from(
								sqmPath.getNavigablePath(),
								tableGroup,
								pathMapping,
								inferredMapping,
								sqlAstCreationState
						);
					}
					else {
						// but if the association FK does not point to the primary key,
						// we can't allow FK optimizations as that is problematic for self-referential associations,
						// because then we would render the PK of the association owner instead of the target
						return from(
								sqmPath.getNavigablePath(),
								tableGroup,
								pathMapping.getEntityMappingType().getIdentifierMapping(),
								false,
								pathMapping,
								pathMapping,
								sqlAstCreationState
						);
					}
				}
			}
			else if ( pathMapping instanceof AnonymousTupleEntityValuedModelPart ) {
				// AnonymousEntityValuedModelParts use the PK, which the inferred path will also use in this case,
				// so we render this path as it is
				return from(
						sqmPath.getNavigablePath(),
						tableGroup,
						pathMapping,
						inferredMapping,
						sqlAstCreationState
				);
			}
			else {
				// This is the case when the inferred mapping is an association, but the path mapping is not,
				// or the path mapping and the inferred mapping are for the same association,
				// in which case we render this path like the inferred mapping
				return from(
						sqmPath.getNavigablePath(),
						tableGroup,
						(EntityValuedModelPart) inferredMapping,
						inferredMapping,
						sqlAstCreationState
				);
			}
		}
		else {
			// No inferred mapping available or it refers to an EntityMappingType
			return from(
					sqmPath.getNavigablePath(),
					tableGroup,
					pathMapping,
					inferredMapping,
					sqlAstCreationState
			);
		}
	}

	private static <T> EntityValuedPathInterpretation<T> from(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			EntityValuedModelPart mapping,
			MappingModelExpressible<?> inferredMapping,
			SqmToSqlAstConverter sqlAstCreationState) {
		final boolean allowFkOptimization;
		final ModelPart resultModelPart;
		final TableGroup resultTableGroup;
		// For association mappings where the FK optimization i.e. use of the parent table group is allowed,
		// we try to make use of it and the FK model part if possible based on the inferred mapping
		if ( mapping instanceof EntityAssociationMapping ) {
			final EntityAssociationMapping associationMapping = (EntityAssociationMapping) mapping;
			final ModelPart associationKeyTargetMatchPart = associationMapping.getKeyTargetMatchPart();
			final ModelPart keyTargetMatchPart;
			if ( associationKeyTargetMatchPart instanceof ToOneAttributeMapping ) {
				keyTargetMatchPart = ( (ToOneAttributeMapping) associationKeyTargetMatchPart ).getKeyTargetMatchPart();
			}
			else {
				keyTargetMatchPart = associationKeyTargetMatchPart;
			}

			if ( associationMapping.isFkOptimizationAllowed() ) {
				final boolean forceUsingForeignKeyAssociationSidePart;
				// The following is an optimization for EntityCollectionPart path mappings with a join table.
				// We can possibly avoid doing the join of the target table by using the parent table group
				// and forcing to use the foreign key part of the association side
				if ( inferredMapping != null && hasJoinTable( associationMapping ) ) {
					// But we need to make sure the inferred mapping points to the same FK target
					if ( inferredMapping instanceof EntityMappingType ) {
						// If the inferred mapping is an EntityMappingType, meaning it's some sort of root path,
						// we only have to make sure the FK target is part of the mapping type
						forceUsingForeignKeyAssociationSidePart = ( (EntityMappingType) inferredMapping ).findSubPart( keyTargetMatchPart.getPartName() ) != null;
					}
					else {
						// Otherwise, it must be an association mapping
						assert inferredMapping instanceof EntityAssociationMapping;
						// Comparing UK-based toOne with PK-based collection part
						// In this case, we compare based on PK/FK, and can use the FK part if it points to the PK
						forceUsingForeignKeyAssociationSidePart = ( (EntityAssociationMapping) inferredMapping ).getKeyTargetMatchPart() != keyTargetMatchPart
								&& associationMapping.isReferenceToPrimaryKey();
					}
				}
				else {
					forceUsingForeignKeyAssociationSidePart = false;
				}
				if ( forceUsingForeignKeyAssociationSidePart ) {
					if ( associationKeyTargetMatchPart instanceof ToOneAttributeMapping ) {
						resultModelPart = keyTargetMatchPart;
					}
					else {
						resultModelPart = associationMapping.getForeignKeyDescriptor()
								.getPart( associationMapping.getSideNature() );
					}
					resultTableGroup = sqlAstCreationState.getFromClauseAccess()
							.findTableGroup( tableGroup.getNavigablePath().getParent() );
				}
				else {
					if ( isCorrelated( tableGroup, sqlAstCreationState )
							|| !tableGroup.getNavigablePath().isParentOrEqual( navigablePath ) ) {
						// Access to the parent table group is forbidden for correlated table groups. For more details,
						// see: `ToOneAttributeMapping.createRootTableGroupJoin`
						// Due to that, we forcefully use the model part to which this association points to i.e. the target

						// Also force the use of the FK target key if the navigable path for this entity valued path is
						// not equal to or a child of the table group navigable path.
						// This can happen when using an implicit join path e.g. `where root.association.id is null`,
						// yet also an explicit join was made which is compatible e.g. `join fetch root.association`.
						// Since we have an explicit join in this case anyway, it's fine to us the FK target key.
						resultModelPart = associationMapping.getForeignKeyDescriptor()
								.getPart( associationMapping.getSideNature().inverse() );
					}
					else {
						resultModelPart = keyTargetMatchPart;
					}
					resultTableGroup = tableGroup;
				}
				allowFkOptimization = true;
			}
			else if ( inferredMapping == null && hasNotFound( mapping ) ) {
				// This is necessary to allow expression like `where root.notFoundAssociation is null`
				// to render to `alias.not_found_fk is null`, but IMO this shouldn't be done
				// todo: discuss removing this part and create a left joined table group instead?
				resultModelPart = keyTargetMatchPart;
				resultTableGroup = sqlAstCreationState.getFromClauseAccess()
						.findTableGroup( tableGroup.getNavigablePath().getParent() );
				allowFkOptimization = false;
			}
			else {
				// If the mapping is an inverse association, use the PK and disallow FK optimizations
				resultModelPart = ( (EntityAssociationMapping) mapping ).getAssociatedEntityMappingType().getIdentifierMapping();
				resultTableGroup = tableGroup;
				allowFkOptimization = false;
			}
		}
		else if ( mapping instanceof AnonymousTupleEntityValuedModelPart ) {
			resultModelPart = ( (AnonymousTupleEntityValuedModelPart) mapping ).getForeignKeyPart();
			resultTableGroup = tableGroup;
			allowFkOptimization = true;
		}
		else {
			// If the mapping is not an association, use the PK and disallow FK optimizations
			resultModelPart = mapping.getEntityMappingType().getIdentifierMapping();
			resultTableGroup = tableGroup;
			allowFkOptimization = false;
		}
		return from(
				navigablePath,
				resultTableGroup,
				resultModelPart,
				allowFkOptimization,
				mapping,
				mapping,
				sqlAstCreationState
		);
	}

	private static boolean isCorrelated(TableGroup tableGroup, SqmToSqlAstConverter sqlAstCreationState) {
		final SqlAstProcessingState processingState = sqlAstCreationState.getCurrentProcessingState();
		if ( !( processingState instanceof SqlAstQueryPartProcessingState )
				|| ( (SqlAstQueryPartProcessingState) processingState ).getInflightQueryPart().isRoot() ) {
			return false;
		}
		final FromClauseAccess fromClauseAccess = sqlAstCreationState.getFromClauseAccess();

		TableGroup realParentTableGroup = fromClauseAccess.findTableGroup( tableGroup.getNavigablePath().getParent() );
		while ( realParentTableGroup.getModelPart() instanceof EmbeddableValuedModelPart ) {
			realParentTableGroup = fromClauseAccess.findTableGroup( realParentTableGroup.getNavigablePath().getParent() );
		}
		return realParentTableGroup instanceof CorrelatedTableGroup;
	}

	private static boolean hasNotFound(EntityValuedModelPart mapping) {
		return mapping instanceof ToOneAttributeMapping && ( (ToOneAttributeMapping) mapping ).hasNotFoundAction();
	}

	private static boolean hasJoinTable(EntityAssociationMapping associationMapping) {
		return associationMapping instanceof EntityCollectionPart
				&& ( (EntityCollectionPart) associationMapping ).getCardinality() == EntityCollectionPart.Cardinality.MANY_TO_MANY;
	}

	public static <T> EntityValuedPathInterpretation<T> from(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			ModelPart resultModelPart,
			boolean allowFkOptimization,
			EntityValuedModelPart mapping,
			EntityValuedModelPart treatedMapping,
			SqmToSqlAstConverter sqlAstCreationState) {
		final SqlExpressionResolver sqlExprResolver = sqlAstCreationState.getSqlExpressionResolver();
		final Expression sqlExpression;

		if ( resultModelPart == null ) {
			// Expand to all columns of the entity mapping type, as we already did for the selection
			final EntityMappingType entityMappingType = mapping.getEntityMappingType();
			final EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();
			final EntityDiscriminatorMapping discriminatorMapping = entityMappingType.getDiscriminatorMapping();
			final int numberOfFetchables = entityMappingType.getNumberOfFetchables();
			final List<Expression> expressions = new ArrayList<>(
					numberOfFetchables + identifierMapping.getJdbcTypeCount()
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
						sqlExprResolver.resolveSqlExpression( tableReference, selectableMapping )
				);
			};
			identifierMapping.forEachSelectable( selectableConsumer );
			if ( discriminatorMapping != null ) {
				discriminatorMapping.forEachSelectable( selectableConsumer );
			}
			for ( int i = 0; i < numberOfFetchables; i++ ) {
				final Fetchable fetchable = entityMappingType.getFetchable( i );
				if ( fetchable.isSelectable() ) {
					fetchable.forEachSelectable( selectableConsumer );
				}
			}
			sqlExpression = new SqlTuple( expressions, entityMappingType );
		}
		else {
			if ( resultModelPart instanceof BasicValuedModelPart ) {
				final BasicValuedModelPart basicValuedModelPart = (BasicValuedModelPart) resultModelPart;
				final TableReference tableReference = tableGroup.resolveTableReference(
						navigablePath,
						basicValuedModelPart.getContainingTableExpression(),
						allowFkOptimization
				);
				sqlExpression = sqlExprResolver.resolveSqlExpression( tableReference, basicValuedModelPart );
			}
			else {
				final List<Expression> expressions = new ArrayList<>( resultModelPart.getJdbcTypeCount() );
				resultModelPart.forEachSelectable(
						(selectionIndex, selectableMapping) -> {
							final TableReference tableReference = tableGroup.resolveTableReference(
									navigablePath,
									selectableMapping.getContainingTableExpression(),
									allowFkOptimization
							);
							expressions.add( sqlExprResolver.resolveSqlExpression( tableReference, selectableMapping ) );
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
		applySqlSelections( sqlExpression, creationState.getSqlAstCreationState() );
	}

	private void applySqlSelections(Expression sqlExpression, SqlAstCreationState creationState) {
		if ( sqlExpression instanceof SqlTuple ) {
			for ( Expression expression : ( (SqlTuple) sqlExpression ).getExpressions() ) {
				applySqlSelections( expression, creationState );
			}
		}
		else {
			creationState.getSqlExpressionResolver().resolveSqlSelection(
					sqlExpression,
					getExpressionType().getJavaType(),
					null,
					creationState.getCreationContext().getMappingMetamodel().getTypeConfiguration()
			);
		}
	}

	@Override
	public EntityValuedModelPart getExpressionType() {
		return (EntityValuedModelPart) super.getExpressionType();
	}

}
