/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Locale;

import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * @author Steve Ebersole
 */
public class ExceptionHandlerHaltImpl implements ExceptionHandler {
	/**
	 * Singleton access
	 */
	public static final ExceptionHandlerHaltImpl INSTANCE = new ExceptionHandlerHaltImpl();

	@Override
	public void handleException(CommandAcceptanceException exception) {
		throw new SchemaManagementException(
				String.format(
						Locale.ROOT,
						"Halting on error : %s",
						exception.getMessage()
				),
				exception
		);
	}
}
