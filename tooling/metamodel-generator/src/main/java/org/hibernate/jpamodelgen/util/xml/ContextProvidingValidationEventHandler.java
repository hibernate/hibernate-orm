/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.jpamodelgen.util.xml;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;

/**
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


