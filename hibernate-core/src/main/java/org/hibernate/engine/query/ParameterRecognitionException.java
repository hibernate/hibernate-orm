/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query;

import org.hibernate.HibernateException;
import org.hibernate.engine.query.spi.ParamLocationRecognizer;

/**
 * Indicates a problem during parameter recognition via
 * {@link ParamLocationRecognizer}
 *
 * @author Steve Ebersole
 */
public class ParameterRecognitionException extends HibernateException {
	public ParameterRecognitionException(String message) {
		super( message );
	}

	public ParameterRecognitionException(String message, Throwable cause) {
		super( message, cause );
	}
}
