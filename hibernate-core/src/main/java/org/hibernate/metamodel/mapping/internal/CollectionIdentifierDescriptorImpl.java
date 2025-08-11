/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class CollectionIdentifierDescriptorImpl implements CollectionIdentifierDescriptor, FetchOptions {
	private final NavigableRole navigableRole;
	private final CollectionPersister collectionDescriptor;
	private final String containingTableName;
	private final String columnName;
	private final BasicType<?> type;

	public CollectionIdentifierDescriptorImpl(
			CollectionPersister collectionDescriptor,
			String containingTableName,
			String columnName,
			BasicType<?> type) {
		this.navigableRole = collectionDescriptor.getNavigableRole().append( Nature.ID.getName() );
		this.collectionDescriptor = collectionDescriptor;
		this.containingTableName = containingTableName;
		this.columnName = columnName;
		this.type = type;
	}

	@Override
	public Nature getNature() {
		return Nature.ID;
	}

	@Override
	public PluralAttributeMapping getCollectionAttribute() {
		return collectionDescriptor.getAttributeMapping();
	}

	@Override
	public String getContainingTableExpression() {
		return containingTableName;
	}

	@Override
	public String getSelectionExpression() {
		return columnName;
	}

	@Override
	public boolean isFormula() {
		return false;
	}

	@Override
	public boolean isInsertable() {
		return true;
	}

	@Override
	public boolean isUpdateable() {
		return false;
	}

	@Override
	public boolean isPartitioned() {
		return false;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public @Nullable String getCustomReadExpression() {
		return null;
	}

	@Override
	public @Nullable String getCustomWriteExpression() {
		return null;
	}

	@Override
	public @Nullable String getColumnDefinition() {
		return null;
	}

	@Override
	public @Nullable Long getLength() {
		return null;
	}

	@Override
	public @Nullable Integer getArrayLength() {
		return null;
	}

	@Override
	public @Nullable Integer getPrecision() {
		return null;
	}

	@Override
	public @Nullable Integer getScale() {
		return null;
	}

	@Override
	public @Nullable Integer getTemporalPrecision() {
		return null;
	}

	@Override
	public MappingType getPartMappingType() {
		return type;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return type;
	}

	@Override
	public MappingType getMappedType() {
		return type;
	}

	@Override
	public JavaType<?> getJavaType() {
		return getMappedType().getMappedJavaType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return collectionDescriptor.getAttributeMapping().createDomainResult(
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
		collectionDescriptor.getAttributeMapping().applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		collectionDescriptor.getAttributeMapping().applySqlSelections(
				navigablePath,
				tableGroup,
				creationState,
				selectionConsumer
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
		valueConsumer.consume( offset, x, y, domainValue, this );
		return getJdbcTypeCount();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return collectionDescriptor.getAttributeMapping().findContainingEntityMapping();
	}

	@Override
	public String getFetchableName() {
		return null;
	}

	@Override
	public int getFetchableKey() {
		return -1;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		// get the collection TableGroup
		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		final TableGroup tableGroup = fromClauseAccess.getTableGroup( fetchablePath.getParent() );

		final SqlAstCreationState astCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = astCreationState.getSqlExpressionResolver();

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						tableGroup.getPrimaryTableReference(),
						this
				),
				type.getJdbcJavaType(),
				fetchParent,
				astCreationState.getCreationContext().getTypeConfiguration()
		);

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				FetchTiming.IMMEDIATE,
				creationState,
				!sqlSelection.isVirtual()
		);
	}

	public DomainResult<?> createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		final SqlAstCreationState astCreationState = creationState.getSqlAstCreationState();
		final SqlAstCreationContext astCreationContext = astCreationState.getCreationContext();
		final SqlExpressionResolver sqlExpressionResolver = astCreationState.getSqlExpressionResolver();

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						tableGroup.getPrimaryTableReference(),
						this
				),
				type.getJdbcJavaType(),
				null,
				astCreationContext.getTypeConfiguration()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				null,
				type,
				collectionPath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + collectionDescriptor.getRole() + ")";
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return type.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		type.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return type.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}
}
