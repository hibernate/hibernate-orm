/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.dialect.function.CurrentFunction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.SoftDeletable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SoftDeletableModelPart;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import java.time.Instant;
import java.util.function.BiConsumer;

import static java.util.Collections.emptyList;
import static org.hibernate.query.sqm.ComparisonOperator.EQUAL;

/**
 * SoftDeleteMapping implementation
 *
 * @author Steve Ebersole
 */
public class SoftDeleteMappingImpl implements SoftDeleteMapping {
	private final NavigableRole navigableRole;
	private final SoftDeletableModelPart softDeletable;
	private final SoftDeleteType strategy;
	private final String columnName;
	private final String tableName;
	private final JdbcMapping jdbcMapping;

	private final Object deletionIndicator;

	// TIMESTAMP
	private final String currentTimestampFunctionName;
	private final SelfRenderingFunctionSqlAstExpression<?> currentTimestampFunctionExpression;

	// ACTIVE/DELETED
	private final Object deletedLiteralValue;
	private final String deletedLiteralText;
	private final Object nonDeletedLiteralValue;
	private final String nonDeletedLiteralText;

	public SoftDeleteMappingImpl(
			SoftDeletableModelPart softDeletable,
			SoftDeletable bootMapping,
			String tableName,
			MappingModelCreationProcess modelCreationProcess) {
		assert bootMapping.getSoftDeleteColumn() != null;

		this.softDeletable = softDeletable;
		navigableRole = softDeletable.getNavigableRole().append( ROLE_NAME );
		strategy = bootMapping.getSoftDeleteStrategy();

		final var dialect = modelCreationProcess.getCreationContext().getDialect();

		final var softDeleteColumn = bootMapping.getSoftDeleteColumn();
		final var columnValue = (BasicValue) softDeleteColumn.getValue();
		final var resolution = columnValue.resolve();

		this.tableName = tableName;
		columnName = softDeleteColumn.getName();
		jdbcMapping = resolution.getJdbcMapping();

		if ( bootMapping.getSoftDeleteStrategy() == SoftDeleteType.TIMESTAMP ) {
			currentTimestampFunctionName = dialect.currentTimestamp();
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

			deletedLiteralText = literalFormatter.toJdbcLiteral( deletedLiteralValue, dialect, null );
			nonDeletedLiteralText = literalFormatter.toJdbcLiteral( nonDeletedLiteralValue, dialect, null );

			deletionIndicator = deletedLiteralValue;

			currentTimestampFunctionName = null;
			currentTimestampFunctionExpression = null;
		}
	}

	@Override
	public SoftDeleteType getSoftDeleteStrategy() {
		return strategy;
	}

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public String getWriteExpression() {
		return strategy == SoftDeleteType.TIMESTAMP ? null : nonDeletedLiteralText;
	}

	public Object getDeletionIndicator() {
		return deletionIndicator;
	}

	@Override
	public Assignment createSoftDeleteAssignment(TableReference tableReference) {
		final var columnReference = new ColumnReference( tableReference, this );
		final var valueExpression =
				strategy == SoftDeleteType.TIMESTAMP
						? currentTimestampFunctionExpression
						: new JdbcLiteral<>( deletedLiteralValue, jdbcMapping );
		return new Assignment( columnReference, valueExpression );
	}

	@Override
	public Predicate createNonDeletedRestriction(TableReference tableReference) {
		final var softDeleteColumn = new ColumnReference( tableReference, this );
		if ( strategy == SoftDeleteType.TIMESTAMP ) {
			return new NullnessPredicate( softDeleteColumn, false, jdbcMapping );
		}
		else {
			final JdbcLiteral<?> notDeletedLiteral = new JdbcLiteral<>( nonDeletedLiteralValue, jdbcMapping );
			return new ComparisonPredicate( softDeleteColumn, EQUAL, notDeletedLiteral );
		}
	}

	@Override
	public Predicate createNonDeletedRestriction(TableReference tableReference, SqlExpressionResolver expressionResolver) {
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

	@Override
	public ColumnValueBinding createNonDeletedValueBinding(ColumnReference softDeleteColumnReference) {
		final var nonDeletedFragment =
				strategy == SoftDeleteType.TIMESTAMP
						? new ColumnWriteFragment( null, emptyList(), jdbcMapping )
						: new ColumnWriteFragment( nonDeletedLiteralText, emptyList(), jdbcMapping );
		return new ColumnValueBinding( softDeleteColumnReference, nonDeletedFragment );
	}

	@Override
	public ColumnValueBinding createDeletedValueBinding(ColumnReference softDeleteColumnReference) {
		final ColumnWriteFragment deletedFragment =
				strategy == SoftDeleteType.TIMESTAMP
						? new ColumnWriteFragment( currentTimestampFunctionName, emptyList(), getJdbcMapping() )
						: new ColumnWriteFragment( deletedLiteralText, emptyList(), jdbcMapping );
		return new ColumnValueBinding( softDeleteColumnReference, deletedFragment );
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
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
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping );
		return 1;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
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
		return 1;
	}

	@Override
	public MappingType getPartMappingType() {
		return jdbcMapping;
	}

	@Override
	public JavaType<?> getJavaType() {
		return jdbcMapping.getMappedJavaType();
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
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
		final var sqlSelection = resolveSqlSelection( navigablePath, tableGroup, creationState );
		selectionConsumer.accept( sqlSelection, getJdbcMapping() );
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
		return 1;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return softDeletable.findContainingEntityMapping();
	}

	@Override
	public String toString() {
		return "SoftDeleteMapping(" + tableName + "." + columnName + ")";
	}
}
