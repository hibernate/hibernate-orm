/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sql.spi.ParameterInterpretation;
import org.hibernate.query.sql.spi.ParameterOccurrence;

import java.util.List;
import java.util.Map;

import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.toSmallList;
import static org.hibernate.internal.util.collections.CollectionHelper.toSmallMap;

/// Standard implementation of ParameterInterpretation
///
/// @author Steve Ebersole
public class ParameterInterpretationImpl implements ParameterInterpretation {
	private final String sqlString;
	private final List<ParameterOccurrence> parameterList;
	private final Map<Integer, QueryParameterImplementor<?>> positionalParameters;
	private final Map<String, QueryParameterImplementor<?>> namedParameters;

	public ParameterInterpretationImpl(ParameterRecognizerImpl parameterRecognizer) {
		this.sqlString = parameterRecognizer.getAdjustedSqlString();
		this.parameterList = toSmallList( parameterRecognizer.getParameterList() );
		this.positionalParameters = toSmallMap( parameterRecognizer.getPositionalQueryParameters() );
		this.namedParameters = toSmallMap( parameterRecognizer.getNamedQueryParameters() );
	}

	public static ParameterInterpretationImpl interpretParameters(
			String sqlString,
			SessionFactoryImplementor sessionFactory) {
		final var parameterRecognizer = new ParameterRecognizerImpl();
		sessionFactory.getQueryEngine()
				.getNativeQueryInterpreter()
				.recognizeParameters( sqlString, parameterRecognizer );
		return new ParameterInterpretationImpl( parameterRecognizer );
	}

	@Override
	public List<ParameterOccurrence> getOrderedParameterOccurrences() {
		return parameterList;
	}

	@Override
	public ParameterMetadataImplementor toParameterMetadata(SharedSessionContractImplementor session1) {
		return isEmpty( positionalParameters ) && isEmpty( namedParameters )
				? ParameterMetadataImpl.EMPTY
				: new ParameterMetadataImpl( positionalParameters, namedParameters );
	}

	@Override
	public String getAdjustedSqlString() {
		return sqlString;
	}

	@Override
	public String toString() {
		final var buffer =
				new StringBuilder( "ParameterInterpretationImpl (" )
						.append( sqlString )
						.append( ") : {" );
		final String lineSeparator = System.lineSeparator();
		if ( isNotEmpty( parameterList ) ) {
			for ( int i = 0, size = parameterList.size(); i < size; i++ ) {
				buffer.append( lineSeparator ).append( "    ," );
			}
			buffer.setLength( buffer.length() - 1 );
		}
		return buffer.append( lineSeparator ).append( "}" ).toString();
	}
}
