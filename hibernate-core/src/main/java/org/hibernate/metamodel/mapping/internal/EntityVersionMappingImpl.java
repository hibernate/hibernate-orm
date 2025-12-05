/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.UnsavedValueFactory;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.VersionJavaType;

/**
 * @author Steve Ebersole
 */
public class EntityVersionMappingImpl implements EntityVersionMapping, FetchOptions {
	private final String attributeName;
	private final EntityMappingType declaringType;

	private final String columnTableExpression;
	private final String columnExpression;
	private final @Nullable String columnDefinition;
	private final @Nullable Long length;
	private final @Nullable Integer arrayLength;
	private final @Nullable Integer precision;
	private final @Nullable Integer scale;
	private final @Nullable Integer temporalPrecision;

	private final BasicType<?> versionBasicType;

	private final VersionValue unsavedValueStrategy;

	@Deprecated(forRemoval = true, since = "7.2")
	public EntityVersionMappingImpl(
			RootClass bootEntityDescriptor,
			Supplier<?> templateInstanceAccess,
			String attributeName,
			String columnTableExpression,
			String columnExpression,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			Integer temporalPrecision,
			BasicType<?> versionBasicType,
			EntityMappingType declaringType) {
		this(
				bootEntityDescriptor,
				templateInstanceAccess,
				attributeName,
				columnTableExpression,
				columnExpression,
				columnDefinition,
				length,
				null,
				precision,
				scale,
				temporalPrecision,
				versionBasicType,
				declaringType
		);
	}

	public EntityVersionMappingImpl(
			RootClass bootEntityDescriptor,
			Supplier<?> templateInstanceAccess,
			String attributeName,
			String columnTableExpression,
			String columnExpression,
			@Nullable String columnDefinition,
			@Nullable Long length,
			@Nullable Integer arrayLength,
			@Nullable Integer precision,
			@Nullable Integer scale,
			@Nullable Integer temporalPrecision,
			BasicType<?> versionBasicType,
			EntityMappingType declaringType) {
		this.attributeName = attributeName;
		this.columnDefinition = columnDefinition;
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
						.getGetter(),
				templateInstanceAccess
		);
	}

	@Override
	public BasicAttributeMapping getVersionAttribute() {
		return (BasicAttributeMapping) declaringType.findAttributeMapping( attributeName );
	}

	@Override
	public VersionValue getUnsavedStrategy() {
		return unsavedValueStrategy;
	}

	@Override
	public String getContainingTableExpression() {
		return columnTableExpression;
	}

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
	public @Nullable String getColumnDefinition() {
		return columnDefinition;
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

	@Override
	public MappingType getPartMappingType() {
		return versionBasicType;
	}

	@Override
	public MappingType getMappedType() {
		return versionBasicType;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return versionBasicType.getJdbcMapping();
	}

	@Override
	public VersionJavaType<?> getJavaType() {
		return (VersionJavaType<?>) versionBasicType.getJavaTypeDescriptor();
	}

	@Override
	public String getPartName() {
		return attributeName;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return getVersionAttribute().getNavigableRole();
	}

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
				creationState,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
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
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		resolveSqlSelection( tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept( resolveSqlSelection( tableGroup, creationState ), getJdbcMapping() );
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

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return versionBasicType.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		versionBasicType.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return versionBasicType.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
