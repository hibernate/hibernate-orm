/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class BasicValuedSingularAttributeMapping extends AbstractSingularAttributeMapping implements SingularAttributeMapping, BasicValuedModelPart {
	private final NavigableRole navigableRole;
	private final String tableExpression;
	private final String mappedColumnExpression;

	private final JdbcMapping jdbcMapping;
	private final BasicValueConverter valueConverter;

	@SuppressWarnings("WeakerAccess")
	public BasicValuedSingularAttributeMapping(
			String attributeName,
			int stateArrayPosition,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchStrategy mappedFetchStrategy,
			String tableExpression,
			String mappedColumnExpression,
			BasicValueConverter valueConverter,
			BasicType basicType,
			JdbcMapping jdbcMapping,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super( attributeName, stateArrayPosition, attributeMetadataAccess, mappedFetchStrategy, basicType, declaringType, propertyAccess );
		this.navigableRole = declaringType.getNavigableRole().append( attributeName );
		this.tableExpression = tableExpression;
		this.mappedColumnExpression = mappedColumnExpression;
		this.valueConverter = valueConverter;
		this.jdbcMapping = jdbcMapping;
	}

	@Override
	public BasicType getBasicType() {
		return getMappedTypeDescriptor();
	}

	@Override
	public BasicType getMappedTypeDescriptor() {
		return (BasicType) super.getMappedTypeDescriptor();
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public String getMappedColumnExpression() {
		return mappedColumnExpression;
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public BasicValueConverter getConverter() {
		return valueConverter;
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
		final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				getMappedTypeDescriptor().getMappedJavaTypeDescriptor(),
				valueConverter,
				navigablePath
		);
	}

	private SqlSelection resolveSqlSelection(TableGroup tableGroup, DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();

		final TableReference tableReference = tableGroup.resolveTableReference( getContainingTableExpression() );

		return expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableReference,
								getMappedColumnExpression()
						),
						sqlAstProcessingState -> new ColumnReference(
								tableReference.getIdentificationVariable(),
								getMappedColumnExpression(),
								jdbcMapping,
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				valueConverter == null ? getMappedTypeDescriptor().getMappedJavaTypeDescriptor() : valueConverter.getRelationalJavaDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		// the act of resolving the selection creates the selection if it not already part of the collected selections

		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference( getContainingTableExpression() );

		expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableReference,
								getMappedColumnExpression()
						),
						sqlAstProcessingState -> new ColumnReference(
								tableReference.getIdentificationVariable(),
								getMappedColumnExpression(),
								jdbcMapping,
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				valueConverter == null ? getMappedTypeDescriptor().getMappedJavaTypeDescriptor() : valueConverter.getRelationalJavaDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
				fetchParent.getNavigablePath()
		);

		assert tableGroup != null;

		final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );

		return new BasicFetch(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				getAttributeMetadataAccess().resolveAttributeMetadata( null ).isNullable(),
				getConverter(),
				fetchTiming,
				creationState
		);
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( valueConverter != null ) {
			//noinspection unchecked
			return valueConverter.toRelationalValue( value );
		}
		return value;
	}

	@Override
	public void visitDisassembledJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( value, getJdbcMapping() );
	}

	@Override
	public void visitJdbcTypes(
			Consumer<JdbcMapping> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( getJdbcMapping() );
	}

	@Override
	public void visitColumns(ColumnConsumer consumer) {
		consumer.accept( mappedColumnExpression, tableExpression, jdbcMapping );
	}
}
