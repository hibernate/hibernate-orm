package org.hibernate.query.sqm.internal;

import jakarta.annotation.Nullable;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import java.util.List;
import java.util.Map;

/**
 * @since 7.1
 */
public record CacheableSqmInterpretation<S extends Statement, J extends JdbcOperation>(
		S statement,
		J jdbcOperation,
		Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref,
		Map<SqmParameter<?>, MappingModelExpressible<?>> sqmParameterMappingModelTypes,
		@Nullable DomainParameterXref domainParameterXref) {

	public CacheableSqmInterpretation(
			S statement, J jdbcOperation,
			Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref,
			Map<SqmParameter<?>, MappingModelExpressible<?>> sqmParameterMappingModelTypes) {
		this( statement, jdbcOperation, jdbcParamsXref, sqmParameterMappingModelTypes, null );
	}

	public DomainParameterXref parameterXrefOr(DomainParameterXref fallback) {
		return domainParameterXref == null ? fallback : domainParameterXref;
	}

	public boolean matches(QueryParameterBindings bindings) {
		return domainParameterXref == null || domainParameterXref.matches( bindings );
	}
}
