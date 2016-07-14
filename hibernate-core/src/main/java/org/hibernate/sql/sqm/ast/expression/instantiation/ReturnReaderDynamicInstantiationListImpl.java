/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression.instantiation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingOptions;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;
import org.hibernate.sql.sqm.exec.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class ReturnReaderDynamicInstantiationListImpl implements ReturnReader<List> {
	private final List<ReturnReader> argumentReaders;
	private final int startPosition;
	private final int numberOfColumnsConsumed;

	public ReturnReaderDynamicInstantiationListImpl(
			List<DynamicInstantiationArgument> arguments,
			int startPosition,
			SessionFactoryImplementor sessionFactory) {
		this.startPosition = startPosition;
		int numberOfColumnsConsumed = 0;

		this.argumentReaders = new ArrayList<ReturnReader>();
		for ( DynamicInstantiationArgument argument : arguments ) {
			ReturnReader argumentReader = argument.getExpression().getReturnReader( startPosition+numberOfColumnsConsumed, true, sessionFactory );
			argumentReaders.add( argumentReader );
			numberOfColumnsConsumed += argumentReader.getNumberOfColumnsRead( sessionFactory );
		}

		this.numberOfColumnsConsumed = numberOfColumnsConsumed;
	}

	@Override
	public Class<List> getReturnedJavaType() {
		return List.class;
	}

	@Override
	public int getNumberOfColumnsRead(SessionFactoryImplementor sessionFactory) {
		return numberOfColumnsConsumed;
	}

	@Override
	public void readBasicValues(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		for ( ReturnReader argumentReader : argumentReaders ) {
			argumentReader.readBasicValues( processingState, options );
		}
	}

	@Override
	public void resolveBasicValues(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		for ( ReturnReader argumentReader : argumentReaders ) {
			argumentReader.resolveBasicValues( processingState, options );
		}
	}

	@Override
	public List assemble(
			RowProcessingState processingState, ResultSetProcessingOptions options) throws SQLException {
		final ArrayList result = new ArrayList();
		for ( ReturnReader argumentReader : argumentReaders ) {
			result.add( argumentReader.assemble( processingState, options ) );
		}
		return result;
	}
}
