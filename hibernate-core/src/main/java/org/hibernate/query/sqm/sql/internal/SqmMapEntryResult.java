/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.BitSet;
import java.util.Map;

import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class SqmMapEntryResult<K, V, R extends Map.Entry<K, V>> implements DomainResult<R> {
	private final DomainResult<K> keyResult;
	private final DomainResult<V> valueResult;

	private final JavaType<R> javaType;
	private final String alias;

	public SqmMapEntryResult(
			DomainResult<K> keyResult,
			DomainResult<V> valueResult,
			String alias,
			JavaType<R> javaType) {
		this.alias = alias;
		this.keyResult = keyResult;
		this.valueResult = valueResult;
		this.javaType = javaType;
	}

	@Override
	public String getResultVariable() {
		return alias;
	}

	@Override
	public DomainResultAssembler<R> createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final DomainResultAssembler<K> keyAssembler = keyResult.createResultAssembler( null, creationState );
		final DomainResultAssembler<V> valueAssembler = valueResult.createResultAssembler( null, creationState );

		return new DomainResultAssembler<>() {
			@Override
			public R assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
				final K key = keyAssembler.assemble( rowProcessingState, options );
				final V value = valueAssembler.assemble( rowProcessingState, options );
				//noinspection unchecked
				return (R) Map.entry( key, value );
			}

			@Override
			public JavaType<R> getAssembledJavaType() {
				return javaType;
			}
		};
	}

	@Override
	public JavaType<R> getResultJavaType() {
		return javaType;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		keyResult.collectValueIndexesToCache( valueIndexes );
		valueResult.collectValueIndexesToCache( valueIndexes );
	}
}
