/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util.xml;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.ValidationEventLocator;

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
