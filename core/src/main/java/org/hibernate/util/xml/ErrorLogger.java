/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.util.xml;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Implements an {@link ErrorHandler} that mainly just logs errors/warnings.  However, it does track
 * the intial error it encounters and makes it available via {@link #getError}.
 *
 * @author Steve Ebersole
 */
public class ErrorLogger implements ErrorHandler, Serializable {
	private static final Logger log = LoggerFactory.getLogger( ErrorLogger.class );

	private SAXParseException error; // capture the initial error

	/**
	 * Retrieve the initial error encountered, or null if no error was encountered.
	 *
	 * @return The initial error, or null if none.
	 */
	public SAXParseException getError() {
		return error;
	}

	/**
	 * {@inheritDoc}
	 */
	public void error(SAXParseException error) {
		log.error( "Error parsing XML (" + error.getLineNumber() + ") : " + error.getMessage() );
		if ( this.error == null ) {
			this.error = error;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void fatalError(SAXParseException error) {
		error( error );
	}

	/**
	 * {@inheritDoc}
	 */
	public void warning(SAXParseException warn) {
		log.error( "Warning parsing XML (" + error.getLineNumber() + ") : " + error.getMessage() );
	}

	public void reset() {
		error = null;
	}
}
