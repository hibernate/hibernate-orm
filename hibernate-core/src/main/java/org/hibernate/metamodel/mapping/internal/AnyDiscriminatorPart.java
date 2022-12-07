/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
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
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * Acts as a ModelPart for the discriminator portion of an any-valued mapping
 *
 * @author Steve Ebersole
 */
public class AnyDiscriminatorPart implements BasicValuedModelPart, FetchOptions, SelectableMapping {
	public static final String ROLE_NAME = EntityDiscriminatorMapping.ROLE_NAME;

	private final NavigableRole navigableRole;
	private final DiscriminatedAssociationModelPart declaringType;

	private final String table;
	private final String column;
	private final String columnDefinition;
	private final Long length;
	private final Integer precision;
	private final Integer scale;
	private final boolean nullable;

	private final MetaType metaType;

	public AnyDiscriminatorPart(
			NavigableRole partRole,
			DiscriminatedAssociationModelPart declaringType,
			String table,
			String column,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			boolean nullable,
			MetaType metaType) {
		this.navigableRole = partRole;
		this.declaringType = declaringType;
		this.table = table;
		this.column = column;
		this.columnDefinition = columnDefinition;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
		this.nullable = nullable;
		this.metaType = metaType;
	}

	public MetaType getMetaType() {
		return metaType;
	}

	public JdbcMapping jdbcMapping() {
		return (JdbcMapping) metaType.getBaseType();
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
	public String getColumnDefinition() {
		return columnDefinition;
	}

	@Override
	public Long getLength() {
		return length;
	}

	@Override
	public Integer getPrecision() {
		return precision;
	}

	@Override
	public Integer getScale() {
		return scale;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping();
	}

	@Override
	public JavaType<?> getJavaType() {
		return jdbcMapping().getMappedJavaType();
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
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		final Object discriminator = metaType.disassemble( value, session, value );
		return discriminator;
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( domainValue, this );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return declaringType.findContainingEntityMapping();
	}

	@Override
	public MappingType getMappedType() {
		return null;
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
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return 1;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SessionFactoryImplementor sessionFactory = sqlAstCreationState.getCreationContext().getSessionFactory();
		final FromClauseAccess fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		final TableGroup tableGroup = fromClauseAccess.getTableGroup( fetchablePath.getParent().getParent() );
		final TableReference tableReference = tableGroup.resolveTableReference( fetchablePath, table );
		final Expression columnReference = sqlExpressionResolver.resolveSqlExpression(
				createColumnReferenceKey( tableReference, column ),
				processingState -> new ColumnReference(
						tableReference,
						column,
						false,
						null,
						null,
						jdbcMapping(),
						sessionFactory
				)
		);
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				columnReference,
				jdbcMapping().getJdbcJavaType(),
				fetchParent,
				sessionFactory.getTypeConfiguration()
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
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}
}
