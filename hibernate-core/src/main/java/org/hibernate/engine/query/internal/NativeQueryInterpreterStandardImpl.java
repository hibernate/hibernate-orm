/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
