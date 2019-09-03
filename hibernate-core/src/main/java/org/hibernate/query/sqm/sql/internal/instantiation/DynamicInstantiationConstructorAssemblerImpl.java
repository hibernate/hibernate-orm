/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal.instantiation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.hibernate.query.sqm.sql.internal.InstantiationException;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationConstructorAssemblerImpl<R> implements DomainResultAssembler<R> {
	private final Constructor<R> targetConstructor;
	private final JavaTypeDescriptor<R> resultType;
	private final List<ArgumentReader> argumentReaders;

	public DynamicInstantiationConstructorAssemblerImpl(
			Constructor<R> targetConstructor,
			JavaTypeDescriptor<R> resultType,
			List<ArgumentReader> argumentReaders) {
		this.targetConstructor = targetConstructor;
		this.resultType = resultType;
		this.argumentReaders = argumentReaders;
	}

	@Override
	public JavaTypeDescriptor<R> getAssembledJavaTypeDescriptor() {
		return resultType;
	}

	@Override
	public R assemble(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		final int numberOfArgs = argumentReaders.size();
		Object[] args = new Object[ numberOfArgs ];
		for ( int i = 0; i < numberOfArgs; i++ ) {
			args[i] = argumentReaders.get( i ).assemble( rowProcessingState, options );
		}

		try {
			return targetConstructor.newInstance( args );
		}
		catch (InvocationTargetException e) {
			throw new InstantiationException( "Error performing dynamic instantiation : " + targetConstructor.getDeclaringClass().getName(), e.getCause() );
		}
		catch (Exception e) {
			throw new InstantiationException( "Error performing dynamic instantiation : " + targetConstructor.getDeclaringClass().getName(), e );
		}
	}
}
