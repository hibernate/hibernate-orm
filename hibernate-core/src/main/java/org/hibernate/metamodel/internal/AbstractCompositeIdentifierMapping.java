/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;

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
		this.navigableRole = entityMapping.getNavigableRole().appendContainer( EntityIdentifierMapping.ID_ROLE_NAME );
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

	@Override
	public EmbeddableMappingType getMappedType() {
		return getPartMappingType();
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return getPartMappingType();
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
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
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		final SqlAstJoinType joinType = determineSqlJoinType( lhs, requestedJoinType, fetched );
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

		return new TableGroupJoin( navigablePath, joinType, tableGroup, null );
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
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return getPartMappingType().findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer,
			EntityMappingType treatTargetType) {
		getPartMappingType().visitSubParts( consumer, treatTargetType );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;
		final EmbeddableMappingType embeddableTypeDescriptor = getEmbeddableTypeDescriptor();
		final int size = embeddableTypeDescriptor.getNumberOfAttributeMappings();
		if ( value == null ) {
			for ( int i = 0; i < size; i++ ) {
				final AttributeMapping attributeMapping = embeddableTypeDescriptor.getAttributeMapping( i );
				span += attributeMapping.forEachJdbcValue( null, span + offset, x, y, valuesConsumer, session );
			}
		}
		else {
			for ( int i = 0; i < size; i++ ) {
				final AttributeMapping attributeMapping = embeddableTypeDescriptor.getAttributeMapping( i );
				final Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
				if ( attributeMapping instanceof ToOneAttributeMapping ) {
					final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
					final ForeignKeyDescriptor fkDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
					final Object identifier = fkDescriptor.getAssociationKeyFromSide(
							o,
							toOneAttributeMapping.getSideNature().inverse(),
							session
					);
					span += fkDescriptor.forEachJdbcValue(
							identifier,
							span + offset,
							x,
							y,
							valuesConsumer,
							session
					);
				}
				else {
					span += attributeMapping.forEachJdbcValue( o, span + offset, x, y, valuesConsumer, session );
				}
			}
		}
		return span;
	}

	@Override
	public SqlTuple toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		final SelectableMappings selectableMappings = getEmbeddableTypeDescriptor();
		final List<ColumnReference> columnReferences = CollectionHelper.arrayList( selectableMappings.getJdbcTypeCount() );
		final NavigablePath navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
		final TableReference defaultTableReference = tableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
		getEmbeddableTypeDescriptor().forEachSelectable(
				(columnIndex, selection) -> {
					final TableReference tableReference = getContainingTableExpression().equals( selection.getContainingTableExpression() )
							? defaultTableReference
							: tableGroup.resolveTableReference( navigablePath, selection.getContainingTableExpression() );
					final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver()
							.resolveSqlExpression( tableReference, selection );

					columnReferences.add( (ColumnReference) columnReference );
				}
		);

		return new SqlTuple( columnReferences, this );
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
	public Object instantiate() {
		return getEntityMapping().getRepresentationStrategy().getInstantiator().instantiate( sessionFactory );
	}

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
