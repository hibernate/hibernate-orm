/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;

import jakarta.annotation.Nullable;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

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
	@Nullable private final Integer temporalPrecision;
	private final SelectablePath selectablePath;
	private final boolean isFormula;
	private final @Nullable String customReadExpression;
	private final @Nullable String customWriteExpression;
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

	public BasicAttributeMapping(
			@Nullable String attributeName,
			@Nonnull NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			@Nullable AttributeMetadata attributeMetadata,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			String tableExpression,
			String mappedColumnExpression,
			@Nullable SelectablePath selectablePath,
			boolean isFormula,
			@Nullable String customReadExpression,
			@Nullable String customWriteExpression,
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
			@Nullable ManagedMappingType declaringType,
			@Nullable PropertyAccess propertyAccess) {
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
		this.selectablePath =
				selectablePath == null
						? new SelectablePath( isFormula ? castNonNull( attributeName ) : mappedColumnExpression )
						: selectablePath;
		this.isFormula = isFormula;
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
		this.customWriteExpression = isFormula ? null : customWriteExpression;
		this.isLazy =
				declaringType != null
					&& navigableRole.getParent().getParent() == null
					&& castNonNull( declaringType.findContainingEntityMapping() )
							.getEntityPersister()
							.getBytecodeEnhancementMetadata()
							.getLazyAttributesMetadata()
							.isLazyAttribute( attributeName );
	}

	public static BasicAttributeMapping withSelectableMapping(
			@Nullable ManagedMappingType declaringType,
			BasicValuedModelPart original,
			@Nullable PropertyAccess propertyAccess,
			boolean insertable,
			boolean updateable,
			SelectableMapping selectableMapping) {
		String attributeName = null;
		int stateArrayPosition = 0;
		AttributeMetadata attributeMetadata;
		if ( original instanceof SingleAttributeIdentifierMapping mapping ) {
			attributeName = mapping.getAttributeName();
			attributeMetadata = new SimpleAttributeMetadata(
					castNonNull( propertyAccess ),
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
				castNonNull( original.getNavigableRole() ),
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

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Nonnull
	@Override
	public MappingType getMappedType() {
		return getJdbcMapping();
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return domainTypeDescriptor;
	}

	@Nonnull
	@Override
	public String getSelectionExpression() {
		return mappedColumnExpression;
	}

	@Nonnull
	@Override
	public String getSelectableName() {
		return selectablePath.getSelectableName();
	}

	@Nonnull
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

	@Nullable
	@Override
	public String getWriteExpression() {
		return customWriteExpression;
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
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String toString() {
		return "BasicAttributeMapping(" + navigableRole + ")@"
			+ System.identityHashCode( this );
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		final var sqlSelection =
				resolveSqlSelection( navigablePath, tableGroup, null, creationState );
		return new BasicResult<>(
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
			@Nullable FetchParent fetchParent,
			DomainResultCreationState creationState) {
		final var sqlAstCreationState = creationState.getSqlAstCreationState();
		final var expressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final var tableReference =
				tableGroup.resolveTableReference( navigablePath, this,
						getContainingTableExpression() );
		return expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression( tableReference, this ),
				jdbcMapping.getJdbcJavaType(),
				fetchParent,
				sqlAstCreationState.getCreationContext().getTypeConfiguration()
		);
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		resolveSqlSelection( navigablePath, tableGroup, null, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept( resolveSqlSelection( navigablePath, tableGroup, null, creationState ),
				getJdbcMapping() );
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
			final var sqlAstCreationState = creationState.getSqlAstCreationState();
			final var tableGroup =
					sqlAstCreationState.getFromClauseAccess()
							.getTableGroup( fetchParent.getNavigablePath() );
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
				coerceResultType,
				sqlSelection != null && !sqlSelection.isVirtual()
		);
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return getJdbcTypeCount();
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		valueConsumer.consume( offset, x, y, disassemble( domainValue, session ), this );
		return getJdbcTypeCount();
	}
}
