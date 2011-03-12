/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.hql.ast;

import antlr.RecognitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.QueryException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An error handler that counts parsing errors and warnings.
 */
public class ErrorCounter implements ParseErrorHandler {
	private Logger log = LoggerFactory.getLogger( ErrorCounter.class );
	private Logger hqlLog = LoggerFactory.getLogger( "org.hibernate.hql.PARSER" );

	private List errorList = new ArrayList();
	private List warningList = new ArrayList();
	private List recognitionExceptions = new ArrayList();

	public void reportError(RecognitionException e) {
		reportError( e.toString() );
		recognitionExceptions.add( e );
		if ( log.isDebugEnabled() ) {
			log.debug( e.toString(), e );
		}
	}

	public void reportError(String message) {
		hqlLog.error( message );
		errorList.add( message );
	}

	public int getErrorCount() {
		return errorList.size();
	}

	public void reportWarning(String message) {
		hqlLog.debug( message );
		warningList.add( message );
	}

	private String getErrorString() {
		StringBuffer buf = new StringBuffer();
		for ( Iterator iterator = errorList.iterator(); iterator.hasNext(); ) {
			buf.append( ( String ) iterator.next() );
			if ( iterator.hasNext() ) buf.append( "\n" );

		}
		return buf.toString();
	}

	public void throwQueryException() throws QueryException {
		if ( getErrorCount() > 0 ) {
			if ( recognitionExceptions.size() > 0 ) {
				throw QuerySyntaxException.convert( ( RecognitionException ) recognitionExceptions.get( 0 ) );
			}
			else {
				throw new QueryException( getErrorString() );
			}
		}
		else {
			// all clear
			if ( log.isDebugEnabled() ) {
				log.debug( "throwQueryException() : no errors" );
			}
		}
	}
}
