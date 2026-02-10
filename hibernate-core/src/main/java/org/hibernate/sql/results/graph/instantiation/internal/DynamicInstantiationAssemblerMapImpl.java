/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A QueryResultAssembler implementation representing handling for dynamic-
 * instantiations targeting a Map, e.g.`select new map( pers.name, pers.dateOfBirth ) ...`
 *
 * @author Steve Ebersole
 */
public class DynamicInstantiationAssemblerMapImpl implements DomainResultAssembler<Map<?,?>> {
	private final JavaType<Map<?,?>> mapJavaType;
	private final List<ArgumentReader<?>> argumentReaders;

	public DynamicInstantiationAssemblerMapImpl(
			JavaType<Map<?,?>> mapJavaType,
			List<ArgumentReader<?>> argumentReaders) {
		this.mapJavaType = mapJavaType;
		this.argumentReaders = argumentReaders;

		final Set<String> aliases = new HashSet<>();
		for ( var argumentReader : argumentReaders ) {
			if ( argumentReader.getAlias() == null ) {
				throw new IllegalStateException( "alias for Map dynamic instantiation argument cannot be null" );
			}

			if ( ! aliases.add( argumentReader.getAlias() ) ) {
				throw new IllegalStateException( "Encountered duplicate alias for Map dynamic instantiation argument [" + argumentReader.getAlias() + "]" );
			}
		}
	}

	private DynamicInstantiationAssemblerMapImpl(
			List<ArgumentReader<?>> argumentReaders,
			JavaType<Map<?,?>> mapJavaType) {
		this.mapJavaType = mapJavaType;
		this.argumentReaders = argumentReaders;
	}

	@Override
	public JavaType<Map<?,?>> getAssembledJavaType() {
		return mapJavaType;
	}

	@Override
	public Map<?,?> assemble(
			RowProcessingState rowProcessingState) {
		final HashMap<String,Object> result = new HashMap<>();
		for ( var argumentReader : argumentReaders ) {
			result.put( argumentReader.getAlias(),
					argumentReader.assemble( rowProcessingState ) );
		}
		return result;
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		for ( var argumentReader : argumentReaders ) {
			argumentReader.resolveState( rowProcessingState );
		}
	}

	@Override
	public <X> void forEachResultAssembler(BiConsumer<Initializer<?>, X> consumer, X arg) {
		for ( ArgumentReader<?> argumentReader : argumentReaders ) {
			argumentReader.forEachResultAssembler( consumer, arg );
		}
	}
}
