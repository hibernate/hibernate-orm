/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.util.xml;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;

/**
 * Validation event handler used for obtaining line and column numbers in case of parsing failures.
 *
 * @author Hardy Ferentschik
 */
public class ContextProvidingValidationEventHandler implements ValidationEventHandler {
	private int lineNumber;
	private int columnNumber;
	private String message;

	@Override
	public boolean handleEvent(ValidationEvent validationEvent) {
		ValidationEventLocator locator = validationEvent.getLocator();
		lineNumber = locator.getLineNumber();
		columnNumber = locator.getColumnNumber();
		message = validationEvent.getMessage();
		return false;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public int getColumnNumber() {
		return columnNumber;
	}

	public String getMessage() {
		return message;
	}
}
