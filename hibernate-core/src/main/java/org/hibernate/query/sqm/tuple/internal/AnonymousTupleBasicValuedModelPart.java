package org.hibernate.query.sqm.tuple.internal;

import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;

import jakarta.annotation.Nullable;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.OwnedValuedModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.sqm.spi.SqmExpressible;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.creation.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
public class AnonymousTupleBasicValuedModelPart implements OwnedValuedModelPart, MappingType, BasicValuedModelPart {

	private static final FetchOptions FETCH_OPTIONS = FetchOptions.valueOf( FetchTiming.IMMEDIATE, FetchStyle.JOIN );
	private final MappingType declaringType;
	private final String partName;
	private final SelectableMapping selectableMapping;
	private final SqmExpressible<?> expressible;
	private final int fetchableIndex;

	public AnonymousTupleBasicValuedModelPart(
			MappingType declaringType,
			String partName,
			String selectionExpression,
			SqmExpressible<?> expressible,
			JdbcMapping jdbcMapping,
			int fetchableIndex) {
		this(
				declaringType,
				partName,
				new SelectableMappingImpl(
						"",
						selectionExpression,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						false,
						true,
						false,
						false,
						false,
						false,
						jdbcMapping
				),
				expressible,
				fetchableIndex
		);
	}

	public AnonymousTupleBasicValuedModelPart(
			MappingType declaringType,
			String partName,
			SelectableMapping selectableMapping,
			SqmExpressible<?> expressible,
			int fetchableIndex) {
		this.declaringType = declaringType;
		this.partName = partName;
		this.selectableMapping = selectableMapping;
		this.expressible = expressible;
		this.fetchableIndex = fetchableIndex;
	}

	@Nonnull
	@Override
	public MappingType getPartMappingType() {
		return this;
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return expressible.getExpressibleJavaType();
	}

	@Nonnull
	@Override
	public JavaType<?> getMappedJavaType() {
		return expressible.getExpressibleJavaType();
	}

	@Nonnull
	@Override
	public MappingType getDeclaringType() {
		return declaringType;
	}

	@Nonnull
	@Override
	public String getPartName() {
		return partName;
	}

	@Nullable
	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Nullable
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return null;
	}

	@Nonnull
	@Override
	public String getSelectableName() {
		return selectableMapping.getSelectableName();
	}

	@Nonnull
	@Override
	public SelectablePath getSelectablePath() {
		return selectableMapping.getSelectablePath();
	}

	@Nullable
	@Override
	public String getWriteExpression() {
		return selectableMapping.getWriteExpression();
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return selectableMapping.getJdbcMapping();
	}

	@Nonnull
	@Override
	public String getContainingTableExpression() {
		return selectableMapping.getContainingTableExpression();
	}

	@Nonnull
	@Override
	public String getSelectionExpression() {
		return selectableMapping.getSelectionExpression();
	}

	@Override
	public @Nullable String getCustomReadExpression() {
		return selectableMapping.getCustomReadExpression();
	}

	@Override
	public @Nullable String getCustomWriteExpression() {
		return selectableMapping.getCustomWriteExpression();
	}

	@Override
	public boolean isFormula() {
		return selectableMapping.isFormula();
	}

	@Override
	public boolean isNullable() {
		return selectableMapping.isNullable();
	}

	@Override
	public boolean isInsertable() {
		return selectableMapping.isInsertable();
	}

	@Override
	public boolean isUpdateable() {
		return selectableMapping.isUpdateable();
	}

	@Override
	public boolean isPartitioned() {
		return selectableMapping.isPartitioned();
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return selectableMapping.isPartitioned();
	}

	@Override
	public @Nullable Long getLength() {
		return selectableMapping.getLength();
	}

	@Override
	public @Nullable Integer getArrayLength() {
		return selectableMapping.getArrayLength();
	}

	@Override
	public @Nullable Integer getPrecision() {
		return selectableMapping.getPrecision();
	}

	@Override
	public @Nullable Integer getScale() {
		return selectableMapping.getScale();
	}

	@Override
	public @Nullable Integer getTemporalPrecision() {
		return selectableMapping.getTemporalPrecision();
	}

	@Nonnull
	@Override
	public MappingType getMappedType() {
		return this;
	}

	@Override
	public String getFetchableName() {
		return partName;
	}

	@Override
	public int getFetchableKey() {
		return fetchableIndex;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return FETCH_OPTIONS;
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = resolveSqlSelection(
				navigablePath,
				tableGroup,
				null,
				creationState.getSqlAstCreationState()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				getJdbcMapping(),
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	private SqlSelection resolveSqlSelection(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			FetchParent fetchParent,
			SqlAstCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference(
				navigablePath,
				this,
				getContainingTableExpression()
		);
		final Expression expression = expressionResolver.resolveSqlExpression( tableReference, this );
		return expressionResolver.resolveSqlSelection(
				expression,
				getJdbcMapping().getJdbcJavaType(),
				fetchParent,
				creationState.getCreationContext().getTypeConfiguration()
		);
	}

	@Override
	public BasicFetch<?> generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
				fetchParent.getNavigablePath()
		);

		assert tableGroup != null;

		final SqlSelection sqlSelection = resolveSqlSelection(
				fetchablePath,
				tableGroup,
				fetchParent,
				creationState.getSqlAstCreationState()
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

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		resolveSqlSelection( navigablePath, tableGroup, null, creationState.getSqlAstCreationState() );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept(
				resolveSqlSelection( navigablePath, tableGroup, null, creationState.getSqlAstCreationState() ),
				getJdbcMapping()
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
		action.accept( offset, getJdbcMapping() );
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
		valueConsumer.consume( offset, x, y, domainValue, this );
		return getJdbcTypeCount();
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		if ( value == null ) {
			return;
		}
		cacheKey.addValue( value );
		cacheKey.addHashCode( ( (JavaType) getExpressibleJavaType() ).extractHashCode( value ) );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
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
	public int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(@Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
