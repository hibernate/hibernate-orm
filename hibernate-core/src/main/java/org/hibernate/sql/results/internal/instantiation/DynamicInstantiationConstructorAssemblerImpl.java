/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.instantiation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationConstructorAssemblerImpl implements DomainResultAssembler {
	private final Constructor targetConstructor;
	private final JavaTypeDescriptor resultType;
	private final List<ArgumentReader> argumentReaders;

	public DynamicInstantiationConstructorAssemblerImpl(
			Constructor targetConstructor,
			JavaTypeDescriptor resultType,
			List<ArgumentReader> argumentReaders) {
		this.targetConstructor = targetConstructor;
		this.resultType = resultType;
		this.argumentReaders = argumentReaders;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return resultType;
	}

	@Override
	public Object assemble(
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
