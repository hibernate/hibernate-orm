/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Mainly intended for cases where we have a converter for a literal,
 * but not a full ConvertibleModelPart.
 *
 * @author Steve Ebersole
 */
public class ConvertedQueryLiteral<D,R> implements Literal, DomainResultProducer<D> {
	private final D domainLiteralValue;
	private final R relationalLiteralValue;
	private final BasicValueConverter<D,R> converter;
	private final BasicValuedMapping relationalMapping;

	public ConvertedQueryLiteral(
			D domainLiteralValue,
			BasicValueConverter<D, R> converter,
			BasicValuedMapping relationalMapping) {
		this.domainLiteralValue = domainLiteralValue;
		this.converter = converter;
		this.relationalMapping = relationalMapping;
		this.relationalLiteralValue = converter.toRelationalValue( domainLiteralValue );
	}

	@Override
	public Object getLiteralValue() {
		return relationalLiteralValue;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return relationalMapping.getJdbcMapping();
	}

	@Override
	public DomainResult<D> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		applySqlSelections( creationState );
		return new ConstantDomainResult<>( domainLiteralValue, converter.getDomainJavaType(), resultVariable );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		// if there is another DomainResultProducer that generates sql-selections,
		//		we actually would not even need to generate this.  we do not know that
		//		here.
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver expressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		expressionResolver.resolveSqlSelection(
				this,
				relationalMapping.getExpressibleJavaType(),
				sqlAstCreationState.getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public void bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) throws SQLException {
		//noinspection unchecked
		relationalMapping.getJdbcMapping().getJdbcValueBinder().bind(
				statement,
				relationalLiteralValue,
				startPosition,
				executionContext.getSession()
		);
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitConvertedQueryLiteral( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return relationalMapping.getJdbcMapping();
	}


	private static class ConstantDomainResult<D> implements DomainResult<D>, DomainResultAssembler<D> {
		private final D literal;
		private final JavaType<D> javaType;
		private final String resultAlias;

		public ConstantDomainResult(D literal, JavaType<D> javaType, String resultAlias) {
			this.literal = literal;
			this.javaType = javaType;
			this.resultAlias = resultAlias;
		}

		@Override
		public String getResultVariable() {
			return resultAlias;
		}

		@Override
		public JavaType<?> getResultJavaType() {
			return javaType;
		}

		@Override
		public DomainResultAssembler<D> createResultAssembler(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
			return this;
		}

		@Override
		public D assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
			return literal;
		}

		@Override
		public JavaType<D> getAssembledJavaType() {
			return javaType;
		}
	}
}
