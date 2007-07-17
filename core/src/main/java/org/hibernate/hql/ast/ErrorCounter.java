// $Id: ErrorCounter.java 9242 2006-02-09 12:37:36Z steveebersole $
package org.hibernate.hql.ast;

import antlr.RecognitionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.QueryException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An error handler that counts parsing errors and warnings.
 */
public class ErrorCounter implements ParseErrorHandler {
	private Log log = LogFactory.getLog( ErrorCounter.class );
	private Log hqlLog = LogFactory.getLog( "org.hibernate.hql.PARSER" );

	private List errorList = new ArrayList();
	private List warningList = new ArrayList();
	private List recognitionExceptions = new ArrayList();

	public void reportError(RecognitionException e) {
		reportError( e.toString() );
		recognitionExceptions.add( e );
		if ( log.isDebugEnabled() ) {
			log.debug( e, e );
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
