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
package org.hibernate.impl;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.hql.HolderInstantiator;
import org.hibernate.loader.Loader;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementation of the <tt>ScrollableResults</tt> interface
 * @author Gavin King
 */
public class ScrollableResultsImpl extends AbstractScrollableResults implements ScrollableResults {

	private Object[] currentRow;

	public ScrollableResultsImpl(
	        ResultSet rs,
	        PreparedStatement ps,
	        SessionImplementor sess,
	        Loader loader,
	        QueryParameters queryParameters,
	        Type[] types, HolderInstantiator holderInstantiator) throws MappingException {
		super( rs, ps, sess, loader, queryParameters, types, holderInstantiator );
	}

	protected Object[] getCurrentRow() {
		return currentRow;
	}

	/**
	 * @see org.hibernate.ScrollableResults#scroll(int)
	 */
	public boolean scroll(int i) throws HibernateException {
		try {
			boolean result = getResultSet().relative(i);
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"could not advance using scroll()"
				);
		}
	}

	/**
	 * @see org.hibernate.ScrollableResults#first()
	 */
	public boolean first() throws HibernateException {
		try {
			boolean result = getResultSet().first();
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"could not advance using first()"
				);
		}
	}

	/**
	 * @see org.hibernate.ScrollableResults#last()
	 */
	public boolean last() throws HibernateException {
		try {
			boolean result = getResultSet().last();
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"could not advance using last()"
				);
		}
	}

	/**
	 * @see org.hibernate.ScrollableResults#next()
	 */
	public boolean next() throws HibernateException {
		try {
			boolean result = getResultSet().next();
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"could not advance using next()"
				);
		}
	}

	/**
	 * @see org.hibernate.ScrollableResults#previous()
	 */
	public boolean previous() throws HibernateException {
		try {
			boolean result = getResultSet().previous();
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"could not advance using previous()"
				);
		}
	}

	/**
	 * @see org.hibernate.ScrollableResults#afterLast()
	 */
	public void afterLast() throws HibernateException {
		try {
			getResultSet().afterLast();
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"exception calling afterLast()"
				);
		}
	}

	/**
	 * @see org.hibernate.ScrollableResults#beforeFirst()
	 */
	public void beforeFirst() throws HibernateException {
		try {
			getResultSet().beforeFirst();
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"exception calling beforeFirst()"
				);
		}
	}

	/**
	 * @see org.hibernate.ScrollableResults#isFirst()
	 */
	public boolean isFirst() throws HibernateException {
		try {
			return getResultSet().isFirst();
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"exception calling isFirst()"
				);
		}
	}

	/**
	 * @see org.hibernate.ScrollableResults#isLast()
	 */
	public boolean isLast() throws HibernateException {
		try {
			return getResultSet().isLast();
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"exception calling isLast()"
				);
		}
	}

	public int getRowNumber() throws HibernateException {
		try {
			return getResultSet().getRow()-1;
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"exception calling getRow()"
				);
		}
	}

	public boolean setRowNumber(int rowNumber) throws HibernateException {
		if (rowNumber>=0) rowNumber++;
		try {
			boolean result = getResultSet().absolute(rowNumber);
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					getSession().getFactory().getSQLExceptionConverter(),
					sqle,
					"could not advance using absolute()"
				);
		}
	}

	private void prepareCurrentRow(boolean underlyingScrollSuccessful) 
	throws HibernateException {
		
		if (!underlyingScrollSuccessful) {
			currentRow = null;
			return;
		}

		Object result = getLoader().loadSingleRow(
				getResultSet(),
				getSession(),
				getQueryParameters(),
				false
		);
		if ( result != null && result.getClass().isArray() ) {
			currentRow = (Object[]) result;
		}
		else {
			currentRow = new Object[] { result };
		}

		if ( getHolderInstantiator() != null ) {
			currentRow = new Object[] { getHolderInstantiator().instantiate(currentRow) };
		}

		afterScrollOperation();
	}

}
