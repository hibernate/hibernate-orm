/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.HolderInstantiator;
import org.hibernate.loader.Loader;
import org.hibernate.type.Type;

/**
 * Standard ScrollableResults implementation.
 *
 * @author Gavin King
 */
public class ScrollableResultsImpl extends AbstractScrollableResults implements ScrollableResults {
	private Object[] currentRow;

	/**
	 * Constructs a ScrollableResultsImpl using the specified information.
	 *
	 * @param rs The scrollable result set
	 * @param ps The prepared statement used to obtain the result set
	 * @param sess The originating session
	 * @param loader The loader
	 * @param queryParameters query parameters
	 * @param types The result types
	 * @param holderInstantiator Ugh
	 */
	public ScrollableResultsImpl(
	        ResultSet rs,
	        PreparedStatement ps,
	        SessionImplementor sess,
	        Loader loader,
	        QueryParameters queryParameters,
	        Type[] types, HolderInstantiator holderInstantiator) {
		super( rs, ps, sess, loader, queryParameters, types, holderInstantiator );
	}

	@Override
	protected Object[] getCurrentRow() {
		return currentRow;
	}

	@Override
	public boolean scroll(int i) {
		try {
			final boolean result = getResultSet().relative(i);
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not advance using scroll()"
			);
		}
	}

	@Override
	public boolean first() {
		try {
			final boolean result = getResultSet().first();
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not advance using first()"
			);
		}
	}

	@Override
	public boolean last() {
		try {
			final boolean result = getResultSet().last();
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not advance using last()"
			);
		}
	}

	@Override
	public boolean next() {
		try {
			final boolean result = getResultSet().next();
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not advance using next()"
			);
		}
	}

	@Override
	public boolean previous() {
		try {
			final boolean result = getResultSet().previous();
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not advance using previous()"
			);
		}
	}

	@Override
	public void afterLast() {
		try {
			getResultSet().afterLast();
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"exception calling afterLast()"
			);
		}
	}

	@Override
	public void beforeFirst() {
		try {
			getResultSet().beforeFirst();
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"exception calling beforeFirst()"
			);
		}
	}

	@Override
	public boolean isFirst() {
		try {
			return getResultSet().isFirst();
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"exception calling isFirst()"
			);
		}
	}

	@Override
	public boolean isLast() {
		try {
			return getResultSet().isLast();
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"exception calling isLast()"
			);
		}
	}

	@Override
	public int getRowNumber() throws HibernateException {
		try {
			return getResultSet().getRow()-1;
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"exception calling getRow()"
			);
		}
	}

	@Override
	public boolean setRowNumber(int rowNumber) throws HibernateException {
		if ( rowNumber >= 0 ) {
			rowNumber++;
		}

		try {
			final boolean result = getResultSet().absolute(rowNumber);
			prepareCurrentRow(result);
			return result;
		}
		catch (SQLException sqle) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not advance using absolute()"
			);
		}
	}

	private void prepareCurrentRow(boolean underlyingScrollSuccessful) {
		if ( !underlyingScrollSuccessful ) {
			currentRow = null;
			return;
		}

		final Object result = getLoader().loadSingleRow(
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
