/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
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
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.tuple.ValueGeneration;

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
			String tableExpression,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			String parentInjectionAttributeName,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EmbeddableMappingType embeddableMappingType,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess,
			ValueGeneration valueGeneration) {
		this(
			name,
			navigableRole,
			stateArrayPosition,
			tableExpression,
			attributeMetadataAccess,
			getPropertyAccess( parentInjectionAttributeName, embeddableMappingType ),
			mappedFetchTiming,
			mappedFetchStyle,
			embeddableMappingType,
			declaringType,
			propertyAccess,
			valueGeneration
		);
	}

	public EmbeddedAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			String tableExpression,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			PropertyAccess parentInjectionAttributePropertyAccess,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EmbeddableMappingType embeddableMappingType,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess,
			ValueGeneration valueGeneration) {
		super(
				name,
				stateArrayPosition,
				attributeMetadataAccess,
				mappedFetchTiming,
				mappedFetchStyle,
				declaringType,
				propertyAccess,
				valueGeneration
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
				null,
				inverseModelPart.getMappedFetchOptions(),
				keyDeclaringType,
				null,
				null
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
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return getEmbeddableTypeDescriptor().forEachSelectable( offset, consumer );
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		getEmbeddableTypeDescriptor().breakDownJdbcValues( domainValue, valueConsumer, session );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
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
		final List<ColumnReference> columnReferences = CollectionHelper.arrayList( embeddableMappingType.getJdbcTypeCount() );
		final NavigablePath navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
		final TableReference defaultTableReference = tableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
		getEmbeddableTypeDescriptor().forEachSelectable(
				(columnIndex, selection) -> {
					final TableReference tableReference = defaultTableReference.resolveTableReference( selection.getContainingTableExpression() ) != null
							? defaultTableReference
							: tableGroup.resolveTableReference( navigablePath, selection.getContainingTableExpression() );
					final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
							SqlExpressionResolver.createColumnReferenceKey(
									tableReference,
									selection.getSelectionExpression()
							),
							sqlAstProcessingState -> new ColumnReference(
									tableReference.getIdentificationVariable(),
									selection,
									sqlAstCreationState.getCreationContext().getSessionFactory()
							)
					);

					columnReferences.add( columnReference.unwrap( ColumnReference.class ) );
				}
		);

		return new SqlTuple( columnReferences, this );
	}

	@Override
	public ModelPart findSubPart(
			String name,
			EntityMappingType treatTargetType) {
		return getMappedType().findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer,
			EntityMappingType treatTargetType) {
		getMappedType().visitSubParts( consumer, treatTargetType );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final SqlAstJoinType joinType;
		if ( requestedJoinType == null ) {
			joinType = SqlAstJoinType.INNER;
		}
		else {
			joinType = requestedJoinType;
		}
		final TableGroup tableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				requestedJoinType,
				fetched,
				null,
				aliasBaseGenerator,
				sqlExpressionResolver,
				fromClauseAccess,
				creationContext
		);

		return new TableGroupJoin( navigablePath, joinType, tableGroup );
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		return new StandardVirtualTableGroup(
				navigablePath,
				this,
				lhs,
				fetched
		);
	}

	@Override
	public String getSqlAliasStem() {
		return getAttributeName();
	}

	@Override
	public int getNumberOfFetchables() {
		return getEmbeddableTypeDescriptor().getNumberOfAttributeMappings();
	}

	@Override
	public String toString() {
		return "EmbeddedAttributeMapping(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	public AttributeMapping copy(ManagedMappingType declaringType) {
		return new EmbeddedAttributeMapping(
				getAttributeName(),
				getNavigableRole(),
				getStateArrayPosition(),
				tableExpression,
				getAttributeMetadataAccess(),
				getParentInjectionAttributePropertyAccess(),
				getTiming(),
				getStyle(),
				getEmbeddableTypeDescriptor(),
				declaringType,
				getPropertyAccess(),
				getValueGeneration()
		);
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
}
