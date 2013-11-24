/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.hql.internal.ast;

import org.hibernate.QueryException;

import antlr.RecognitionException;

/**
 * Exception thrown when there is a syntax error in the HQL.
 *
 * @author josh
 */
public class QuerySyntaxException extends QueryException {
	/**
	 * Constructs a QuerySyntaxException
	 *
	 * @param message Message explaining the condition that led to the exception
	 */
	public QuerySyntaxException(String message) {
		super( message );
	}

	/**
	 * Constructs a QuerySyntaxException
	 *
	 * @param message Message explaining the condition that led to the exception
	 * @param hql The hql query that was being parsed/analyzed
	 */
	public QuerySyntaxException(String message, String hql) {
		super( message, hql );
	}

	/**
	 * Intended for use solely from {@link #generateQueryException(String)}
	 *
	 * @param message Message explaining the condition that led to the exception
	 * @param queryString The hql query that was being parsed/analyzed
	 * @param cause The cause, generally another QuerySyntaxException
	 */
	protected QuerySyntaxException(String message, String queryString, Exception cause) {
		super( message, queryString, cause );
	}

	/**
	 * Converts the given ANTLR RecognitionException into a QuerySyntaxException.  The RecognitionException
	 * does not become the cause because ANTLR exceptions are not serializable.
	 *
	 * @param e The ANTLR exception
	 *
	 * @return The QuerySyntaxException
	 */
	public static QuerySyntaxException convert(RecognitionException e) {
		return convert( e, null );
	}

	/**
	 * Converts the given ANTLR RecognitionException into a QuerySyntaxException.  The RecognitionException
	 * does not become the cause because ANTLR exceptions are not serializable.
	 *
	 * @param e The ANTLR exception
	 * @param hql The query string
	 *
	 * @return The QuerySyntaxException
	 */
	public static QuerySyntaxException convert(RecognitionException e, String hql) {
		String positionInfo = e.getLine() > 0 && e.getColumn() > 0
				? " near line " + e.getLine() + ", column " + e.getColumn()
				: "";
		return new QuerySyntaxException( e.getMessage() + positionInfo, hql );
	}

	@Override
	protected QueryException generateQueryException(String queryString) {
		return new QuerySyntaxException( getOriginalMessage(), queryString, this );
	}
}
