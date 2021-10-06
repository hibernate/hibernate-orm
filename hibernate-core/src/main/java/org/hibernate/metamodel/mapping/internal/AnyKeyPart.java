/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Acts as a ModelPart for the key portion of an any-valued mapping
 *
 * @author Steve Ebersole
 */
public class AnyKeyPart implements BasicValuedModelPart, FetchOptions {
	public static final String ROLE_NAME = "{key}";

	private final NavigableRole navigableRole;
	private final String table;
	private final String column;
	private final DiscriminatedAssociationModelPart anyPart;
	private final boolean nullable;
	private final JdbcMapping jdbcMapping;

	public AnyKeyPart(
			NavigableRole navigableRole,
			DiscriminatedAssociationModelPart anyPart, String table,
			String column,
			boolean nullable,
			JdbcMapping jdbcMapping) {
		this.navigableRole = navigableRole;
		this.table = table;
		this.column = column;
		this.anyPart = anyPart;
		this.nullable = nullable;
		this.jdbcMapping = jdbcMapping;
	}

	@Override
	public String getContainingTableExpression() {
		return table;
	}

	@Override
	public String getSelectionExpression() {
		return column;
	}

	@Override
	public boolean isFormula() {
		return false;
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
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public JavaType<?> getJavaTypeDescriptor() {
		return jdbcMapping.getMappedJavaTypeDescriptor();
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
	public EntityMappingType findContainingEntityMapping() {
		return anyPart.findContainingEntityMapping();
	}

	@Override
	public MappingType getMappedType() {
		return jdbcMapping;
	}

	@Override
	public String getFetchableName() {
		return getPartName();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final FromClauseAccess fromClauseAccess = creationState
				.getSqlAstCreationState()
				.getFromClauseAccess();
		final SqlExpressionResolver sqlExpressionResolver = creationState
				.getSqlAstCreationState()
				.getSqlExpressionResolver();
		final SessionFactoryImplementor sessionFactory = creationState
				.getSqlAstCreationState()
				.getCreationContext()
				.getSessionFactory();

		final TableGroup tableGroup = fromClauseAccess.getTableGroup( fetchParent.getNavigablePath().getParent() );
		final TableReference tableReference = tableGroup.resolveTableReference( fetchablePath, table );

		final Expression columnReference = sqlExpressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey( tableReference, column ),
				processingState -> new ColumnReference(
						tableReference,
						column,
						false,
						null,
						null,
						jdbcMapping,
						sessionFactory
				)
		);

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				columnReference,
				getJavaTypeDescriptor(),
				sessionFactory.getTypeConfiguration()
		);

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				null,
				fetchTiming,
				creationState
		);
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.SELECT;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return getJdbcTypeCount();
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( domainValue, this );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return Collections.singletonList( jdbcMapping );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return anyPart.disassemble( value, session );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return anyPart.forEachDisassembledJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return anyPart.createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
	}
}
