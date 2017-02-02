/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

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

	@Override
	public void error(SAXParseException error) {
		if ( this.errors == null ) {
			errors = new ArrayList<SAXParseException>();
		}
		errors.add( error );
	}

	@Override
	public void fatalError(SAXParseException error) {
		error( error );
	}

	@Override
	public void warning(SAXParseException warn) {
		LOG.parsingXmlWarning( warn.getLineNumber(), warn.getMessage() );
	}

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
