/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ExceptionHandlerLoggedImpl implements ExceptionHandler {
	private static final Logger log = Logger.getLogger( ExceptionHandlerLoggedImpl.class );

	/**
	 * Singleton access
	 */
	public static final ExceptionHandlerLoggedImpl INSTANCE = new ExceptionHandlerLoggedImpl();

	@Override
	public void handleException(CommandAcceptanceException exception) {
		log.warnf(
				exception,
				"GenerationTarget encountered exception accepting command : %s",
				exception.getMessage()
		);
	}
}
