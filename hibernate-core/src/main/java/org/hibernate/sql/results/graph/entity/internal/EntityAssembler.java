/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EntityAssembler<T> implements DomainResultAssembler<T> {
	private final JavaType<T> javaType;
	private final EntityInitializer<InitializerData> initializer;

	public EntityAssembler(JavaType<T> javaType, EntityInitializer<?> initializer) {
		this.javaType = javaType;
		this.initializer = (EntityInitializer<InitializerData>) initializer;
	}

	@Override
	public JavaType<T> getAssembledJavaType() {
		return javaType;
	}

	@Override
	public T assemble(RowProcessingState rowProcessingState) {
		// Ensure that the instance really is initialized
		// This is important for key-many-to-ones that are part of a collection key fk,
		// as the instance is needed for resolveKey before initializing the instance in RowReader
		final InitializerData data = initializer.getData( rowProcessingState );
		final Initializer.State state = data.getState();
		if ( state == Initializer.State.KEY_RESOLVED ) {
			initializer.resolveInstance( data );
		}
		//noinspection unchecked
		return (T) initializer.getResolvedInstance( data );
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		initializer.resolveState( rowProcessingState );
	}

	@Override
	public EntityInitializer<?> getInitializer() {
		return initializer;
	}

	@Override
	public <X> void forEachResultAssembler(BiConsumer<Initializer<?>, X> consumer, X arg) {
		if ( initializer.isResultInitializer() ) {
			consumer.accept( initializer, arg );
		}
	}
}
