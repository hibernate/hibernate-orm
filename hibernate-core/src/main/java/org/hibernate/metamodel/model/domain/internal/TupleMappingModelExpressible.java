/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.ArrayList;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;

/**
 * @author Christian Beikov
 */
public class TupleMappingModelExpressible implements MappingModelExpressible {

	private final MappingModelExpressible<Object>[] components;
	private final JdbcMapping[] mappings;

	public TupleMappingModelExpressible(MappingModelExpressible<?>[] components) {
		this.components = (MappingModelExpressible<Object>[]) components;
		final ArrayList<JdbcMapping> results = new ArrayList<>();
		forEachJdbcType( 0, (index, jdbcMapping) -> results.add( jdbcMapping ) );
		this.mappings = results.toArray( new JdbcMapping[0] );
	}

	@Override
	public JdbcMapping getJdbcMapping(final int index) {
		return mappings[ index ];
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = 0;
		for ( int i = 0; i < components.length; i++ ) {
			span += components[i].forEachJdbcType( offset + span, action );
		}
		return span;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		final Object[] disassembled = new Object[components.length];
		final Object[] array = (Object[]) value;
		for ( int i = 0; i < components.length; i++ ) {
			disassembled[i] = components[i].disassemble( array[i], session );
		}
		return disassembled;
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			for ( int i = 0; i < components.length; i++ ) {
				components[i].addToCacheKey( cacheKey, null, session );
			}
		}
		else {
			final Object[] array = (Object[]) value;
			for ( int i = 0; i < components.length; i++ ) {
				components[i].addToCacheKey( cacheKey, array[i], session );
			}
		}
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;
		if ( value == null ) {
			for ( int i = 0; i < components.length; i++ ) {
				span += components[i].forEachDisassembledJdbcValue(
						null,
						span + offset,
						x,
						y,
						valuesConsumer,
						session
				);
			}
		}
		else {
			final Object[] values = (Object[]) value;
			for ( int i = 0; i < components.length; i++ ) {
				span += components[i].forEachDisassembledJdbcValue(
						values[i],
						span + offset,
						x,
						y,
						valuesConsumer,
						session
				);
			}
		}
		return span;
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;
		if ( value == null ) {
			for ( int i = 0; i < components.length; i++ ) {
				span += components[i].forEachDisassembledJdbcValue(
						components[i].disassemble( null, session ),
						span + offset,
						x, y, valuesConsumer,
						session
				);
			}
		}
		else {
			final Object[] values = (Object[]) value;
			for ( int i = 0; i < components.length; i++ ) {
				span += components[i].forEachDisassembledJdbcValue(
						components[i].disassemble( values[i], session ),
						span + offset,
						x, y, valuesConsumer,
						session
				);
			}
		}
		return span;
	}
}
