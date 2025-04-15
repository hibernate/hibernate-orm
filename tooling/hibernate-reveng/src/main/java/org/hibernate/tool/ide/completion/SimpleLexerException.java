/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.ide.completion;

import java.io.ObjectStreamClass;

/**
 * Exception that can be thrown when the lexer encounters errors (such as syntax errors etc.)
 * 
 * @author Max Rydahl Andersen
 *
 */
public class SimpleLexerException extends RuntimeException {

	private static final long serialVersionUID = 
			ObjectStreamClass.lookup(SimpleLexerException.class).getSerialVersionUID();
	
	public SimpleLexerException() {
		super();
	}

	public SimpleLexerException(String message, Throwable cause) {
		super( message, cause );
	}

	public SimpleLexerException(String message) {
		super( message );
	}

	public SimpleLexerException(Throwable cause) {
		super( cause );
	}

}
