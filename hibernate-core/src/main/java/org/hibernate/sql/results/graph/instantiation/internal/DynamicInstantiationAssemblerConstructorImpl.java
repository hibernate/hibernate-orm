/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.query.sqm.sql.internal.InstantiationException;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationAssemblerConstructorImpl<R> implements DomainResultAssembler<R> {
	private final Constructor<R> targetConstructor;
	private final JavaType<R> resultType;
	private final List<ArgumentReader<?>> argumentReaders;

	public DynamicInstantiationAssemblerConstructorImpl(
			Constructor<R> targetConstructor,
			JavaType<R> resultType,
			List<ArgumentReader<?>> argumentReaders) {
		this.targetConstructor = targetConstructor;
		this.resultType = resultType;
		this.argumentReaders = argumentReaders;
	}

	@Override
	public JavaType<R> getAssembledJavaType() {
		return resultType;
	}

	@Override
	public R assemble(RowProcessingState rowProcessingState) {
		final int numberOfArgs = argumentReaders.size();
		Object[] args = new Object[ numberOfArgs ];
		for ( int i = 0; i < numberOfArgs; i++ ) {
			args[i] = argumentReaders.get( i ).assemble( rowProcessingState );
		}

		try {
			return targetConstructor.newInstance( args );
		}
		catch (InvocationTargetException e) {
			throw new InstantiationException( "Error instantiating class '"
					+ targetConstructor.getDeclaringClass().getName() + "'", e.getCause() );
		}
		catch (Exception e) {
			throw new InstantiationException( "Error instantiating class '"
					+ targetConstructor.getDeclaringClass().getName() + "'", e );
		}
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		for ( ArgumentReader<?> argumentReader : argumentReaders ) {
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
