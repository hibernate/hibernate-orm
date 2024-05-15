/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SoftDeletableModelPart;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * SoftDeleteMapping implementation
 *
 * @author Steve Ebersole
 */
public class SoftDeleteMappingImpl implements SoftDeleteMapping {
	public static final String ROLE_NAME = "{soft-delete}";

	private final SoftDeletableModelPart softDeletable;
	private final String columnName;
	private final String tableName;
	private final Object deletedLiteralValue;
	private final String deletedLiteralText;
	private final Object nonDeletedLiteralValue;
	private final String nonDeletedLiteralText;
	private final JdbcMapping jdbcMapping;

	private final NavigableRole navigableRole;

	public SoftDeleteMappingImpl(
			SoftDeletableModelPart softDeletable,
			String columnName,
			String tableName,
			Object deletedLiteralValue,
			String deletedLiteralText,
			Object nonDeletedLiteralValue,
			String nonDeletedLiteralText,
			JdbcMapping jdbcMapping) {
		this.softDeletable = softDeletable;
		this.columnName = columnName;
		this.tableName = tableName;
		this.deletedLiteralValue = deletedLiteralValue;
		this.deletedLiteralText = deletedLiteralText;
		this.nonDeletedLiteralValue = nonDeletedLiteralValue;
		this.nonDeletedLiteralText = nonDeletedLiteralText;
		this.jdbcMapping = jdbcMapping;

		this.navigableRole = softDeletable.getNavigableRole().append( ROLE_NAME );
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
