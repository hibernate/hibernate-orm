/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.mapping.Any;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.AnyType;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNullElse;

/**
 * @author Steve Ebersole
 */
public class DiscriminatedCollectionPart implements DiscriminatedAssociationModelPart, CollectionPart {
	private final Nature nature;

	private final NavigableRole partRole;
	private final CollectionPersister collectionDescriptor;
	private final DiscriminatedAssociationMapping associationMapping;

	public DiscriminatedCollectionPart(
			Nature nature,
			CollectionPersister collectionDescriptor,
			JavaType<Object> baseAssociationJtd,
			Any bootValueMapping,
			AnyType anyType,
			MappingModelCreationProcess creationProcess) {
		this.nature = nature;
		this.partRole = collectionDescriptor.getNavigableRole().append( nature.getName() );
		this.collectionDescriptor = collectionDescriptor;
		this.associationMapping = DiscriminatedAssociationMapping.from(
				partRole,
				baseAssociationJtd,
				this,
				anyType,
				bootValueMapping,
				creationProcess
		);
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public PluralAttributeMapping getCollectionAttribute() {
		return collectionDescriptor.getAttributeMapping();
	}

	@Override
	public DiscriminatorMapping getDiscriminatorMapping() {
		return associationMapping.getDiscriminatorPart();
	}

	@Override
	public void applyDiscriminator(Consumer<Predicate> predicateConsumer, String alias, TableGroup tableGroup, SqlAstCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BasicValuedModelPart getKeyPart() {
		return associationMapping.getKeyPart();
	}

	@Override
	public EntityMappingType resolveDiscriminatorValue(Object discriminatorValue) {
		return associationMapping.resolveDiscriminatorValueToEntityMapping( discriminatorValue );
	}

	@Override
	public Object resolveDiscriminatorForEntityType(EntityMappingType entityMappingType) {
		return associationMapping.resolveDiscriminatorValueToEntityMapping( entityMappingType );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		associationMapping.getDiscriminatorPart().forEachSelectable( offset, consumer );
		associationMapping.getKeyPart().forEachSelectable( offset + 1, consumer );
		return 2;
	}

	@Override
	public String getFetchableName() {
		return nature.getName();
	}

	@Override
	public int getFetchableKey() {
		return nature == Nature.INDEX || !collectionDescriptor.hasIndex() ? 0 : 1;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return associationMapping;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return associationMapping.getDiscriminatorPart().isPartitioned()
			|| associationMapping.getKeyPart().isPartitioned();
	}

	@Override
	public String toString() {
		return "DiscriminatedCollectionPart(" + getNavigableRole() + ")@" + System.identityHashCode( this );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		return associationMapping.generateFetch(
				fetchParent,
				fetchablePath,
				fetchTiming,
				selected,
				resultVariable,
				creationState
		);
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return associationMapping.createDomainResult(
				navigablePath,
				tableGroup,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		associationMapping.getDiscriminatorPart().applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		associationMapping.getDiscriminatorPart().applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public MappingType getPartMappingType() {
		return associationMapping;
	}

	@Override
	public JavaType<?> getJavaType() {
		return associationMapping.getJavaType();
	}

	@Override
	public MappingType getMappedType() {
		return getPartMappingType();
	}

	@Override
	public JavaType<?> getExpressibleJavaType() {
		return getJavaType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return partRole;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return collectionDescriptor.getAttributeMapping().findContainingEntityMapping();
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return associationMapping.findSubPart( name, treatTargetType );
	}

	@Override
	public void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget) {
		consumer.accept( 0, getDiscriminatorPart() );
		consumer.accept( 1, getKeyPart() );
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		consumer.accept( getDiscriminatorPart() );
		consumer.accept( getKeyPart() );
	}

	@Override
	public int getNumberOfFetchables() {
		return 2;
	}

	@Override
	public Fetchable getFetchable(int position) {
		return switch ( position ) {
			case 0 -> getDiscriminatorPart();
			case 1 -> getKeyPart();
			default -> throw new IndexOutOfBoundsException( position );
		};
	}

	@Override
	public String getContainingTableExpression() {
		return getDiscriminatorPart().getContainingTableExpression();
	}

	@Override
	public int getJdbcTypeCount() {
		return getDiscriminatorPart().getJdbcTypeCount() + getKeyPart().getJdbcTypeCount();
	}

	@Override
	public JdbcMapping getJdbcMapping(final int index) {
		final int base = getDiscriminatorPart().getJdbcTypeCount();
		return index >= base
				? getKeyPart().getJdbcMapping( index - base )
				: getDiscriminatorPart().getJdbcMapping( index );
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return getDiscriminatorPart().getSelectable( columnIndex );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return associationMapping.getDiscriminatorPart().disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		associationMapping.getDiscriminatorPart().addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return associationMapping.getDiscriminatorPart().forEachDisassembledJdbcValue(
				value,
				offset,
				x,
				y,
				valuesConsumer,
				session
		);
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		return associationMapping.breakDownJdbcValues( offset, x, y, domainValue, valueConsumer, session );
	}

	@Override
	public <X, Y> int decompose(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		return associationMapping.decompose( offset, x, y, domainValue, valueConsumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = getDiscriminatorPart().forEachJdbcType( offset, action );
		return span + getKeyPart().forEachJdbcType( offset + span, action );
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
		final var joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );
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

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.LEFT;
	}

	@Override
	public String getSqlAliasStem() {
		return collectionDescriptor.getAttributeMapping().getSqlAliasStem();
	}
}
