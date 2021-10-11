/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.Map;

import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class SqmMapEntryResult<K, V, R extends Map.Entry<K, V>> implements DomainResult<R> {
	private final DomainResult<K> keyResult;
	private final DomainResult<V> valueResult;

	private final JavaType<R> javaTypeDescriptor;
	private final String alias;

	public SqmMapEntryResult(
			DomainResult<K> keyResult,
			DomainResult<V> valueResult,
			String alias,
			JavaType<R> javaTypeDescriptor) {
		this.alias = alias;
		this.keyResult = keyResult;
		this.valueResult = valueResult;
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public String getResultVariable() {
		return alias;
	}

	@Override
	public DomainResultAssembler<R> createResultAssembler(AssemblerCreationState creationState) {
		final DomainResultAssembler<K> keyAssembler = keyResult.createResultAssembler( creationState );
		final DomainResultAssembler<V> valueAssembler = valueResult.createResultAssembler( creationState );

		return new DomainResultAssembler<R>() {
			@Override
			public R assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
				final K key = keyAssembler.assemble( rowProcessingState, options );
				final V value = valueAssembler.assemble( rowProcessingState, options );
				//noinspection unchecked
				return (R) new MapEntryImpl<>( key, value );
			}

			@Override
			public JavaType<R> getAssembledJavaTypeDescriptor() {
				return javaTypeDescriptor;
			}
		};
	}

	@Override
	public JavaType<R> getResultJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public static class MapEntryImpl<K,V> implements Map.Entry<K,V> {
		private final K key;
		private final V value;

		public MapEntryImpl(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}
	}
}
