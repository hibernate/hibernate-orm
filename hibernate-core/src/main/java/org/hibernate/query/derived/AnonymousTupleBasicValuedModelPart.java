/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.util.function.BiConsumer;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleBasicValuedModelPart implements ModelPart, MappingType, BasicValuedModelPart {

	private static final FetchOptions FETCH_OPTIONS = FetchOptions.valueOf( FetchTiming.IMMEDIATE, FetchStyle.JOIN );
	private final String partName;
	private final String selectionExpression;
	private final SqmExpressible<?> expressible;
	private final JdbcMapping jdbcMapping;

	public AnonymousTupleBasicValuedModelPart(
			String partName,
			String selectionExpression,
			SqmExpressible<?> expressible,
			JdbcMapping jdbcMapping) {
		this.partName = partName;
		this.selectionExpression = selectionExpression;
		this.expressible = expressible;
		this.jdbcMapping = jdbcMapping;
	}

	@Override
	public MappingType getPartMappingType() {
		return this;
	}

	@Override
	public JavaType<?> getJavaType() {
		return expressible.getExpressibleJavaType();
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return expressible.getExpressibleJavaType();
	}

	@Override
	public String getPartName() {
		return partName;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return null;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public String getContainingTableExpression() {
		return "";
	}

	@Override
	public String getSelectionExpression() {
		return selectionExpression;
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
	public boolean isFormula() {
		return false;
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
	public MappingType getMappedType() {
		return this;
	}

	@Override
	public String getFetchableName() {
		return partName;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return FETCH_OPTIONS;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = resolveSqlSelection(
				navigablePath,
				tableGroup,
				null,
				creationState.getSqlAstCreationState()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				jdbcMapping,
				navigablePath
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
				getContainingTableExpression()
		);
		final Expression expression = expressionResolver.resolveSqlExpression(
				createColumnReferenceKey( tableReference, getSelectionExpression() ),
				sqlAstProcessingState -> new ColumnReference(
						tableReference,
						this,
						creationState.getCreationContext().getSessionFactory()
				)
		);
		return expressionResolver.resolveSqlSelection(
				expression,
				getJdbcMapping().getJdbcJavaType(),
				fetchParent,
				creationState.getCreationContext().getSessionFactory().getTypeConfiguration()
		);
	}

	@Override
	public BasicFetch generateFetch(
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
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		resolveSqlSelection( navigablePath, tableGroup, null, creationState.getSqlAstCreationState() );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept(
				resolveSqlSelection( navigablePath, tableGroup, null, creationState.getSqlAstCreationState() ),
				getJdbcMapping()
		);
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( domainValue, this );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	public int forEachJdbcValue(Object value, Clause clause, int offset, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
