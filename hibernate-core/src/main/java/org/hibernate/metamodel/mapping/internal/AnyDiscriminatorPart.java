package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
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
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
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
	private final @Nullable Long length;
	private final @Nullable Integer arrayLength;
	private final @Nullable Integer precision;
	private final @Nullable Integer scale;

	private final boolean insertable;
	private final boolean updateable;
	private final boolean partitioned;

	private final BasicType<?> underlyingJdbcMapping;
	private final DiscriminatorConverter<?,?> valueConverter;

	public AnyDiscriminatorPart(
			NavigableRole partRole,
			DiscriminatedAssociationModelPart declaringType,
			String table,
			String column,
			SelectablePath selectablePath,
			@Nullable String customReadExpression,
			@Nullable String customWriteExpression,
			@Nullable Long length,
			@Nullable Integer arrayLength,
			@Nullable Integer precision,
			@Nullable Integer scale,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			BasicType<?> underlyingJdbcMapping,
			Map<DiscriminatorValue,String> valueToEntityNameMap,
			ImplicitDiscriminatorStrategy implicitValueStrategy,
			MappingMetamodelImplementor mappingMetamodel) {
		this.navigableRole = partRole;
		this.declaringType = declaringType;
		this.table = table;
		this.column = column;
		this.selectablePath = selectablePath;
		this.customReadExpression = customReadExpression;
		this.customWriteExpression = customWriteExpression;
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
			Map<DiscriminatorValue, String> valueToEntityNameMap,
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

	@Nonnull
	public DiscriminatorConverter<?,?> getValueConverter() {
		return valueConverter;
	}

	public JdbcMapping jdbcMapping() {
		return underlyingJdbcMapping;
	}

	@Nonnull
	@Override
	public String getContainingTableExpression() {
		return table;
	}

	@Nonnull
	@Override
	public String getSelectionExpression() {
		return column;
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

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping();
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return jdbcMapping().getMappedJavaType();
	}

	@Nonnull
	@Override
	public String getPartName() {
		return ROLE_NAME;
	}

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Nonnull
	@Override
	public JdbcMapping getUnderlyingJdbcMapping() {
		return underlyingJdbcMapping;
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return underlyingJdbcMapping.disassemble( value, session, value );
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		cacheKey.addValue( underlyingJdbcMapping.disassemble( value, session, value ) );
		cacheKey.addHashCode( underlyingJdbcMapping.getHashCode( value ) );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
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
	public EntityMappingType findContainingEntityMapping() {
		return declaringType.findContainingEntityMapping();
	}

	@Nonnull
	@Override
	public MappingType getMappedType() {
		return getJdbcMapping();
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
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachSelectable(@Nonnull SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return 1;
	}

	@Nonnull
	@Override
	public BasicFetch<?> generateFetch(
			@Nonnull FetchParent fetchParent,
			@Nonnull NavigablePath fetchablePath,
			@Nonnull FetchTiming fetchTiming,
			boolean selected,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		final var sqlAstCreationState = creationState.getSqlAstCreationState();
		final var fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
		final var sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		final var tableGroup = DiscriminatedAssociationMapping.getTableGroup(
				fetchablePath.getParent(),
				fromClauseAccess
		);
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

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
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

	@Nonnull
	@Override
	public Expression resolveSqlExpression(
			@Nonnull NavigablePath navigablePath,
			@Nullable JdbcMapping jdbcMappingToUse,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAstCreationState creationState) {
		final var tableReference =
				tableGroup.resolveTableReference( navigablePath, this,
						getContainingTableExpression() );
		return creationState.getSqlExpressionResolver()
				.resolveSqlExpression( tableReference, this );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		resolveSqlSelection( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
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
				sqlAstCreationState.getCreationContext().getTypeConfiguration()
		);
	}
}
