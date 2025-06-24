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
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

/**
 * @author Steve Ebersole
 */
public class SelectInterpretationsKey implements QueryInterpretationCache.Key {
	private final String sql;
	private final JdbcValuesMappingProducer jdbcValuesMappingProducer;
	private final Collection<String> querySpaces;
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

	public SelectInterpretationsKey(
			String sql,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Collection<String> querySpaces) {
		this.sql = sql;
		this.jdbcValuesMappingProducer = jdbcValuesMappingProducer;
		this.querySpaces = querySpaces;
		this.hash = generateHashCode();
	}

	private SelectInterpretationsKey(
			String sql,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Collection<String> querySpaces,
			int hash) {
		this.sql = sql;
		this.jdbcValuesMappingProducer = jdbcValuesMappingProducer;
		this.querySpaces = querySpaces;
		this.hash = hash;
	}

	@Override
	public String getQueryString() {
		return sql;
	}

	@Override
	public QueryInterpretationCache.Key prepareForStore() {
		return new SelectInterpretationsKey(
				sql,
				jdbcValuesMappingProducer.cacheKeyInstance(),
				new HashSet<>( querySpaces ),
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
			&& Objects.equals( jdbcValuesMappingProducer, that.jdbcValuesMappingProducer )
			&& Objects.equals( querySpaces, that.querySpaces );
	}
}
