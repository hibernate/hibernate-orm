/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.CurrentFunction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.SoftDeletable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SoftDeletableModelPart;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
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
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import java.time.Instant;
import java.util.Collections;
import java.util.function.BiConsumer;

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

		this.navigableRole = softDeletable.getNavigableRole().append( ROLE_NAME );
		this.softDeletable = softDeletable;
		this.strategy = bootMapping.getSoftDeleteStrategy();

		final Dialect dialect = modelCreationProcess.getCreationContext().getDialect();

		final Column softDeleteColumn = bootMapping.getSoftDeleteColumn();
		final BasicValue columnValue = (BasicValue) softDeleteColumn.getValue();
		final BasicValue.Resolution<?> resolution = columnValue.resolve();

		this.columnName = softDeleteColumn.getName();
		this.tableName = tableName;
		this.jdbcMapping = resolution.getJdbcMapping();

		if ( bootMapping.getSoftDeleteStrategy() == SoftDeleteType.TIMESTAMP ) {
			this.currentTimestampFunctionName = dialect.currentTimestamp();
			final BasicType<?> currentTimestampFunctionType = modelCreationProcess
					.getCreationContext()
					.getTypeConfiguration()
					.getBasicTypeForJavaType( Instant.class );
			final CurrentFunction currentTimestampFunction = new CurrentFunction(
					currentTimestampFunctionName,
					currentTimestampFunctionName,
					currentTimestampFunctionType
			);
			this.currentTimestampFunctionExpression = new SelfRenderingFunctionSqlAstExpression<>(
					currentTimestampFunctionName,
					currentTimestampFunction,
					Collections.emptyList(),
					currentTimestampFunctionType,
					softDeletable
			);

			this.deletionIndicator = currentTimestampFunctionName;

			this.deletedLiteralValue = null;
			this.deletedLiteralText = null;

			this.nonDeletedLiteralValue = null;
			this.nonDeletedLiteralText = null;
		}
		else {
			//noinspection unchecked
			final BasicValueConverter<Boolean, ?> converter =
					(BasicValueConverter<Boolean, ?>)
							resolution.getValueConverter();
			//noinspection unchecked
			final JdbcLiteralFormatter<Object> literalFormatter =
					resolution.getJdbcMapping().getJdbcLiteralFormatter();

			if ( converter == null ) {
				// the database column is BIT or BOOLEAN : pass-thru
				this.deletedLiteralValue = true;
				this.nonDeletedLiteralValue = false;
			}
			else {
				this.deletedLiteralValue = converter.toRelationalValue( true );
				this.nonDeletedLiteralValue = converter.toRelationalValue( false );
			}

			this.deletedLiteralText = literalFormatter.toJdbcLiteral( deletedLiteralValue, dialect, null );
			this.nonDeletedLiteralText = literalFormatter.toJdbcLiteral( nonDeletedLiteralValue, dialect, null );

			this.deletionIndicator = deletedLiteralValue;

			this.currentTimestampFunctionName = null;
			this.currentTimestampFunctionExpression = null;
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
		final ColumnReference columnReference = new ColumnReference( tableReference, this );
		final Expression valueExpression =
				strategy == SoftDeleteType.TIMESTAMP
						? currentTimestampFunctionExpression
						: new JdbcLiteral<>( deletedLiteralValue, jdbcMapping );
		return new Assignment( columnReference, valueExpression );
	}

	@Override
	public Predicate createNonDeletedRestriction(TableReference tableReference) {
		final ColumnReference softDeleteColumn = new ColumnReference( tableReference, this );
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
		final Expression softDeleteColumn = expressionResolver.resolveSqlExpression( tableReference, this );
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
						? new ColumnWriteFragment( null, Collections.emptyList(), this )
						: new ColumnWriteFragment( nonDeletedLiteralText, Collections.emptyList(), this );
		return new ColumnValueBinding( softDeleteColumnReference, nonDeletedFragment );
	}

	@Override
	public ColumnValueBinding createDeletedValueBinding(ColumnReference softDeleteColumnReference) {
		final ColumnWriteFragment deletedFragment =
				strategy == SoftDeleteType.TIMESTAMP
						? new ColumnWriteFragment( currentTimestampFunctionName, Collections.emptyList(), this )
						: new ColumnWriteFragment( deletedLiteralText, Collections.emptyList(), this );
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
		final SqlSelection sqlSelection = resolveSqlSelection( navigablePath, tableGroup, creationState );
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
		final TableDetails indicatorTable = softDeletable.getSoftDeleteTableDetails();
		final TableReference tableReference = tableGroup.resolveTableReference(
				navigablePath.getRealParent(),
				indicatorTable.getTableName()
		);
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
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
		final SqlSelection sqlSelection = resolveSqlSelection( navigablePath, tableGroup, creationState );
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
