/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
public class EntityAssembler implements DomainResultAssembler {
	private final JavaType javaType;
	private final EntityInitializer<InitializerData> initializer;

	public EntityAssembler(JavaType javaType, EntityInitializer<?> initializer) {
		this.javaType = javaType;
		this.initializer = (EntityInitializer<InitializerData>) initializer;
	}

	@Override
	public JavaType getAssembledJavaType() {
		return javaType;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState) {
		// Ensure that the instance really is initialized
		// This is important for key-many-to-ones that are part of a collection key fk,
		// as the instance is needed for resolveKey before initializing the instance in RowReader
		final InitializerData data = initializer.getData( rowProcessingState );
		final Initializer.State state = data.getState();
		if ( state == Initializer.State.KEY_RESOLVED ) {
			initializer.resolveInstance( data );
		}
		return initializer.getResolvedInstance( data );
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
	public void forEachResultAssembler(BiConsumer consumer, Object arg) {
		if ( initializer.isResultInitializer() ) {
			consumer.accept( initializer, arg );
		}
	}
}
