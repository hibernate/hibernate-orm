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
	private final TupleTransformer tupleTransformer;
	private final ResultListTransformer resultListTransformer;
	private final int hash;

	public SelectInterpretationsKey(
			String sql,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Collection<String> querySpaces,
			TupleTransformer tupleTransformer,
			ResultListTransformer resultListTransformer) {
		this.sql = sql;
		this.jdbcValuesMappingProducer = jdbcValuesMappingProducer;
		this.querySpaces = querySpaces;
		this.tupleTransformer = tupleTransformer;
		this.resultListTransformer = resultListTransformer;
		this.hash = generateHashCode();
	}

	private SelectInterpretationsKey(
			String sql,
			JdbcValuesMappingProducer jdbcValuesMappingProducer,
			Collection<String> querySpaces,
			TupleTransformer tupleTransformer,
			ResultListTransformer resultListTransformer,
			int hash) {
		this.sql = sql;
		this.jdbcValuesMappingProducer = jdbcValuesMappingProducer;
		this.querySpaces = querySpaces;
		this.tupleTransformer = tupleTransformer;
		this.resultListTransformer = resultListTransformer;
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
				tupleTransformer,
				resultListTransformer,
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
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final SelectInterpretationsKey that = (SelectInterpretationsKey) o;
		return sql.equals( that.sql )
				&& Objects.equals( jdbcValuesMappingProducer, that.jdbcValuesMappingProducer )
				&& Objects.equals( querySpaces, that.querySpaces )
				&& Objects.equals( tupleTransformer, that.tupleTransformer )
				&& Objects.equals( resultListTransformer, that.resultListTransformer );
	}
}
