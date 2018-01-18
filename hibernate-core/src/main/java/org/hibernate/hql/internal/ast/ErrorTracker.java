/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

import antlr.RecognitionException;

/**
 * An error handler that counts parsing errors and warnings.
 */
public class ErrorTracker implements ParseErrorHandler {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ErrorTracker.class.getName()
	);

	private final String hql;

	private List<String> errorList = new ArrayList<>();
	private List<RecognitionException> recognitionExceptions = new ArrayList<>();

	/**
	 * Constructs an ErrorCounter without knowledge of the HQL, meaning that generated QueryException
	 * instances *will not* contain the HQL (and will need to be wrapped at a higher level in another
	 * QueryException).
	 */
	@SuppressWarnings("WeakerAccess")
	public ErrorTracker() {
		this( null );
	}

	/**
	 * Constructs an ErrorCounter with knowledge of the HQL, meaning that generated QueryException
	 * instances *will* contain the HQL.
	 */
	@SuppressWarnings("WeakerAccess")
	public ErrorTracker(String hql) {
		this.hql = hql;
	}

	@Override
	public void reportError(RecognitionException e) {
		reportError( e.toString() );
		recognitionExceptions.add( e );
		LOG.error( e.toString(), e );
	}

	@Override
	public void reportError(String message) {
		LOG.error( message );
		errorList.add( message );
	}

	@Override
	public int getErrorCount() {
		return errorList.size();
	}

	@Override
	public void reportWarning(String message) {
		LOG.debug( message );
	}

	private String getErrorString() {
		final StringBuilder buf = new StringBuilder();
		final Iterator<String> iterator = errorList.iterator();
		while ( iterator.hasNext() ) {
			buf.append( iterator.next() );
			if ( iterator.hasNext() ) {
				buf.append( "\n" );
			}

		}
		return buf.toString();
	}

	@Override
	public void throwQueryException() throws QueryException {
		if ( getErrorCount() > 0 ) {
		if ( recognitionExceptions.size() > 0 ) {
				throw QuerySyntaxException.convert( recognitionExceptions.get( 0 ), hql );
		}
			throw new QueryException( getErrorString(), hql );
		}
		LOG.debug( "throwQueryException() : no errors" );
	}
}
