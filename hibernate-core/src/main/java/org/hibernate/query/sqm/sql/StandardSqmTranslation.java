/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.JdbcParameterMetadata;
import org.hibernate.sql.ast.internal.JdbcParameterMetadataImpl;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * @author Christian Beikov
 */
public class StandardSqmTranslation<T extends Statement> implements SqmTranslation<T> {

	private final T sqlAst;
	private final Map<SqmParameter<?>, List<List<JdbcParameter>>> jdbcParamMap;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> parameterMappingModelTypeMap;
	private final SqlExpressionResolver sqlExpressionResolver;
	private final FromClauseAccess fromClauseAccess;
	private final JdbcParameterMetadata parameterInfo;

	public StandardSqmTranslation(
			T sqlAst,
			Map<SqmParameter<?>, List<List<JdbcParameter>>> jdbcParamMap,
			Map<SqmParameter<?>, MappingModelExpressible<?>> parameterMappingModelTypeMap,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess) {
		this.sqlAst = sqlAst;
		this.jdbcParamMap = jdbcParamMap;
		this.parameterMappingModelTypeMap = parameterMappingModelTypeMap;
		this.sqlExpressionResolver = sqlExpressionResolver;
		this.fromClauseAccess = fromClauseAccess;
		final IdentityHashMap<JdbcParameter, Integer> parameterIdMap = new IdentityHashMap<>();
		int parameterId = 0;
		for ( Map.Entry<SqmParameter<?>, List<List<JdbcParameter>>> entry : jdbcParamMap.entrySet() ) {
			final List<List<JdbcParameter>> parameterUses = entry.getValue();
			final int baseId = parameterId;
			for ( List<JdbcParameter> jdbcParameters : parameterUses ) {
				for ( int i = 0; i < jdbcParameters.size(); i++ ) {
					parameterIdMap.put( jdbcParameters.get( i ), baseId + i );
				}
			}
			parameterId += parameterUses.get( 0 ).size();
		}
		this.parameterInfo = new JdbcParameterMetadataImpl( Collections.unmodifiableMap( parameterIdMap ), parameterId );
	}

	@Override
	public T getSqlAst() {
		return sqlAst;
	}

	@Override
	public Map<SqmParameter<?>, List<List<JdbcParameter>>> getJdbcParamsBySqmParam() {
		return jdbcParamMap;
	}

	@Override
	public Map<SqmParameter<?>, MappingModelExpressible<?>> getSqmParameterMappingModelTypeResolutions() {
		return parameterMappingModelTypeMap;
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return sqlExpressionResolver;
	}

	@Override
	public FromClauseAccess getFromClauseAccess() {
		return fromClauseAccess;
	}

	@Override
	public JdbcParameterMetadata getParameterInfo() {
		return parameterInfo;
	}
}
