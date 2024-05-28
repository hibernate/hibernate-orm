/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import java.util.function.BiConsumer;

import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Specialized QueryResultAssembler for use as a "reader" for dynamic-
 * instantiation arguments.
 *
 * @author Steve Ebersole
 */
public class ArgumentReader<A> implements DomainResultAssembler<A> {
	private final DomainResultAssembler<A> delegateAssembler;
	private final String alias;

	public ArgumentReader(DomainResultAssembler<A> delegateAssembler, String alias) {
		this.delegateAssembler = delegateAssembler;
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	@Override
	public @Nullable A assemble(RowProcessingState rowProcessingState) {
		return delegateAssembler.assemble( rowProcessingState );
	}

	@Override
	public JavaType<A> getAssembledJavaType() {
		return delegateAssembler.getAssembledJavaType();
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		delegateAssembler.resolveState( rowProcessingState );
	}

	@Override
	public @Nullable Initializer<?> getInitializer() {
		return delegateAssembler.getInitializer();
	}

	@Override
	public <X> void forEachResultAssembler(BiConsumer<Initializer<?>, X> consumer, X arg) {
		delegateAssembler.forEachResultAssembler( consumer, arg );
	}
}
