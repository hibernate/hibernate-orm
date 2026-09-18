/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;

import jakarta.annotation.Nullable;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Nathan Xu
 */
public class EntityRowIdMappingImpl implements EntityRowIdMapping {
	private final String rowIdName;
	private final EntityMappingType declaringType;
	private final String tableExpression;
	private final BasicType<Object> rowIdType;

	public EntityRowIdMappingImpl(String rowIdName, String tableExpression, EntityMappingType declaringType) {
		this.rowIdName = rowIdName;
		this.tableExpression = tableExpression;
		this.declaringType = declaringType;
		final SessionFactoryImplementor factory = declaringType.getEntityPersister().getFactory();
		this.rowIdType = factory.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( Object.class, factory.getJdbcServices().getDialect().getRowIdSupport().sqlTypeCode() );
	}

	@Nonnull
	@Override
	public String getRowIdName() {
		return rowIdName;
	}

	@Nonnull
	@Override
	public MappingType getPartMappingType() {
		return rowIdType;
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return rowIdType.getJavaTypeDescriptor();
	}

	@Nonnull
	@Override
	public String getPartName() {
		return rowIdName;
	}

	@Nullable
	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Nonnull
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return declaringType;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		final var sqlAstCreationState = creationState.getSqlAstCreationState();

		final var sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final var columnTableReference = tableGroup.resolveTableReference( navigablePath, tableExpression );

		final var sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression( columnTableReference, this ),
				rowIdType.getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getTypeConfiguration()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				rowIdType,
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return getJdbcMapping();
	}

	@Nonnull
	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return getJdbcMapping();
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return rowIdType.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		rowIdType.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return rowIdType.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath, @Nonnull TableGroup tableGroup, @Nonnull DomainResultCreationState creationState) {
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		valueConsumer.consume( offset, x, y, domainValue, this );
		return getJdbcTypeCount();
	}

	@Nonnull
	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Nonnull
	@Override
	public String getSelectionExpression() {
		return rowIdName;
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
	public boolean isFormula() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean isInsertable() {
		return false;
	}

	@Override
	public boolean isUpdateable() {
		return false;
	}

	@Override
	public boolean isPartitioned() {
		return false;
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return rowIdType.getJdbcMapping();
	}

	@Nonnull
	@Override
	public MappingType getMappedType() {
		return rowIdType;
	}

	@Override
	public String getFetchableName() {
		return rowIdName;
	}

	@Override
	public int getFetchableKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}
}
