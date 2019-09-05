/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqlExpressionResolver;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.internal.ScalarDomainResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class BasicValuedSingularAttributeMapping extends AbstractSingularAttributeMapping implements SingularAttributeMapping, BasicValuedModelPart {
	private final String tableExpression;
	private final String mappedColumnExpression;

	private final JdbcMapping jdbcMapping;
	private final BasicValueConverter valueConverter;

	public BasicValuedSingularAttributeMapping(
			String attributeName,
			String tableExpression,
			String mappedColumnExpression,
			BasicValueConverter valueConverter,
			BasicType basicType,
			JdbcMapping jdbcMapping) {
		super( attributeName, basicType );
		this.tableExpression = tableExpression;
		this.mappedColumnExpression = mappedColumnExpression;
		this.valueConverter = valueConverter;
		this.jdbcMapping = jdbcMapping;
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
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		final SqlSelection sqlSelection = expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								getContainingTableExpression(),
								getMappedColumnExpression()
						),
						sqlAstProcessingState -> new ColumnReference(
								getMappedColumnExpression(),
								tableGroup.resolveTableReference( getContainingTableExpression() ).getIdentificationVariable(),
								jdbcMapping,
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				valueConverter == null ? getMappedTypeDescriptor().getMappedJavaTypeDescriptor() : valueConverter.getRelationalJavaDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);

		//noinspection unchecked
		return new ScalarDomainResultImpl(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				getMappedTypeDescriptor().getMappedJavaTypeDescriptor(),
				valueConverter,
				navigablePath
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();

		// the act of resolving the selection creates the selection if it not already part of the collected selections
		expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								getContainingTableExpression(),
								getMappedColumnExpression()
						),
						sqlAstProcessingState -> new ColumnReference(
								getMappedColumnExpression(),
								tableGroup.resolveTableReference( getContainingTableExpression() ).getIdentificationVariable(),
								jdbcMapping,
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				valueConverter == null ? getMappedTypeDescriptor().getMappedJavaTypeDescriptor() : valueConverter.getRelationalJavaDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
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
}
