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
package org.hibernate;


/**
 * A problem occurred translating a Hibernate query to SQL
 * due to invalid query syntax, etc.
 */
public class QueryException extends HibernateException {

	private String queryString;

	public QueryException(String message) {
		super(message);
	}
	public QueryException(String message, Throwable e) {
		super(message, e);
	}

	public QueryException(String message, String queryString) {
		super(message);
		this.queryString = queryString;
	}

	public QueryException(Exception e) {
		super(e);
	}
	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public String getMessage() {
		String msg = super.getMessage();
		if ( queryString!=null ) msg += " [" + queryString + ']';
		return msg;
	}

}







