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
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class BasicAttributeMapping
		extends AbstractSingularAttributeMapping
		implements SingularAttributeMapping, BasicValuedModelPart {
	private final NavigableRole navigableRole;

	private final String tableExpression;
	private final String mappedColumnExpression;
	private final Integer temporalPrecision;
	private final SelectablePath selectablePath;
	private final boolean isFormula;
	private final @Nullable String customReadExpression;
	private final @Nullable String customWriteExpression;
	private final @Nullable String columnDefinition;
	private final @Nullable Long length;
	private final @Nullable Integer arrayLength;
	private final @Nullable Integer precision;
	private final @Nullable Integer scale;

	private final JdbcMapping jdbcMapping;
	private final boolean isLob;
	private final boolean nullable;
	private final boolean insertable;
	private final boolean updateable;
	private final boolean partitioned;
	private final boolean isLazy;

	private final JavaType domainTypeDescriptor;

	@Deprecated(forRemoval = true, since = "7.2")
	public BasicAttributeMapping(
			String attributeName,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			AttributeMetadata attributeMetadata,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			String tableExpression,
			String mappedColumnExpression,
			SelectablePath selectablePath,
			boolean isFormula,
			@Nullable String customReadExpression,
			@Nullable String customWriteExpression,
			@Nullable String columnDefinition,
			@Nullable Long length,
			@Nullable Integer precision,
			@Nullable Integer scale,
			@Nullable Integer temporalPrecision,
			boolean isLob,
			boolean nullable,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			JdbcMapping jdbcMapping,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		this(
				attributeName,
				navigableRole,
				stateArrayPosition,
				fetchableIndex,
				attributeMetadata,
				mappedFetchTiming,
				mappedFetchStyle,
				tableExpression,
				mappedColumnExpression,
				selectablePath,
				isFormula,
				customReadExpression,
				customWriteExpression,
				columnDefinition,
				length,
				null,
				precision,
				scale,
				temporalPrecision,
				isLob,
				nullable,
				insertable,
				updateable,
				partitioned,
				jdbcMapping,
				declaringType,
				propertyAccess );
	}

	public BasicAttributeMapping(
			String attributeName,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			AttributeMetadata attributeMetadata,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			String tableExpression,
			String mappedColumnExpression,
			SelectablePath selectablePath,
			boolean isFormula,
			@Nullable String customReadExpression,
			@Nullable String customWriteExpression,
			@Nullable String columnDefinition,
			@Nullable Long length,
			@Nullable Integer arrayLength,
			@Nullable Integer precision,
			@Nullable Integer scale,
			@Nullable Integer temporalPrecision,
			boolean isLob,
			boolean nullable,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			JdbcMapping jdbcMapping,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				attributeName,
				stateArrayPosition,
				fetchableIndex,
				attributeMetadata,
				mappedFetchTiming,
				mappedFetchStyle,
				declaringType,
				propertyAccess
		);
		this.navigableRole = navigableRole;
		this.tableExpression = tableExpression;
		this.mappedColumnExpression = mappedColumnExpression;
		this.temporalPrecision = temporalPrecision;
		this.selectablePath = selectablePath == null
				? new SelectablePath( mappedColumnExpression )
				: selectablePath;
		this.isFormula = isFormula;
		this.columnDefinition = columnDefinition;
		this.length = length;
		this.arrayLength = arrayLength;
		this.precision = precision;
		this.scale = scale;
		this.isLob = isLob;
		this.nullable = nullable;
		this.insertable = insertable;
		this.updateable = updateable;
		this.partitioned = partitioned;
		this.jdbcMapping = jdbcMapping;
		this.domainTypeDescriptor = jdbcMapping.getJavaTypeDescriptor();

		this.customReadExpression = customReadExpression;

		if ( isFormula ) {
			this.customWriteExpression = null;
		}
		else {
			this.customWriteExpression = customWriteExpression;
		}
		this.isLazy = navigableRole.getParent().getParent() == null && declaringType.findContainingEntityMapping()
				.getEntityPersister()
				.getBytecodeEnhancementMetadata()
				.getLazyAttributesMetadata()
				.isLazyAttribute( attributeName );
	}

	public static BasicAttributeMapping withSelectableMapping(
			ManagedMappingType declaringType,
			BasicValuedModelPart original,
			PropertyAccess propertyAccess,
			boolean insertable,
			boolean updateable,
			SelectableMapping selectableMapping) {
		String attributeName = null;
		int stateArrayPosition = 0;
		AttributeMetadata attributeMetadata;
		if ( original instanceof SingleAttributeIdentifierMapping mapping ) {
			attributeName = mapping.getAttributeName();
			attributeMetadata = new SimpleAttributeMetadata(
					propertyAccess,
					mapping.getExpressibleJavaType().getMutabilityPlan(),
					selectableMapping.isNullable(),
					insertable,
					updateable,
					false,
					true
			);
		}
		else if ( original instanceof SingularAttributeMapping mapping ) {
			attributeName = mapping.getAttributeName();
			stateArrayPosition = mapping.getStateArrayPosition();
			attributeMetadata = mapping.getAttributeMetadata();
		}
		else {
			attributeMetadata = null;
		}
		return new BasicAttributeMapping(
				attributeName,
				original.getNavigableRole(),
				stateArrayPosition,
				original.getFetchableKey(),
				attributeMetadata,
				FetchTiming.IMMEDIATE,
				FetchStyle.JOIN,
				selectableMapping.getContainingTableExpression(),
				selectableMapping.getSelectionExpression(),
				selectableMapping.getSelectablePath(),
				selectableMapping.isFormula(),
				selectableMapping.getCustomReadExpression(),
				selectableMapping.getCustomWriteExpression(),
				selectableMapping.getColumnDefinition(),
				selectableMapping.getLength(),
				selectableMapping.getArrayLength(),
				selectableMapping.getPrecision(),
				selectableMapping.getScale(),
				selectableMapping.getTemporalPrecision(),
				selectableMapping.isLob(),
				selectableMapping.isNullable(),
				insertable,
				updateable,
				selectableMapping.isPartitioned(),
				original.getJdbcMapping(),
				declaringType,
				propertyAccess
		);
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public MappingType getMappedType() {
		return getJdbcMapping();
	}

	@Override
	public JavaType<?> getJavaType() {
		return domainTypeDescriptor;
	}

	@Override
	public String getSelectionExpression() {
		return mappedColumnExpression;
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
	public boolean isLob() {
		return isLob;
	}

	@Override
	public boolean isFormula() {
		return isFormula;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	public boolean isLazy() {
		return isLazy;
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
	public String getWriteExpression() {
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
		return temporalPrecision;
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
	public String toString() {
		return "BasicAttributeMapping(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = resolveSqlSelection( navigablePath, tableGroup, null, creationState );

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				jdbcMapping,
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	private SqlSelection resolveSqlSelection(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference(
				navigablePath,
				this,
				getContainingTableExpression()
		);

		return expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						tableReference,
						this
				),
				jdbcMapping.getJdbcJavaType(),
				fetchParent,
				creationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		resolveSqlSelection( navigablePath, tableGroup, null, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept( resolveSqlSelection( navigablePath, tableGroup, null, creationState ), getJdbcMapping() );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final int valuesArrayPosition;
		boolean coerceResultType = false;
		final SqlSelection sqlSelection;
		if ( fetchTiming == FetchTiming.DELAYED && isLazy ) {
			// Lazy property. A valuesArrayPosition of -1 will lead to
			// returning a domain result assembler that returns LazyPropertyInitializer.UNFETCHED_PROPERTY
			valuesArrayPosition = -1;
			sqlSelection = null;
		}
		else {
			final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
			final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
					fetchParent.getNavigablePath()
			);

			assert tableGroup != null;

			sqlSelection = resolveSqlSelection(
					fetchablePath,
					tableGroup,
					fetchParent,
					creationState
			);
			valuesArrayPosition = sqlSelection.getValuesArrayPosition();
			if ( sqlSelection.getExpressionType() != null) {
				// if the expression type is different that the expected type coerce the value
				coerceResultType = sqlSelection.getExpressionType().getSingleJdbcMapping().getJdbcJavaType() != getJdbcMapping().getJdbcJavaType();
			}
		}

		return new BasicFetch<>(
				valuesArrayPosition,
				fetchParent,
				fetchablePath,
				this,
				getJdbcMapping().getValueConverter(),
				fetchTiming,
				true,
				creationState,
				coerceResultType,
				sqlSelection != null && !sqlSelection.isVirtual()
		);
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return getJdbcTypeCount();
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		valueConsumer.consume( offset, x, y, disassemble( domainValue, session ), this );
		return getJdbcTypeCount();
	}
}
