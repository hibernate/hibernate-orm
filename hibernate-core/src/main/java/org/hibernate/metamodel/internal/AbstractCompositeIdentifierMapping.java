package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;

import jakarta.annotation.Nullable;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Base implementation for composite identifier mappings
 *
 * @author Andrea Boriero
 */
public abstract class AbstractCompositeIdentifierMapping
		implements CompositeIdentifierMapping, EmbeddableValuedFetchable, FetchOptions {
	private final NavigableRole navigableRole;
	private final String tableExpression;

	private final EntityMappingType entityMapping;

	protected final SessionFactoryImplementor sessionFactory;

	public AbstractCompositeIdentifierMapping(
			EntityMappingType entityMapping,
			String tableExpression,
			MappingModelCreationProcess creationProcess) {
		this.navigableRole =
				entityMapping.getNavigableRole()
						.appendContainer( ID_ROLE_NAME );
		this.entityMapping = entityMapping;
		this.tableExpression = tableExpression;
		this.sessionFactory = creationProcess.getCreationContext().getSessionFactory();
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected AbstractCompositeIdentifierMapping(AbstractCompositeIdentifierMapping original) {
		this.navigableRole = original.navigableRole;
		this.entityMapping = original.entityMapping;
		this.tableExpression = original.tableExpression;
		this.sessionFactory = original.sessionFactory;
	}

	@Override
	public boolean hasContainingClass() {
		return true;
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getMappedType() {
		return getPartMappingType();
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return getPartMappingType();
	}

	@Nonnull
	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Nonnull
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
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		final var joinType = determineSqlJoinType( lhs, requestedJoinType, fetched );
		final var tableGroup = createRootTableGroupJoin(
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
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			@Nullable Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		return new StandardVirtualTableGroup( navigablePath, this, lhs, fetched );
	}

	@Nullable
	@Override
	public ModelPart findSubPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType) {
		return getPartMappingType().findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(
			@Nonnull Consumer<ModelPart> consumer,
			@Nullable EntityMappingType treatTargetType) {
		getPartMappingType().visitSubParts( consumer, treatTargetType );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		int span = 0;
		final var embeddableTypeDescriptor = getEmbeddableTypeDescriptor();
		final int size = embeddableTypeDescriptor.getNumberOfAttributeMappings();
		if ( value == null ) {
			for ( int i = 0; i < size; i++ ) {
				final var attributeMapping = embeddableTypeDescriptor.getAttributeMapping( i );
				span += attributeMapping.forEachJdbcValue( null, span + offset, x, y, valuesConsumer, session );
			}
		}
		else {
			for ( int i = 0; i < size; i++ ) {
				final var attributeMapping = embeddableTypeDescriptor.getAttributeMapping( i );
				final Object object = embeddableTypeDescriptor.getValue( value, i );
				if ( attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping ) {
					final var foreignKeyDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
					final var inverse = toOneAttributeMapping.getSideNature().inverse();
					final Object identifier =
							foreignKeyDescriptor.getAssociationKeyFromSide( object, inverse, session );
					span += foreignKeyDescriptor.forEachJdbcValue(
							identifier,
							span + offset,
							x,
							y,
							valuesConsumer,
							session
					);
				}
				else {
					span += attributeMapping.forEachJdbcValue( object, span + offset, x, y, valuesConsumer, session );
				}
			}
		}
		return span;
	}

	@Nonnull
	@Override
	public SqlTuple toSqlExpression(
			@Nonnull TableGroup tableGroup,
			@Nonnull Clause clause,
			@Nonnull SqmToSqlAstConverter walker,
			@Nonnull SqlAstCreationState sqlAstCreationState) {
		final List<ColumnReference> columnReferences = arrayList( getEmbeddableTypeDescriptor().getJdbcTypeCount() );
		final var navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
		final var defaultTableReference = tableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
		getEmbeddableTypeDescriptor().forEachSelectable(
				(columnIndex, selection) -> {
					final String containingTableExpression = selection.getContainingTableExpression();
					final var tableReference =
							getContainingTableExpression().equals( containingTableExpression )
									? defaultTableReference
									: tableGroup.resolveTableReference( navigablePath, containingTableExpression );
					final var columnReference =
							sqlAstCreationState.getSqlExpressionResolver()
									.resolveSqlExpression( tableReference, selection );
					columnReferences.add( (ColumnReference) columnReference );
				}
		);
		return new SqlTuple( columnReferences, this );
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		return new EmbeddableResultImpl<>(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Nullable
	@Override
	public Object instantiate() {
		return getEntityMapping().getRepresentationStrategy().getInstantiator().instantiate();
	}

	@Nonnull
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return entityMapping;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	protected EntityMappingType getEntityMapping() {
		return entityMapping;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		return entityMapping.containsTableReference( tableExpression );
	}

}
