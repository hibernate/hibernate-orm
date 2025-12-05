/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Acts as a ModelPart for the discriminator portion of an any-valued mapping
 *
 * @author Steve Ebersole
 */
public class AnyDiscriminatorPart implements DiscriminatorMapping, FetchOptions {
	public static final String ROLE_NAME = EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME;

	private final NavigableRole navigableRole;
	private final DiscriminatedAssociationModelPart declaringType;

	private final String table;
	private final String column;
	private final SelectablePath selectablePath;
	private final @Nullable String customReadExpression;
	private final @Nullable String customWriteExpression;
	private final @Nullable String columnDefinition;
	private final @Nullable Long length;
	private final @Nullable Integer arrayLength;
	private final @Nullable Integer precision;
	private final @Nullable Integer scale;

	private final boolean insertable;
	private final boolean updateable;
	private final boolean partitioned;

	private final BasicType<?> underlyingJdbcMapping;
	private final DiscriminatorConverter<?,?> valueConverter;

	@Deprecated(forRemoval = true, since = "7.2")
	public AnyDiscriminatorPart(
			NavigableRole partRole,
			DiscriminatedAssociationModelPart declaringType,
			String table,
			String column,
			SelectablePath selectablePath,
			String customReadExpression,
			String customWriteExpression,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			BasicType<?> underlyingJdbcMapping,
			Map<Object, String> valueToEntityNameMap,
			ImplicitDiscriminatorStrategy implicitValueStrategy,
			MappingMetamodelImplementor mappingMetamodel) {
		this(
				partRole,
				declaringType,
				table,
				column,
				selectablePath,
				customReadExpression,
				customWriteExpression,
				columnDefinition,
				length,
				null,
				precision,
				scale,
				insertable,
				updateable,
				partitioned,
				underlyingJdbcMapping,
				valueToEntityNameMap,
				implicitValueStrategy,
				mappingMetamodel
		);
	}

	public AnyDiscriminatorPart(
			NavigableRole partRole,
			DiscriminatedAssociationModelPart declaringType,
			String table,
			String column,
			SelectablePath selectablePath,
			@Nullable String customReadExpression,
			@Nullable String customWriteExpression,
			@Nullable String columnDefinition,
			@Nullable Long length,
			@Nullable Integer arrayLength,
			@Nullable Integer precision,
			@Nullable Integer scale,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			BasicType<?> underlyingJdbcMapping,
			Map<Object,String> valueToEntityNameMap,
			ImplicitDiscriminatorStrategy implicitValueStrategy,
			MappingMetamodelImplementor mappingMetamodel) {
		this.navigableRole = partRole;
		this.declaringType = declaringType;
		this.table = table;
		this.column = column;
		this.selectablePath = selectablePath;
		this.customReadExpression = customReadExpression;
		this.customWriteExpression = customWriteExpression;
		this.columnDefinition = columnDefinition;
		this.length = length;
		this.arrayLength = arrayLength;
		this.precision = precision;
		this.scale = scale;
		this.insertable = insertable;
		this.updateable = updateable;
		this.partitioned = partitioned;

		this.underlyingJdbcMapping = underlyingJdbcMapping;
		this.valueConverter = determineDiscriminatorConverter(
				partRole,
				underlyingJdbcMapping,
				valueToEntityNameMap,
				implicitValueStrategy,
				mappingMetamodel
		);
	}

	public static DiscriminatorConverter<?, ?> determineDiscriminatorConverter(
			NavigableRole partRole,
			BasicType<?> underlyingJdbcMapping,
			Map<Object, String> valueToEntityNameMap,
			ImplicitDiscriminatorStrategy implicitValueStrategy,
			MappingMetamodelImplementor mappingMetamodel) {
		return new UnifiedAnyDiscriminatorConverter<>(
				partRole,
				ClassJavaType.INSTANCE,
				underlyingJdbcMapping.getJavaTypeDescriptor(),
				valueToEntityNameMap,
				implicitValueStrategy,
				mappingMetamodel
		);
	}

	public DiscriminatorConverter<?,?> getValueConverter() {
		return valueConverter;
	}

	public JdbcMapping jdbcMapping() {
		return underlyingJdbcMapping;
	}

	@Override
	public String getContainingTableExpression() {
		return table;
	}

	@Override
	public String getSelectionExpression() {
		return column;
	}

	@Override
	public String getSelectableName() {
		return selectablePath.getSelectableName();
	}

	@Override
	public SelectablePath getSelectablePath() {
		return selectablePath;
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
		return insertable;
	}

	@Override
	public boolean isUpdateable() {
		return updateable;
	}

	@Override
	public boolean isPartitioned() {
		return partitioned;
	}

	@Override
	public @Nullable String getCustomReadExpression() {
		return customReadExpression;
	}

	@Override
	public @Nullable String getCustomWriteExpression() {
		return customWriteExpression;
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
	public @Nullable Integer getTemporalPrecision() {
		return null;
	}

	@Override
	public @Nullable Integer getScale() {
		return scale;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping();
	}

	@Override
	public JavaType<?> getJavaType() {
		return jdbcMapping().getMappedJavaType();
	}

	@Override
	public String getPartName() {
		return ROLE_NAME;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public JdbcMapping getUnderlyingJdbcMapping() {
		return underlyingJdbcMapping;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return underlyingJdbcMapping.disassemble( value, session, value );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		cacheKey.addValue( underlyingJdbcMapping.disassemble( value, session, value ) );
		cacheKey.addHashCode( underlyingJdbcMapping.getHashCode( value ) );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
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
		return declaringType.findContainingEntityMapping();
	}

	@Override
	public MappingType getMappedType() {
		return null;
	}

	@Override
	public String getFetchableName() {
		return getPartName();
	}

	@Override
	public int getFetchableKey() {
		return 0;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return 1;
	}

	@Override
	public BasicFetch<?> generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final var sqlAstCreationState = creationState.getSqlAstCreationState();
		final var fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
		final var sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		final var tableGroup = fromClauseAccess.getTableGroup( fetchablePath.getParent().getParent() );
		final var tableReference = tableGroup.resolveTableReference( fetchablePath, table );
		final var columnReference = sqlExpressionResolver.resolveSqlExpression(
				tableReference,
				this
		);
		final var sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				columnReference,
				jdbcMapping().getJdbcJavaType(),
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
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final var sqlSelection = resolveSqlSelection( navigablePath, tableGroup, creationState );
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				jdbcMapping(),
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public Expression resolveSqlExpression(
			NavigablePath navigablePath,
			JdbcMapping jdbcMappingToUse,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		var tableReference = tableGroup.resolveTableReference(
				navigablePath,
				this,
				getContainingTableExpression()
		);
		return creationState.getSqlExpressionResolver()
				.resolveSqlExpression( tableReference, this );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		resolveSqlSelection( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept( resolveSqlSelection( navigablePath, tableGroup, creationState ), getJdbcMapping() );
	}

	private SqlSelection resolveSqlSelection(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		final var sqlAstCreationState = creationState.getSqlAstCreationState();
		return sqlAstCreationState.getSqlExpressionResolver().resolveSqlSelection(
				resolveSqlExpression( navigablePath, null, tableGroup, sqlAstCreationState ),
				jdbcMapping().getJdbcJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
		);
	}
}
