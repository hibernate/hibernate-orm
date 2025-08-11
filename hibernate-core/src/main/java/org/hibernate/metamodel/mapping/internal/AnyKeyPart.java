/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Acts as a ModelPart for the key portion of an any-valued mapping
 *
 * @author Steve Ebersole
 */
public class AnyKeyPart implements BasicValuedModelPart, FetchOptions {
	public static final String KEY_NAME = "{key}";

	private final NavigableRole navigableRole;
	private final String table;
	private final String column;
	private final SelectablePath selectablePath;
	private final DiscriminatedAssociationModelPart anyPart;
	private final @Nullable String customReadExpression;
	private final @Nullable String customWriteExpression;
	private final @Nullable String columnDefinition;
	private final @Nullable Long length;
	private final @Nullable Integer arrayLength;
	private final @Nullable Integer precision;
	private final @Nullable Integer scale;
	private final boolean nullable;
	private final boolean insertable;
	private final boolean updateable;
	private final boolean partitioned;
	private final JdbcMapping jdbcMapping;

	public AnyKeyPart(
			NavigableRole navigableRole,
			DiscriminatedAssociationModelPart anyPart,
			String table,
			String column,
			SelectablePath selectablePath,
			String customReadExpression,
			String customWriteExpression,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			boolean nullable,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			JdbcMapping jdbcMapping) {
		this(
				navigableRole,
				anyPart,
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
				nullable,
				insertable,
				updateable,
				partitioned,
				jdbcMapping
		);
	}

	public AnyKeyPart(
			NavigableRole navigableRole,
			DiscriminatedAssociationModelPart anyPart,
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
			boolean nullable,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			JdbcMapping jdbcMapping) {
		this.navigableRole = navigableRole;
		this.table = table;
		this.column = column;
		this.selectablePath = selectablePath;
		this.anyPart = anyPart;
		this.customReadExpression = customReadExpression;
		this.customWriteExpression = customWriteExpression;
		this.columnDefinition = columnDefinition;
		this.length = length;
		this.arrayLength = arrayLength;
		this.precision = precision;
		this.scale = scale;
		this.nullable = nullable;
		this.insertable = insertable;
		this.updateable = updateable;
		this.partitioned = partitioned;
		this.jdbcMapping = jdbcMapping;
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
		return nullable;
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
	public @Nullable Integer getScale() {
		return scale;
	}

	@Override
	public @Nullable Integer getTemporalPrecision() {
		return null;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public JavaType<?> getJavaType() {
		return jdbcMapping.getMappedJavaType();
	}

	@Override
	public String getPartName() {
		return KEY_NAME;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return anyPart.findContainingEntityMapping();
	}

	@Override
	public MappingType getMappedType() {
		return jdbcMapping;
	}

	@Override
	public String getFetchableName() {
		return getPartName();
	}

	@Override
	public int getFetchableKey() {
		return 1;
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
		final FromClauseAccess fromClauseAccess = creationState
				.getSqlAstCreationState()
				.getFromClauseAccess();
		final SqlExpressionResolver sqlExpressionResolver = creationState
				.getSqlAstCreationState()
				.getSqlExpressionResolver();

		final TableGroup tableGroup = fromClauseAccess.getTableGroup( fetchParent.getNavigablePath().getParent() );
		final TableReference tableReference = tableGroup.resolveTableReference( fetchablePath, table );

		final Expression columnReference = sqlExpressionResolver.resolveSqlExpression(
				tableReference,
				this
		);

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				columnReference,
				jdbcMapping.getJdbcJavaType(),
				fetchParent,
				creationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
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
		return FetchStyle.SELECT;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
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
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return jdbcMapping;
	}

	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, jdbcMapping );
		return 1;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = resolveSqlSelection( navigablePath, tableGroup, creationState );
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				jdbcMapping,
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
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
		final TableReference tableReference = tableGroup.resolveTableReference(
				navigablePath,
				this,
				getContainingTableExpression()
		);
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState()
				.getSqlExpressionResolver();
		return expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						tableReference,
						this
				),
				jdbcMapping.getJdbcJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
		);
	}
}
