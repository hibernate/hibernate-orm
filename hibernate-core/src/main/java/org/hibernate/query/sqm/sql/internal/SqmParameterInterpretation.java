/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.List;
import java.util.function.Function;

import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public class SqmParameterInterpretation implements Expression, DomainResultProducer, SqlTupleContainer {
	private final SqmParameter sqmParameter;
	private final QueryParameterImplementor<?> queryParameter;
	private final MappingModelExpressable valueMapping;
	private final Function<QueryParameterImplementor, QueryParameterBinding> queryParameterBindingResolver;

	private final Expression resolvedExpression;

	public SqmParameterInterpretation(
			SqmParameter sqmParameter,
			QueryParameterImplementor<?> queryParameter,
			List<JdbcParameter> jdbcParameters,
			MappingModelExpressable valueMapping,
			Function<QueryParameterImplementor, QueryParameterBinding> queryParameterBindingResolver) {
		this.sqmParameter = sqmParameter;
		this.queryParameter = queryParameter;
		this.queryParameterBindingResolver = queryParameterBindingResolver;

		if ( valueMapping instanceof EntityValuedModelPart ) {
			this.valueMapping = ( (EntityValuedModelPart) valueMapping ).getEntityMappingType().getIdentifierMapping();
		}
		else {
			this.valueMapping = valueMapping;
		}

		assert jdbcParameters != null;
		assert jdbcParameters.size() > 0;

		this.resolvedExpression = determineResolvedExpression( jdbcParameters, this.valueMapping );
	}

	private Expression determineResolvedExpression(List<JdbcParameter> jdbcParameters, MappingModelExpressable valueMapping) {
		if ( valueMapping instanceof EmbeddableValuedModelPart
				|| valueMapping instanceof DiscriminatedAssociationModelPart ) {
			return new SqlTuple( jdbcParameters, valueMapping );
		}

		assert jdbcParameters.size() == 1;
		return jdbcParameters.get( 0 );
	}

	public Expression getResolvedExpression() {
		return resolvedExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		resolvedExpression.accept( sqlTreeWalker );
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return valueMapping;
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		if ( resolvedExpression instanceof SqlTuple ) {
			throw new SemanticException( "Composite query parameter cannot be used in select" );
		}

		AllowableParameterType nodeType = sqmParameter.getNodeType();
		if ( nodeType == null ) {
			final QueryParameterBinding<?> binding = queryParameterBindingResolver.apply( queryParameter );
			nodeType = binding.getBindType();
		}

		final SqlSelection sqlSelection = creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				resolvedExpression,
				nodeType.getExpressableJavaTypeDescriptor(),
				creationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory()
						.getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				nodeType.getExpressableJavaTypeDescriptor()
		);
	}

	@Override
	public SqlTuple getSqlTuple() {
		return resolvedExpression instanceof SqlTuple
				? (SqlTuple) resolvedExpression
				: null;
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		resolveSqlSelection( creationState );
	}

	public SqlSelection resolveSqlSelection(DomainResultCreationState creationState) {
		if ( resolvedExpression instanceof SqlTuple ) {
			throw new SemanticException( "Composite query parameter cannot be used in select" );
		}

		AllowableParameterType nodeType = sqmParameter.getNodeType();
		if ( nodeType == null ) {
			final QueryParameterBinding<?> binding = queryParameterBindingResolver.apply( queryParameter );
			nodeType = binding.getBindType();
		}

		return creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				resolvedExpression,
				nodeType.getExpressableJavaTypeDescriptor(),
				creationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory()
						.getTypeConfiguration()
		);
	}
}
