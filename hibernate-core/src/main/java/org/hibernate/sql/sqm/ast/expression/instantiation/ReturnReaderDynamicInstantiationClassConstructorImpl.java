/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression.instantiation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingOptions;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;
import org.hibernate.sql.sqm.exec.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class ReturnReaderDynamicInstantiationClassConstructorImpl implements ReturnReader {
	private final Constructor constructor;
	private final List<AliasedReturnReader> argumentReaders;
	private final int numberOfColumnsRead;

	public ReturnReaderDynamicInstantiationClassConstructorImpl(
			Constructor constructor,
			List<AliasedReturnReader> argumentReaders,
			int numberOfColumnsRead) {
		this.constructor = constructor;
		this.argumentReaders = argumentReaders;
		this.numberOfColumnsRead = numberOfColumnsRead;
	}

	@Override
	public void readBasicValues(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		for ( AliasedReturnReader argumentReader : argumentReaders ) {
			argumentReader.getReturnReader().readBasicValues( processingState, options );
		}
	}

	@Override
	public void resolveBasicValues(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		for ( AliasedReturnReader argumentReader : argumentReaders ) {
			argumentReader.getReturnReader().resolveBasicValues( processingState, options );
		}
	}

	@Override
	public Object assemble(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		Object[] args = new Object[ argumentReaders.size() ];
		for ( int i = 0; i < argumentReaders.size(); i++ ) {
			args[i] = argumentReaders.get( i ).getReturnReader().assemble( processingState, options );
		}

		try {
			return constructor.newInstance( args );
		}
		catch (InvocationTargetException e) {
			throw new InstantiationException( "Error performing dynamic instantiation : " + constructor.getDeclaringClass().getName(), e.getCause() );
		}
		catch (Exception e) {
			throw new InstantiationException( "Error performing dynamic instantiation : " + constructor.getDeclaringClass().getName(), e );
		}
	}

	@Override
	public Class getReturnedJavaType() {
		return constructor.getDeclaringClass();
	}

	@Override
	public int getNumberOfColumnsRead(SessionFactoryImplementor sessionFactory) {
		return numberOfColumnsRead;
	}
}
