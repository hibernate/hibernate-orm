/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import org.hibernate.query.spi.NativeQueryInterpreter;
import org.hibernate.query.spi.ParameterRecognizer;

/**
 * @author Steve Ebersole
 */
public class StandardNativeQueryInterpreter implements NativeQueryInterpreter {
	/**
	 * Singleton access
	 */
	public static final StandardNativeQueryInterpreter INSTANCE = new StandardNativeQueryInterpreter();

	@Override
	public void recognizeParameters(String nativeQuery, ParameterRecognizer recognizer) {
		ParameterParser.parse( nativeQuery, recognizer );
	}
}
