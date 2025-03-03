/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.List;

import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.query.SemanticException;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class SqmParameterInterpretation implements Expression, DomainResultProducer, SqlTupleContainer {
	private final MappingModelExpressible<?> valueMapping;
	private final List<JdbcParameter> jdbcParameters;
	private Expression resolvedExpression;

	public SqmParameterInterpretation(
			List<JdbcParameter> jdbcParameters,
			MappingModelExpressible<?> valueMapping) {

		if ( valueMapping instanceof EntityAssociationMapping mapping ) {
			this.valueMapping = mapping.getForeignKeyDescriptor().getPart( mapping.getSideNature() );
		}
		else if ( valueMapping instanceof EntityValuedModelPart ) {
			this.valueMapping = ( (EntityValuedModelPart) valueMapping ).getEntityMappingType().getIdentifierMapping();
		}
		else {
			this.valueMapping = valueMapping;
		}

		assert jdbcParameters != null;
		assert jdbcParameters.size() > 0;

		this.jdbcParameters = jdbcParameters;
	}

	private Expression determineResolvedExpression(List<JdbcParameter> jdbcParameters, MappingModelExpressible<?> valueMapping) {
		if ( valueMapping instanceof EmbeddableValuedModelPart
				|| valueMapping instanceof DiscriminatedAssociationModelPart ) {
			return new SqlTuple( jdbcParameters, valueMapping );
		}

		assert jdbcParameters.size() == 1;
		return jdbcParameters.get( 0 );
	}

	public Expression getResolvedExpression() {
		// We need to defer the resolution because the JdbcParameter might be replaced in BaseSqmToSqlAstConverter#replaceJdbcParametersType
		if ( resolvedExpression == null ) {
			return this.resolvedExpression = determineResolvedExpression( jdbcParameters, this.valueMapping );
		}
		return resolvedExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		getResolvedExpression().accept( sqlTreeWalker );
	}

	@Override
	public MappingModelExpressible<?> getExpressionType() {
		return valueMapping;
	}

	@Override
	public DomainResult<?> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final Expression resolvedExpression = getResolvedExpression();
		if ( resolvedExpression instanceof SqlTuple ) {
			throw new SemanticException( "Composite query parameter cannot be used in select" );
		}

		final JdbcMapping jdbcMapping = resolvedExpression.getExpressionType().getSingleJdbcMapping();
		final JavaType<?> jdbcJavaType = jdbcMapping.getJdbcJavaType();
		final BasicValueConverter<?, ?> converter = jdbcMapping.getValueConverter();

		final SqlSelection sqlSelection = creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				resolvedExpression,
				jdbcJavaType,
				null,
				creationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
		);

		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				jdbcMapping.getMappedJavaType(),
				converter,
				null,
				false,
				false
		);
	}

	@Override
	public SqlTuple getSqlTuple() {
		final Expression resolvedExpression = getResolvedExpression();
		return resolvedExpression instanceof SqlTuple
				? (SqlTuple) resolvedExpression
				: null;
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		resolveSqlSelection( creationState );
	}

	public SqlSelection resolveSqlSelection(DomainResultCreationState creationState) {
		final Expression resolvedExpression = getResolvedExpression();
		if ( resolvedExpression instanceof SqlTuple ) {
			throw new SemanticException( "Composite query parameter cannot be used in select" );
		}

		return creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				resolvedExpression,
				resolvedExpression.getExpressionType().getSingleJdbcMapping().getMappedJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
		);
	}
}
