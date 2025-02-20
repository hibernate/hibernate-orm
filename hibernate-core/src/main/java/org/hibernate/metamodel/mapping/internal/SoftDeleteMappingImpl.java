/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import java.time.Instant;
import java.util.function.BiConsumer;

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

	// TIMESTAMP
	private final String currentTimestampFunctionName;
	private final BasicType<?> currentTimestampFunctionType;
	private final FunctionRenderer currentTimestampFunction;

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
			this.currentTimestampFunctionType = modelCreationProcess.getCreationContext().getTypeConfiguration().getBasicTypeForJavaType( Instant.class );
			this.currentTimestampFunction = new CurrentFunction(
					currentTimestampFunctionName,
					currentTimestampFunctionName,
					currentTimestampFunctionType
			);

			this.deletedLiteralValue = null;
			this.deletedLiteralText = null;

			this.nonDeletedLiteralValue = null;
			this.nonDeletedLiteralText = null;
		}
		else {
			//noinspection unchecked
			final BasicValueConverter<Boolean, Object> converter = (BasicValueConverter<Boolean, Object>) resolution.getValueConverter();
			//noinspection unchecked
			final JdbcLiteralFormatter<Object> literalFormatter = resolution.getJdbcMapping().getJdbcLiteralFormatter();

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

			this.currentTimestampFunctionName = null;
			this.currentTimestampFunctionType = null;
			this.currentTimestampFunction = null;
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
	public Object getDeletedLiteralValue() {
		return deletedLiteralValue;
	}

	@Override
	public String getDeletedLiteralText() {
		return deletedLiteralText;
	}

	@Override
	public Object getNonDeletedLiteralValue() {
		return nonDeletedLiteralValue;
	}

	@Override
	public String getNonDeletedLiteralText() {
		return nonDeletedLiteralText;
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
		final SqlSelection sqlSelection = expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression( tableReference, this ),
				getJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
		return sqlSelection;
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
		return "SoftDeleteMappingImpl(" + tableName + "." + columnName + ")";
	}
}
