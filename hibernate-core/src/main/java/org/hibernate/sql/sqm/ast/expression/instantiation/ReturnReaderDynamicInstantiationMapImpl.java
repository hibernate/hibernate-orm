/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression.instantiation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingOptions;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;
import org.hibernate.sql.sqm.exec.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class ReturnReaderDynamicInstantiationMapImpl implements ReturnReader {
	private final List<AliasedReturnReader> entryReaders;
	private final int numberOfColumnsConsumed;

	public ReturnReaderDynamicInstantiationMapImpl(
			List<DynamicInstantiationArgument> arguments,
			int startPosition,
			SessionFactoryImplementor sessionFactory) {
		this.entryReaders = new ArrayList<AliasedReturnReader>();
		int numberOfColumnsConsumed = 0;

		for ( DynamicInstantiationArgument argument : arguments ) {
			final ReturnReader reader = argument.getExpression().getReturnReader(
					startPosition+numberOfColumnsConsumed,
					true,
					sessionFactory
			);
			numberOfColumnsConsumed += reader.getNumberOfColumnsRead( sessionFactory );
			entryReaders.add( new AliasedReturnReader( argument.getAlias(), reader ) );
		}

		this.numberOfColumnsConsumed = numberOfColumnsConsumed;
	}

	@Override
	public int getNumberOfColumnsRead(SessionFactoryImplementor sessionFactory) {
		return numberOfColumnsConsumed;
	}

	@Override
	public Class getReturnedJavaType() {
		return Map.class;
	}


	@Override
	public void readBasicValues(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		for ( AliasedReturnReader entryReader : entryReaders ) {
			entryReader.getReturnReader().readBasicValues( processingState, options );
		}
	}

	@Override
	public void resolveBasicValues(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		for ( AliasedReturnReader entryReader : entryReaders ) {
			entryReader.getReturnReader().resolveBasicValues( processingState, options );
		}
	}

	@Override
	public Object assemble(
			RowProcessingState processingState,
			ResultSetProcessingOptions options) throws SQLException {
		final HashMap result = new HashMap();

		for ( AliasedReturnReader entryReader : entryReaders ) {
			result.put(
					entryReader.getAlias(),
					entryReader.getReturnReader().assemble( processingState, options )
			);
		}

		return result;
	}
}
