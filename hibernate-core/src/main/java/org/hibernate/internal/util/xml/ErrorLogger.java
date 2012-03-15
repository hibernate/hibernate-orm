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
package org.hibernate.internal.util.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import org.hibernate.internal.CoreMessageLogger;

/**
 * Implements an {@link ErrorHandler} that mainly just logs errors/warnings.  However, it does track
 * the errors it encounters and makes them available via {@link #getErrors}.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class ErrorLogger implements ErrorHandler, Serializable {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ErrorLogger.class.getName()
	);

	// lazily initalized
	private List<SAXParseException> errors;
	private String file;

	public ErrorLogger() {
	}

	public ErrorLogger(String file) {
		this.file = file;
	}

	/**
	 * {@inheritDoc}
	 */
	public void error(SAXParseException error) {
		if ( this.errors == null ) {
			errors = new ArrayList<SAXParseException>();
		}
		errors.add( error );
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
		LOG.parsingXmlWarning( warn.getLineNumber(), warn.getMessage() );
	}

	/**
	 * @return returns a list of encountered xml parsing errors, or the empty list if there was no error
	 */
	public List<SAXParseException> getErrors() {
		return errors;
	}

	public void reset() {
		errors = null;
	}

	public boolean hasErrors() {
		return errors != null && errors.size() > 0;
	}

	public void logErrors() {
		if ( errors != null ) {
			for ( SAXParseException e : errors ) {
				if ( file == null ) {
					LOG.parsingXmlError( e.getLineNumber(), e.getMessage() );
				}
				else {
					LOG.parsingXmlErrorForFile( file, e.getLineNumber(), e.getMessage() );
				}
			}
		}
	}
}
