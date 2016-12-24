/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.internal.instantiation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;

/**
 * @author Steve Ebersole
 */
public class ReturnAssemblerConstructorImpl implements ReturnAssembler {
	private final Constructor targetConstructor;
	private final List<ArgumentReader> argumentReaders;

	public ReturnAssemblerConstructorImpl(
			Constructor targetConstructor,
			List<ArgumentReader> argumentReaders) {
		this.targetConstructor = targetConstructor;
		this.argumentReaders = argumentReaders;
	}

	@Override
	public Class getReturnedJavaType() {
		return targetConstructor.getDeclaringClass();
	}

	@Override
	public Object assemble(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) throws SQLException {
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
