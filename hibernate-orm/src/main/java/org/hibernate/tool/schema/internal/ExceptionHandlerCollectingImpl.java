/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;

/**
 * @author Steve Ebersole
 */
public class ExceptionHandlerCollectingImpl implements ExceptionHandler {
	private final List<CommandAcceptanceException> exceptions = new ArrayList<CommandAcceptanceException>();

	public ExceptionHandlerCollectingImpl() {
	}

	@Override
	public void handleException(CommandAcceptanceException exception) {
		exceptions.add( exception );
	}

	public List<CommandAcceptanceException> getExceptions() {
		return exceptions;
	}
}
