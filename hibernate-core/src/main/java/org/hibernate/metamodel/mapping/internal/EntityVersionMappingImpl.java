/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.UnsavedValueFactory;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.creation.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.VersionJavaType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class EntityVersionMappingImpl implements EntityVersionMapping, FetchOptions {
	private final String attributeName;
	private final EntityMappingType declaringType;

	private final String columnTableExpression;
	private final String columnExpression;
	private final @Nullable Long length;
	private final @Nullable Integer arrayLength;
	private final @Nullable Integer precision;
	private final @Nullable Integer scale;
	private final @Nullable Integer temporalPrecision;

	private final BasicType<?> versionBasicType;

	private final VersionValue unsavedValueStrategy;

	public EntityVersionMappingImpl(
			RootClass bootEntityDescriptor,
			Supplier<?> templateInstanceAccess,
			String attributeName,
			String columnTableExpression,
			String columnExpression,
			@Nullable Long length,
			@Nullable Integer arrayLength,
			@Nullable Integer precision,
			@Nullable Integer scale,
			@Nullable Integer temporalPrecision,
			BasicType<?> versionBasicType,
			EntityMappingType declaringType) {
		this.attributeName = attributeName;
		this.length = length;
		this.arrayLength = arrayLength;
		this.precision = precision;
		this.scale = scale;
		this.temporalPrecision = temporalPrecision;
		this.declaringType = declaringType;

		this.columnTableExpression = columnTableExpression;
		this.columnExpression = columnExpression;

		this.versionBasicType = versionBasicType;

		unsavedValueStrategy = UnsavedValueFactory.getUnsavedVersionValue(
				(KeyValue) bootEntityDescriptor.getVersion().getValue(),
				(VersionJavaType<?>) versionBasicType.getJavaTypeDescriptor(),
				declaringType.getRepresentationStrategy()
						.resolvePropertyAccess( bootEntityDescriptor.getVersion() )
						.getPropertyValueAccessor(),
				templateInstanceAccess
		);
	}

	@Nonnull
	@Override
	public BasicAttributeMapping getVersionAttribute() {
		return (BasicAttributeMapping) castNonNull( declaringType.findAttributeMapping( attributeName ) );
	}

	@Nonnull
	@Override
	public VersionValue getUnsavedStrategy() {
		return unsavedValueStrategy;
	}

	@Nonnull
	@Override
	public String getContainingTableExpression() {
		return columnTableExpression;
	}

	@Nonnull
	@Override
	public String getSelectionExpression() {
		return columnExpression;
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
		return true;
	}

	@Override
	public boolean isUpdateable() {
		return true;
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
	public @Nullable String getCustomReadExpression() {
		return null;
	}

	@Override
	public @Nullable String getCustomWriteExpression() {
		return null;
	}

	@Override
	public @Nullable Long getLength() {
		return length;
	}

	@Override
	public @Nullable Integer getArrayLength() {
		return arrayLength;
	}

	@Override
	public @Nullable Integer getPrecision() {
		return precision;
	}

	@Override
	public @Nullable Integer getScale() {
		return scale;
	}

	@Override
	public @Nullable Integer getTemporalPrecision() {
		return temporalPrecision;
	}

	@Nonnull
	@Override
	public MappingType getPartMappingType() {
		return versionBasicType;
	}

	@Nonnull
	@Override
	public MappingType getMappedType() {
		return versionBasicType;
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return versionBasicType.getJdbcMapping();
	}

	@Nonnull
	@Override
	public VersionJavaType<?> getJavaType() {
		return (VersionJavaType<?>) versionBasicType.getJavaTypeDescriptor();
	}

	@Nonnull
	@Override
	public String getPartName() {
		return attributeName;
	}

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return getVersionAttribute().getNavigableRole();
	}

	@Nonnull
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return declaringType;
	}

	@Override
	public String getFetchableName() {
		return attributeName;
	}

	@Override
	public int getFetchableKey() {
		return getVersionAttribute().getFetchableKey();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().findTableGroup( fetchParent.getNavigablePath() );

		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference columnTableReference = tableGroup.resolveTableReference( fetchablePath, columnTableExpression );

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression( columnTableReference, this ),
				versionBasicType.getJdbcJavaType(),
				fetchParent,
				sqlAstCreationState.getCreationContext().getTypeConfiguration()
		);

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				fetchTiming,
				!sqlSelection.isVirtual()
		);
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				versionBasicType,
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath, @Nonnull TableGroup tableGroup, @Nonnull DomainResultCreationState creationState) {
		resolveSqlSelection( tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept( resolveSqlSelection( tableGroup, creationState ), getJdbcMapping() );
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

	private SqlSelection resolveSqlSelection(TableGroup tableGroup, DomainResultCreationState creationState) {
		final var sqlAstCreationState = creationState.getSqlAstCreationState();

		final var sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final var columnTableReference = tableGroup.resolveTableReference(
				tableGroup.getNavigablePath()
						.append( getNavigableRole().getNavigableName() ),
				columnTableExpression
		);

		return sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression( columnTableReference, this ),
				versionBasicType.getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getTypeConfiguration()
		);
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return versionBasicType.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		versionBasicType.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return versionBasicType.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
