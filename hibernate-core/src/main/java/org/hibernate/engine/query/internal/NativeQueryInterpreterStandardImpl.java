/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.query.internal;

import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.ParameterRecognizer;

/**
 * @author Steve Ebersole
 */
public class NativeQueryInterpreterStandardImpl implements NativeQueryInterpreter {
	/**
	 * Singleton access
	 */
	public static final NativeQueryInterpreterStandardImpl NATIVE_QUERY_INTERPRETER = new NativeQueryInterpreterStandardImpl( false );

	private final boolean nativeJdbcParametersIgnored;

	public NativeQueryInterpreterStandardImpl(boolean nativeJdbcParametersIgnored) {
		this.nativeJdbcParametersIgnored = nativeJdbcParametersIgnored;
	}

	@Override
	public void recognizeParameters(String nativeQuery, ParameterRecognizer recognizer) {
		ParameterParser.parse( nativeQuery, recognizer, nativeJdbcParametersIgnored );
	}
}
