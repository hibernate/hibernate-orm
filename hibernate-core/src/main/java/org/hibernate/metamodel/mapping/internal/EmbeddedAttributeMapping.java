/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PropertyBasedMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.AggregateEmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.AggregateEmbeddableResultImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;

/**
 * @author Steve Ebersole
 */
public class EmbeddedAttributeMapping
		extends AbstractSingularAttributeMapping
		implements EmbeddableValuedFetchable, Fetchable {
	private final NavigableRole navigableRole;

	private final String tableExpression;
	private final EmbeddableMappingType embeddableMappingType;
	private final PropertyAccess parentInjectionAttributePropertyAccess;

	public EmbeddedAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			String tableExpression,
			AttributeMetadata attributeMetadata,
			String parentInjectionAttributeName,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EmbeddableMappingType embeddableMappingType,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		this(
			name,
			navigableRole,
			stateArrayPosition,
			fetchableIndex,
			tableExpression,
			attributeMetadata,
			getPropertyAccess( parentInjectionAttributeName, embeddableMappingType ),
			mappedFetchTiming,
			mappedFetchStyle,
			embeddableMappingType,
			declaringType,
			propertyAccess
		);
	}

	public EmbeddedAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			String tableExpression,
			AttributeMetadata attributeMetadata,
			PropertyAccess parentInjectionAttributePropertyAccess,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EmbeddableMappingType embeddableMappingType,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				name,
				stateArrayPosition,
				fetchableIndex,
				attributeMetadata,
				mappedFetchTiming,
				mappedFetchStyle,
				declaringType,
				propertyAccess
		);
		this.navigableRole = navigableRole;

		this.parentInjectionAttributePropertyAccess = parentInjectionAttributePropertyAccess;
		this.tableExpression = tableExpression;

		this.embeddableMappingType = embeddableMappingType;

	}

	// Constructor is only used for creating the inverse attribute mapping
	EmbeddedAttributeMapping(
			ManagedMappingType keyDeclaringType,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			EmbeddableValuedModelPart inverseModelPart,
			EmbeddableMappingType embeddableTypeDescriptor,
			MappingModelCreationProcess creationProcess) {
		super(
				inverseModelPart.getFetchableName(),
				-1,
				inverseModelPart.getFetchableKey(),
				inverseModelPart instanceof AttributeMapping
						? inverseModelPart.asAttributeMapping().getAttributeMetadata()
						: null,
				inverseModelPart.getMappedFetchOptions(),
				keyDeclaringType,
				inverseModelPart instanceof PropertyBasedMapping
						? ( (PropertyBasedMapping) inverseModelPart ).getPropertyAccess()
						: null
		);

		this.navigableRole = inverseModelPart.getNavigableRole().getParent().append( inverseModelPart.getFetchableName() );

		this.tableExpression = selectableMappings.getSelectable( 0 ).getContainingTableExpression();
		this.embeddableMappingType = embeddableTypeDescriptor.createInverseMappingType(
				this,
				declaringTableGroupProducer,
				selectableMappings,
				creationProcess
		);
		this.parentInjectionAttributePropertyAccess = null;
	}

	@Override
	public EmbeddableMappingType getMappedType() {
		return getEmbeddableTypeDescriptor();
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return embeddableMappingType;
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public PropertyAccess getParentInjectionAttributePropertyAccess() {
		return parentInjectionAttributePropertyAccess;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		if ( embeddableMappingType.shouldSelectAggregateMapping() ) {
			return new AggregateEmbeddableResultImpl<>(
					navigablePath,
					this,
					resultVariable,
					creationState
			);
		}
		return new EmbeddableResultImpl<>(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		embeddableMappingType.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		embeddableMappingType.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		if ( embeddableMappingType.shouldSelectAggregateMapping() ) {
			return new AggregateEmbeddableFetchImpl(
					fetchablePath,
					this,
					fetchParent,
					fetchTiming,
					selected,
					creationState
			);
		}
		return new EmbeddableFetchImpl(
				fetchablePath,
				this,
				fetchParent,
				fetchTiming,
				selected,
				creationState
		);
	}

	@Override
	public SqlTuple toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		if ( embeddableMappingType.getAggregateMapping() != null ) {
			final SelectableMapping selection = embeddableMappingType.getAggregateMapping();
			final NavigablePath navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
			final TableReference tableReference = tableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
			return new SqlTuple(
					Collections.singletonList(
							sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
									tableReference,
									selection
							)
					),
					this
			);
		}
		final List<ColumnReference> columnReferences = CollectionHelper.arrayList( embeddableMappingType.getJdbcTypeCount() );
		final NavigablePath navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
		final TableReference defaultTableReference = tableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
		getEmbeddableTypeDescriptor().forEachSelectable(
				(columnIndex, selection) -> {
					final TableReference tableReference = getContainingTableExpression().equals( selection.getContainingTableExpression() )
							? defaultTableReference
							: tableGroup.resolveTableReference( navigablePath, selection.getContainingTableExpression() );
					final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
							tableReference,
							selection
					);

					columnReferences.add( columnReference.getColumnReference() );
				}
		);

		return new SqlTuple( columnReferences, this );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		final SqlAstJoinType joinType = requestedJoinType == null ? SqlAstJoinType.INNER : requestedJoinType;
		final TableGroup tableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				explicitSqlAliasBase,
				requestedJoinType,
				fetched,
				null,
				creationState
		);

		return new TableGroupJoin( navigablePath, joinType, tableGroup );
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		return new StandardVirtualTableGroup( navigablePath, this, lhs, fetched );
	}

	@Override
	public String getSqlAliasStem() {
		return getAttributeName();
	}

	@Override
	public String toString() {
		return "EmbeddedAttributeMapping(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	private static PropertyAccess getPropertyAccess(
			String parentInjectionAttributeName,
			EmbeddableMappingType embeddableMappingType) {
		final PropertyAccess parentInjectionAttributePropertyAccess;
		if ( parentInjectionAttributeName != null ) {
			parentInjectionAttributePropertyAccess = PropertyAccessStrategyBasicImpl.INSTANCE.buildPropertyAccess(
					embeddableMappingType.getMappedJavaType().getJavaTypeClass(),
					parentInjectionAttributeName,
					true );
		}
		else {
			parentInjectionAttributePropertyAccess = null;
		}
		return parentInjectionAttributePropertyAccess;
	}

	@Override
	public EmbeddedAttributeMapping asEmbeddedAttributeMapping() {
		return this;
	}

	@Override
	public boolean isEmbeddedAttributeMapping() {
		return true;
	}
}
