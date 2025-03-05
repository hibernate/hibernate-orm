/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import java.util.function.BiConsumer;

import org.hibernate.Incubating;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Responsible for "assembling" a result for inclusion in the domain query
 * result.  "Assembling" the result basically means building the result object
 * (whatever that means for a specific result type) and returning it for
 * injection into the result "row" currently being processed
 *
 * @author Steve Ebersole
 */
@Incubating
public interface DomainResultAssembler<J> {
	/**
	 * The main "assembly" contract.  Assemble the result and return it.
	 */
	@Nullable J assemble(RowProcessingState rowProcessingState);

	/**
	 * The JavaType describing the Java type that this assembler
	 * assembles.
	 */
	JavaType<J> getAssembledJavaType();

	/**
	 * This method is used to resolve the assembler's state, i.e. reading the result values,
	 * with some performance optimization when we don't need the result object itself
	 */
	default void resolveState(RowProcessingState rowProcessingState) {
		assemble( rowProcessingState );
	}

	default @Nullable Initializer<?> getInitializer() {
		return null;
	}

	/**
	 * Invokes the consumer with every initializer part of this assembler that returns {@code true} for
	 * {@link Initializer#isResultInitializer()}.
	 */
	default <X> void forEachResultAssembler(BiConsumer<Initializer<?>, X> consumer, X arg) {

	}

}
