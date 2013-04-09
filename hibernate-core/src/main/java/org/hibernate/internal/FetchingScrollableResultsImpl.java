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
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.HolderInstantiator;
import org.hibernate.loader.Loader;
import org.hibernate.type.Type;

/**
 * Implementation of ScrollableResults which can handle collection fetches.
 *
 * @author Steve Ebersole
 */
public class FetchingScrollableResultsImpl extends AbstractScrollableResults {
	private Object[] currentRow;
	private int currentPosition;
	private Integer maxPosition;

	/**
	 * Constructs a FetchingScrollableResultsImpl.
	 *
	 * @param rs The scrollable result set
	 * @param ps The prepared statement used to obtain the result set
	 * @param sess The originating session
	 * @param loader The loader
	 * @param queryParameters query parameters
	 * @param types The result types
	 * @param holderInstantiator Ugh
	 */
	public FetchingScrollableResultsImpl(
	        ResultSet rs,
	        PreparedStatement ps,
	        SessionImplementor sess,
	        Loader loader,
	        QueryParameters queryParameters,
	        Type[] types,
	        HolderInstantiator holderInstantiator) {
		super( rs, ps, sess, loader, queryParameters, types, holderInstantiator );
	}

	@Override
    protected Object[] getCurrentRow() {
		return currentRow;
	}

	@Override
	public boolean next() {
		if ( maxPosition != null && maxPosition <= currentPosition ) {
			currentRow = null;
			currentPosition = maxPosition + 1;
			return false;
		}

		if ( isResultSetEmpty() ) {
			currentRow = null;
			currentPosition = 0;
			return false;
		}

		final Object row = getLoader().loadSequentialRowsForward(
				getResultSet(),
				getSession(),
				getQueryParameters(),
				false
		);


		final boolean afterLast;
		try {
			afterLast = getResultSet().isAfterLast();
		}
		catch( SQLException e ) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
			        e,
			        "exception calling isAfterLast()"
			);
		}

		currentPosition++;
		currentRow = new Object[] { row };

		if ( afterLast ) {
			if ( maxPosition == null ) {
				// we just hit the last position
				maxPosition = currentPosition;
			}
		}

		afterScrollOperation();

		return true;
	}

	@Override
	public boolean previous() {
		if ( currentPosition <= 1 ) {
			currentPosition = 0;
			currentRow = null;
			return false;
		}

		final Object loadResult = getLoader().loadSequentialRowsReverse(
				getResultSet(),
				getSession(),
				getQueryParameters(),
				false,
		        ( maxPosition != null && currentPosition > maxPosition )
		);

		currentRow = new Object[] { loadResult };
		currentPosition--;

		afterScrollOperation();

		return true;
	}

	@Override
	public boolean scroll(int positions) {
		boolean more = false;
		if ( positions > 0 ) {
			// scroll ahead
			for ( int i = 0; i < positions; i++ ) {
				more = next();
				if ( !more ) {
					break;
				}
			}
		}
		else if ( positions < 0 ) {
			// scroll backward
			for ( int i = 0; i < ( 0 - positions ); i++ ) {
				more = previous();
				if ( !more ) {
					break;
				}
			}
		}
		else {
			throw new HibernateException( "scroll(0) not valid" );
		}

		afterScrollOperation();

		return more;
	}

	@Override
	public boolean last() {
		boolean more = false;
		if ( maxPosition != null ) {
			if ( currentPosition > maxPosition ) {
				more = previous();
			}
			for ( int i = currentPosition; i < maxPosition; i++ ) {
				more = next();
			}
		}
		else {
			try {
				if ( isResultSetEmpty() || getResultSet().isAfterLast() ) {
					// should not be able to reach last without maxPosition being set
					// unless there are no results
					return false;
				}

				while ( !getResultSet().isAfterLast() ) {
					more = next();
				}
			}
			catch( SQLException e ) {
				throw getSession().getFactory().getSQLExceptionHelper().convert(
						e,
						"exception calling isAfterLast()"
				);
			}
		}

		afterScrollOperation();

		return more;
	}

	@Override
	public boolean first() {
		beforeFirst();
		boolean more = next();

		afterScrollOperation();

		return more;
	}

	@Override
	public void beforeFirst() {
		try {
			getResultSet().beforeFirst();
		}
		catch( SQLException e ) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
			        e,
			        "exception calling beforeFirst()"
			);
		}
		currentRow = null;
		currentPosition = 0;
	}

	@Override
	public void afterLast() {
		// TODO : not sure the best way to handle this.
		// The non-performant way :
		last();
		next();
		afterScrollOperation();
	}

	@Override
	public boolean isFirst() {
		return currentPosition == 1;
	}

	@Override
	public boolean isLast() {
		return maxPosition != null && currentPosition == maxPosition;
	}

	@Override
	public int getRowNumber() {
		return currentPosition;
	}

	@Override
	public boolean setRowNumber(int rowNumber) {
		if ( rowNumber == 1 ) {
			return first();
		}
		else if ( rowNumber == -1 ) {
			return last();
		}
		else if ( maxPosition != null && rowNumber == maxPosition ) {
			return last();
		}
		return scroll( rowNumber - currentPosition );
	}

	private boolean isResultSetEmpty() {
		try {
			return currentPosition == 0 && ! getResultSet().isBeforeFirst() && ! getResultSet().isAfterLast();
		}
		catch( SQLException e ) {
			throw getSession().getFactory().getSQLExceptionHelper().convert(
			        e,
			        "Could not determine if resultset is empty due to exception calling isBeforeFirst or isAfterLast()"
			);
		}
	}

}
