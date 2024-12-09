/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupport;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.query.derived.AnonymousTupleEntityValuedModelPart;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;

import jakarta.persistence.criteria.Selection;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EntityValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T>
		implements SqlTupleContainer, Assignable {
	private final Expression sqlExpression;
	private final @Nullable String affectedTableName;

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
		final ModelPart resultModelPart;
		final TableGroup resultTableGroup;
		// For association mappings where the FK optimization i.e. use of the parent table group is allowed,
		// we try to make use of it and the FK model part if possible based on the inferred mapping
		if ( mapping instanceof EntityAssociationMapping ) {
			final EntityAssociationMapping associationMapping = (EntityAssociationMapping) mapping;
			final ModelPart keyTargetMatchPart = associationMapping.getForeignKeyDescriptor().getPart(
					associationMapping.getSideNature()
			);

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
					resultModelPart = associationMapping.getForeignKeyDescriptor()
							.getPart( associationMapping.getSideNature() );
					resultTableGroup = sqlAstCreationState.getFromClauseAccess()
							.findTableGroup( tableGroup.getNavigablePath().getParent() );
				}
				else {
					if ( !tableGroup.getNavigablePath().isParentOrEqual( navigablePath ) ) {
						// Force the use of the FK target key if the navigable path for this entity valued path is
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
			}
			else if ( inferredMapping == null
					&& hasNotFound( mapping )
					&& sqlAstCreationState.getCurrentClauseStack().getCurrent() == Clause.SET ) {
				// for not-found mappings encountered in the SET clause of an UPDATE statement
				// we will want to (1) not join and (2) render the fk
				resultModelPart = keyTargetMatchPart;
				resultTableGroup = sqlAstCreationState.getFromClauseAccess()
						.findTableGroup( tableGroup.getNavigablePath().getParent() );
			}
			else {
				// If the mapping is an inverse association, use the PK and disallow FK optimizations
				resultModelPart = associationMapping.getAssociatedEntityMappingType().getIdentifierMapping();
				resultTableGroup = tableGroup;
			}
		}
		else if ( mapping instanceof AnonymousTupleEntityValuedModelPart ) {
			resultModelPart = ( (AnonymousTupleEntityValuedModelPart) mapping ).getForeignKeyPart();
			resultTableGroup = tableGroup;
		}
		else {
			// If the mapping is not an association, use the PK and disallow FK optimizations
			resultModelPart = mapping.getEntityMappingType().getIdentifierMapping();
			resultTableGroup = tableGroup;
		}
		return from(
				navigablePath,
				resultTableGroup,
				resultModelPart,
				mapping,
				mapping,
				sqlAstCreationState
		);
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
			EntityValuedModelPart mapping,
			EntityValuedModelPart treatedMapping,
			SqmToSqlAstConverter sqlAstCreationState) {
		final boolean expandToAllColumns;
		final Clause currentClause = sqlAstCreationState.getCurrentClauseStack().getCurrent();
		if ( currentClause == Clause.GROUP || currentClause == Clause.ORDER ) {
			assert sqlAstCreationState.getCurrentSqmQueryPart().isSimpleQueryPart();
			final SqmQuerySpec<?> querySpec = sqlAstCreationState.getCurrentSqmQueryPart().getFirstQuerySpec();
			if ( currentClause == Clause.ORDER && !querySpec.groupByClauseContains( navigablePath, sqlAstCreationState ) ) {
				// We must ensure that the order by expression be expanded but only if the group by
				// contained the same expression, and that was expanded as well
				expandToAllColumns = false;
			}
			else {
				// When the table group is selected and the navigablePath is selected we need to expand
				// to all columns, as we must make sure we include all columns present in the select clause
				expandToAllColumns = isSelected(
						tableGroup,
						navigablePath,
						querySpec,
						sqlAstCreationState.getCurrentProcessingState().isTopLevel()
				);
			}
		}
		else {
			expandToAllColumns = false;
		}

		final SqlExpressionResolver sqlExprResolver = sqlAstCreationState.getSqlExpressionResolver();
		final Expression sqlExpression;
		if ( expandToAllColumns ) {
			// Expand to all columns of the entity mapping type to ensure a correct group / order by expression,
			// or use only the primary key if the dialect supports functional dependency
			final Dialect dialect = sqlAstCreationState.getCreationContext()
					.getSessionFactory()
					.getJdbcServices()
					.getDialect();
			final EntityMappingType entityMappingType = mapping.getEntityMappingType();
			final EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();
			final List<Expression> expressions = new ArrayList<>( identifierMapping.getJdbcTypeCount() );
			final SelectableConsumer selectableConsumer = (selectionIndex, selectableMapping) -> {
				final TableReference tableReference = tableGroup.resolveTableReference(
						navigablePath,
						selectableMapping.getContainingTableExpression()
				);
				expressions.add( sqlExprResolver.resolveSqlExpression( tableReference, selectableMapping ) );
			};
			identifierMapping.forEachSelectable( selectableConsumer );
			if ( !supportsFunctionalDependency( dialect, entityMappingType ) ) {
				final EntityDiscriminatorMapping discriminatorMapping = entityMappingType.getDiscriminatorMapping();
				if ( discriminatorMapping != null ) {
					expressions.add( discriminatorMapping.resolveSqlExpression(
							navigablePath,
							discriminatorMapping.getUnderlyingJdbcMapping(),
							tableGroup,
							sqlAstCreationState
					) );
				}
				for ( int i = 0; i < entityMappingType.getNumberOfFetchables(); i++ ) {
					final Fetchable fetchable = entityMappingType.getFetchable( i );
					if ( fetchable.isSelectable() ) {
						fetchable.forEachSelectable( selectableConsumer );
					}
				}
			}
			sqlExpression = new SqlTuple( expressions, entityMappingType );
		}
		else {
			final BasicValuedModelPart basicValuedModelPart = resultModelPart.asBasicValuedModelPart();
			if ( basicValuedModelPart != null ) {
				final TableReference tableReference = tableGroup.resolveTableReference(
						navigablePath,
						basicValuedModelPart,
						basicValuedModelPart.getContainingTableExpression()
				);
				sqlExpression = sqlExprResolver.resolveSqlExpression( tableReference, basicValuedModelPart );
			}
			else {
				final List<Expression> expressions = new ArrayList<>( resultModelPart.getJdbcTypeCount() );
				resultModelPart.forEachSelectable(
						(selectionIndex, selectableMapping) -> {
							final TableReference tableReference = tableGroup.resolveTableReference(
									navigablePath,
									(ValuedModelPart) resultModelPart,
									selectableMapping.getContainingTableExpression()
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

	private static boolean isSelected(
			TableGroup tableGroup,
			NavigablePath path,
			SqmQuerySpec<?> sqmQuerySpec,
			boolean isTopLevel) {
		// If the table group is not initialized, i.e. not selected, no need to check selections
		if ( !tableGroup.isInitialized() || sqmQuerySpec.getSelectClause() == null ) {
			return false;
		}
		final NavigablePath tableGroupPath = isTopLevel ? null : tableGroup.getNavigablePath();
		for ( SqmSelection<?> selection : sqmQuerySpec.getSelectClause().getSelections() ) {
			if ( selectionContains( selection.getSelectableNode(), path, tableGroupPath ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean selectionContains(Selection<?> selection, NavigablePath path, NavigablePath tableGroupPath) {
		if ( selection instanceof SqmPath<?> ) {
			final SqmPath<?> sqmPath = (SqmPath<?>) selection;
			// Expansion is needed if the table group is null, i.e. we're in a top level query where EVPs are always
			// expanded to all columns, or if the selection is on the same table (lhs) as the group by expression ...
			return ( tableGroupPath == null || sqmPath.getLhs() != null && sqmPath.getLhs().getNavigablePath().equals( tableGroupPath ) )
					// ... and if the entity valued path is selected or any of its columns are
					&& path.isParentOrEqual( sqmPath.getNavigablePath() );
		}
		else if ( selection.isCompoundSelection() ) {
			for ( Selection<?> compoundSelection : selection.getCompoundSelectionItems() ) {
				if ( selectionContains( compoundSelection, path, tableGroupPath ) ) {
					return true;
				}
			}
		}
		else if ( selection instanceof SqmDynamicInstantiation ) {
			for ( SqmDynamicInstantiationArgument<?> argument : ( (SqmDynamicInstantiation<?>) selection ).getArguments() ) {
				if ( selectionContains( argument.getSelectableNode(), path, tableGroupPath ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean supportsFunctionalDependency(Dialect dialect, EntityMappingType entityMappingType) {
		final FunctionalDependencyAnalysisSupport analysisSupport = dialect.getFunctionalDependencyAnalysisSupport();
		if ( analysisSupport.supportsAnalysis() ) {
			if ( entityMappingType.getSqmMultiTableMutationStrategy() == null ) {
				return true;
			}
			else {
				return analysisSupport.supportsTableGroups() && ( analysisSupport.supportsConstants() ||
						// Union entity persisters use a literal 'clazz_' column as a discriminator
						// that breaks functional dependency for dialects that don't support constants
						!( entityMappingType.getEntityPersister() instanceof UnionSubclassEntityPersister ) );
			}
		}
		return false;
	}

	public EntityValuedPathInterpretation(
			Expression sqlExpression,
			NavigablePath navigablePath,
			TableGroup tableGroup,
			EntityValuedModelPart mapping) {
		this( sqlExpression, navigablePath, tableGroup, mapping, determineAffectedTableName( tableGroup, mapping ) );
	}

	public EntityValuedPathInterpretation(
			Expression sqlExpression,
			NavigablePath navigablePath,
			TableGroup tableGroup,
			EntityValuedModelPart mapping,
			@Nullable String affectedTableName) {
		super( navigablePath, mapping, tableGroup );
		this.sqlExpression = sqlExpression;
		this.affectedTableName = affectedTableName;
	}

	private static @Nullable String determineAffectedTableName(TableGroup tableGroup, EntityValuedModelPart mapping) {
		final ModelPartContainer modelPart = tableGroup.getModelPart();
		if ( modelPart instanceof EntityAssociationMapping && mapping instanceof ValuedModelPart ) {
			final EntityAssociationMapping associationMapping = (EntityAssociationMapping) modelPart;
			if ( !associationMapping.containsTableReference( ( (ValuedModelPart) mapping ).getContainingTableExpression() ) ) {
				return associationMapping.getAssociatedEntityMappingType().getMappedTableDetails().getTableName();
			}
		}
		return null;
	}

	@Override
	public Expression getSqlExpression() {
		return sqlExpression;
	}

	@Override
	public @Nullable String getAffectedTableName() {
		return affectedTableName;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		if ( affectedTableName != null && sqlTreeWalker instanceof SqlAstTranslator<?> ) {
			( (SqlAstTranslator<?>) sqlTreeWalker ).addAffectedTableName( affectedTableName );
		}
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
