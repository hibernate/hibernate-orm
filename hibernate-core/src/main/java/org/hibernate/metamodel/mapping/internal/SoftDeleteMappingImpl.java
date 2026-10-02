package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.dialect.function.CurrentFunction;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.SoftDeletable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.LegacyAuxiliaryMutationSupport;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SoftDeletableModelPart;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.creation.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.JdbcLiteral;
import org.hibernate.sql.ast.spi.query.from.LazyTableGroup;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.NullnessPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.update.Assignment;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.builder.MutationGroupBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilder;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static java.util.Collections.emptyList;
import static org.hibernate.query.sqm.ComparisonOperator.EQUAL;


/**
 * SoftDeleteMapping implementation
 *
 * @author Steve Ebersole
 */
public class SoftDeleteMappingImpl implements SoftDeleteMapping, LegacyAuxiliaryMutationSupport {
	private final NavigableRole navigableRole;
	private final SoftDeletableModelPart softDeletable;
	private final SoftDeleteType strategy;
	private final String columnName;
	private final String tableName;
	private final JdbcMapping jdbcMapping;

	private final Object deletionIndicator;

	// TIMESTAMP
	@Nullable private final String currentTimestampFunctionName;
	@Nullable private final SelfRenderingFunctionSqlAstExpression<?> currentTimestampFunctionExpression;

	// ACTIVE/DELETED
	@Nullable private final Object deletedLiteralValue;
	@Nullable private final String deletedLiteralText;
	@Nullable private final Object nonDeletedLiteralValue;
	@Nullable private final String nonDeletedLiteralText;

	public SoftDeleteMappingImpl(
			SoftDeletableModelPart softDeletable,
			SoftDeletable bootMapping,
			String tableName,
			MappingModelCreationProcess modelCreationProcess) {
		assert bootMapping.getSoftDeleteColumn() != null;

		this.softDeletable = softDeletable;
		navigableRole = castNonNull( softDeletable.getNavigableRole() ).append( ROLE_NAME );
		strategy = bootMapping.getSoftDeleteStrategy();

		final var dialect = modelCreationProcess.getCreationContext().getDialect();

		final var softDeleteColumn = bootMapping.getSoftDeleteColumn();
		final var columnValue = (BasicValue) softDeleteColumn.getValue();
		final var resolution = columnValue.resolve();

		this.tableName = tableName;
		columnName = softDeleteColumn.getName();
		jdbcMapping = resolution.getJdbcMapping();

		if ( bootMapping.getSoftDeleteStrategy() == SoftDeleteType.TIMESTAMP ) {
			currentTimestampFunctionName = dialect.getCurrentTemporalSupport().currentTimestamp();
			final var currentTimestampFunctionType =
					modelCreationProcess.getCreationContext().getTypeConfiguration()
							.getBasicTypeForJavaType( Instant.class );
			final var currentTimestampFunction = new CurrentFunction(
					currentTimestampFunctionName,
					currentTimestampFunctionName,
					currentTimestampFunctionType
			);
			currentTimestampFunctionExpression = new SelfRenderingFunctionSqlAstExpression<>(
					currentTimestampFunctionName,
					currentTimestampFunction,
					emptyList(),
					currentTimestampFunctionType,
					softDeletable
			);

			deletionIndicator = currentTimestampFunctionName;

			deletedLiteralValue = null;
			deletedLiteralText = null;

			nonDeletedLiteralValue = null;
			nonDeletedLiteralText = null;
		}
		else {
			//noinspection unchecked
			final var converter =
					(BasicValueConverter<Boolean, ?>)
							resolution.getValueConverter();
			//noinspection unchecked
			final JdbcLiteralFormatter<Object> literalFormatter =
					resolution.getJdbcMapping().getJdbcLiteralFormatter();

			if ( converter == null ) {
				// the database column is BIT or BOOLEAN: pass-thru
				deletedLiteralValue = true;
				nonDeletedLiteralValue = false;
			}
			else {
				deletedLiteralValue = converter.toRelationalValue( true );
				nonDeletedLiteralValue = converter.toRelationalValue( false );
			}

			deletedLiteralText = castNonNull( literalFormatter ).toJdbcLiteral( deletedLiteralValue, dialect, null );
			nonDeletedLiteralText = castNonNull( literalFormatter ).toJdbcLiteral( nonDeletedLiteralValue, dialect, null );

			deletionIndicator = deletedLiteralValue;

			currentTimestampFunctionName = null;
			currentTimestampFunctionExpression = null;
		}
	}

	@Nonnull
	@Override
	public SoftDeleteType getSoftDeleteStrategy() {
		return strategy;
	}

	@Nonnull
	@Override
	public String getColumnName() {
		return columnName;
	}

	@Nonnull
	@Override
	public String getTableName() {
		return tableName;
	}

	@Nullable
	@Override
	public String getWriteExpression() {
		return strategy == SoftDeleteType.TIMESTAMP ? null : nonDeletedLiteralText;
	}

	public Object getDeletionIndicator() {
		return deletionIndicator;
	}

	@Nonnull
	@Override
	public Assignment createSoftDeleteAssignment(@Nonnull TableReference tableReference) {
		final var columnReference = new ColumnReference( tableReference, this );
		final var valueExpression =
				strategy == SoftDeleteType.TIMESTAMP
						? currentTimestampFunctionExpression
						: new JdbcLiteral<>( deletedLiteralValue, jdbcMapping );
		return new Assignment( columnReference, valueExpression );
	}

	@Nonnull
	@Override
	public Predicate createNonDeletedRestriction(@Nonnull TableReference tableReference) {
		final var softDeleteColumn = new ColumnReference( tableReference, this );
		if ( strategy == SoftDeleteType.TIMESTAMP ) {
			return new NullnessPredicate( softDeleteColumn, false, jdbcMapping );
		}
		else {
			final JdbcLiteral<?> notDeletedLiteral = new JdbcLiteral<>( nonDeletedLiteralValue, jdbcMapping );
			return new ComparisonPredicate( softDeleteColumn, EQUAL, notDeletedLiteral );
		}
	}

	@Nonnull
	@Override
	public Predicate createNonDeletedRestriction(@Nonnull TableReference tableReference, @Nonnull SqlExpressionResolver expressionResolver) {
		final var softDeleteColumn = expressionResolver.resolveSqlExpression( tableReference, this );
		if ( strategy == SoftDeleteType.TIMESTAMP ) {
			return new NullnessPredicate( softDeleteColumn, false, jdbcMapping );
		}
		else {
			return new ComparisonPredicate(
					softDeleteColumn,
					EQUAL,
					new JdbcLiteral<>( nonDeletedLiteralValue, jdbcMapping )
			);
		}
	}

	@Nonnull
	@Override
	public ColumnValueBinding createNonDeletedValueBinding(@Nonnull ColumnReference softDeleteColumnReference) {
		final var nonDeletedFragment =
				strategy == SoftDeleteType.TIMESTAMP
						? new ColumnWriteFragment( null, emptyList(), this )
						: new ColumnWriteFragment( nonDeletedLiteralText, emptyList(), this );
		return new ColumnValueBinding( softDeleteColumnReference, nonDeletedFragment );
	}

	@Nonnull
	@Override
	public ColumnValueBinding createDeletedValueBinding(@Nonnull ColumnReference softDeleteColumnReference) {
		final ColumnWriteFragment deletedFragment =
				strategy == SoftDeleteType.TIMESTAMP
						? new ColumnWriteFragment( currentTimestampFunctionName, emptyList(), this )
						: new ColumnWriteFragment( deletedLiteralText, emptyList(), this );
		return new ColumnValueBinding( softDeleteColumnReference, deletedFragment );
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
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

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping );
		return 1;
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
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
		return 1;
	}

	@Nonnull
	@Override
	public MappingType getPartMappingType() {
		return jdbcMapping;
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return jdbcMapping.getMappedJavaType();
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
		final var sqlSelection = resolveSqlSelection( navigablePath, tableGroup, creationState );
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
			DomainResultCreationState creationState) {
		final var indicatorTable = softDeletable.getSoftDeleteTableDetails();
		final var tableReference = tableGroup.resolveTableReference(
				navigablePath.getRealParent(),
				indicatorTable.getTableName()
		);
		final var expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		return expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression( tableReference, this ),
				getJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
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
		final var sqlSelection = resolveSqlSelection( navigablePath, tableGroup, creationState );
		selectionConsumer.accept( sqlSelection, getJdbcMapping() );
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
		return 1;
	}

	@Nullable
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return softDeletable.findContainingEntityMapping();
	}

	@Override
	public void addToInsertGroup(@Nonnull MutationGroupBuilder insertGroupBuilder, @Nonnull EntityPersister persister) {
		final TableInsertBuilder insertBuilder =
				insertGroupBuilder.getTableDetailsBuilder( persister.getIdentifierTableName() );
		insertBuilder.addValueColumn( createNonDeletedValueBinding(
				new ColumnReference( insertBuilder.getMutatingTable(), this ) ) );
	}

	@Override
	public void applyPredicate(
			@Nonnull EntityMappingType associatedEntityMappingType,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull LazyTableGroup lazyTableGroup,
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqlAstCreationState creationState) {
		// add the restriction
		final var tableReference =
				lazyTableGroup.resolveTableReference( navigablePath,
						associatedEntityMappingType.getSoftDeleteTableDetails().getTableName() );
		predicateConsumer.accept( createNonDeletedRestriction( tableReference,
				creationState.getSqlExpressionResolver() ) );
	}

	@Override
	public void applyPredicate(
			@Nonnull EntityMappingType associatedEntityDescriptor,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers influencers) {
		final String primaryTableName =
				associatedEntityDescriptor.getSoftDeleteTableDetails().getTableName();
		predicateConsumer.accept( createNonDeletedRestriction(
				tableGroup.resolveTableReference( primaryTableName ) ) );
	}

	@Override
	public void applyPredicate(
			@Nonnull PluralAttributeMapping associatedEntityDescriptor,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers influencers) {
		predicateConsumer.accept( createNonDeletedRestriction(
				tableGroup.resolveTableReference( getTableName() ) ) );
	}

	@Override
	public void applyPredicate(@Nonnull TableGroupJoin tableGroupJoin, @Nonnull LoadQueryInfluencers loadQueryInfluencers) {
		tableGroupJoin.applyPredicate( createNonDeletedRestriction(
				tableGroupJoin.getJoinedGroup().resolveTableReference( getTableName() )
		) );
	}

	@Override
	public void applyPredicate(
			@Nonnull Supplier<Consumer<Predicate>> predicateCollector,
			@Nonnull SqlAstCreationState creationState,
			@Nonnull TableGroup tableGroup,
			@Nonnull NamedTableReference rootTableReference,
			@Nonnull EntityMappingType entityMappingType) {
		final var tableReference =
				tableGroup.resolveTableReference( getTableName() );
		final var softDeletePredicate =
				createNonDeletedRestriction( tableReference,
						creationState.getSqlExpressionResolver() );
		predicateCollector.get().accept( softDeletePredicate );
		if ( tableReference != rootTableReference && creationState.supportsEntityNameUsage() ) {
			// Register entity name usage for the hierarchy root table to avoid pruning
			creationState.registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
					entityMappingType.getRootEntityDescriptor().getEntityName() );
		}
	}

	@Override
	public boolean useAuxiliaryTable(@Nonnull LoadQueryInfluencers influencers) {
		return false;
	}

	@Override
	public boolean isAffectedByInfluencers(@Nonnull LoadQueryInfluencers influencers) {
		return false;
	}

	@Override
	public String toString() {
		return "SoftDeleteMapping(" + tableName + "." + columnName + ")";
	}
}
