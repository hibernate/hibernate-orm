/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Nathan Xu
 */
public class EntityRowIdMappingImpl implements EntityRowIdMapping {
	private final String rowIdName;
	private final EntityMappingType declaringType;
	private final String tableExpression;
	private final BasicType<Object> rowIdType;

	public EntityRowIdMappingImpl(String rowIdName, String tableExpression, EntityMappingType declaringType) {
		this.rowIdName = rowIdName;
		this.tableExpression = tableExpression;
		this.declaringType = declaringType;
		final SessionFactoryImplementor factory = declaringType.getEntityPersister().getFactory();
		this.rowIdType = factory.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( Object.class, factory.getJdbcServices().getDialect().rowIdSqlType() );
	}

	@Override
	public String getRowIdName() {
		return rowIdName;
	}

	@Override
	public MappingType getPartMappingType() {
		return rowIdType;
	}

	@Override
	public JavaType<?> getJavaType() {
		return rowIdType.getJavaTypeDescriptor();
	}

	@Override
	public String getPartName() {
		return rowIdName;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return declaringType;
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
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();

		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference columnTableReference = tableGroup.resolveTableReference( navigablePath, tableExpression );

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression( columnTableReference, this ),
				rowIdType.getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getSessionFactory().getTypeConfiguration()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				rowIdType,
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public int getJdbcTypeCount() {
		return 1;
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return getJdbcMapping();
	}

	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return getJdbcMapping();
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return rowIdType.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		rowIdType.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return rowIdType.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
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
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public String getSelectionExpression() {
		return rowIdName;
	}

	@Override
	public String getCustomReadExpression() {
		return null;
	}

	@Override
	public String getCustomWriteExpression() {
		return null;
	}

	@Override
	public String getColumnDefinition() {
		return null;
	}

	@Override
	public Long getLength() {
		return null;
	}

	@Override
	public Integer getPrecision() {
		return null;
	}

	@Override
	public Integer getScale() {
		return null;
	}

	@Override
	public Integer getTemporalPrecision() {
		return null;
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
		return false;
	}

	@Override
	public boolean isUpdateable() {
		return false;
	}

	@Override
	public boolean isPartitioned() {
		return false;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return rowIdType.getJdbcMapping();
	}

	@Override
	public MappingType getMappedType() {
		return rowIdType;
	}

	@Override
	public String getFetchableName() {
		return rowIdName;
	}

	@Override
	public int getFetchableKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}
}
