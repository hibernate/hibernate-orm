/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

/**
 * @author Steve Ebersole
 */
public class SelectInterpretationsKey implements QueryInterpretationCache.Key {
	private final String sql;
	private final ResultSetMapping resultSetMapping;
	private final Collection<String> querySpaces;
	private final int parameterStartPosition;
	private final int hash;

	@Deprecated(forRemoval = true)
	public SelectInterpretationsKey(
			String sql,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Collection<String> querySpaces,
			TupleTransformer<?> tupleTransformer,
			ResultListTransformer<?> resultListTransformer) {
		this( sql, jdbcValuesMappingProducer, querySpaces );
	}

	@Deprecated(forRemoval = true)
	public SelectInterpretationsKey(
			String sql,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Collection<String> querySpaces) {
		this( sql, (ResultSetMapping) jdbcValuesMappingProducer, querySpaces, 1 );
	}

	public SelectInterpretationsKey(
			String sql,
			ResultSetMapping jdbcValuesMappingProducer,
			Collection<String> querySpaces,
			int parameterStartPosition) {
		this.sql = sql;
		this.resultSetMapping = jdbcValuesMappingProducer;
		this.querySpaces = querySpaces;
		this.parameterStartPosition = parameterStartPosition;
		this.hash = generateHashCode();
	}

	private SelectInterpretationsKey(
			String sql,
			ResultSetMapping resultSetMapping,
			Collection<String> querySpaces,
			int parameterStartPosition,
			int hash) {
		this.sql = sql;
		this.resultSetMapping = resultSetMapping;
		this.querySpaces = querySpaces;
		this.parameterStartPosition = parameterStartPosition;
		this.hash = hash;
	}

	@Override
	public String getQueryString() {
		return sql;
	}

	public ResultSetMapping getResultSetMapping() {
		return resultSetMapping;
	}

	public int getStartPosition() {
		return parameterStartPosition;
	}

	@Override
	public QueryInterpretationCache.Key prepareForStore() {
		return new SelectInterpretationsKey(
				sql,
				resultSetMapping.cacheKeyInstance(),
				new HashSet<>( querySpaces ),
				parameterStartPosition,
				hash
		);
	}

	private int generateHashCode() {
		return sql.hashCode();
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SelectInterpretationsKey that) ) {
			return false;
		}
		return sql.equals( that.sql )
			&& Objects.equals( resultSetMapping, that.resultSetMapping )
			&& Objects.equals( querySpaces, that.querySpaces )
			&& parameterStartPosition == that.parameterStartPosition;
	}
}
