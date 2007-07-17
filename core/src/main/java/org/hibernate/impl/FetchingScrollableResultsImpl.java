// $Id: FetchingScrollableResultsImpl.java 7469 2005-07-14 13:12:19Z steveebersole $
package org.hibernate.impl;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.hql.HolderInstantiator;
import org.hibernate.type.Type;
import org.hibernate.loader.Loader;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.QueryParameters;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Implementation of ScrollableResults which can handle collection fetches.
 *
 * @author Steve Ebersole
 */
public class FetchingScrollableResultsImpl extends AbstractScrollableResults {

	public FetchingScrollableResultsImpl(
	        ResultSet rs,
	        PreparedStatement ps,
	        SessionImplementor sess,
	        Loader loader,
	        QueryParameters queryParameters,
	        Type[] types,
	        HolderInstantiator holderInstantiator) throws MappingException {
		super( rs, ps, sess, loader, queryParameters, types, holderInstantiator );
	}

	private Object[] currentRow = null;
	private int currentPosition = 0;
	private Integer maxPosition = null;

	protected Object[] getCurrentRow() {
		return currentRow;
	}

	/**
	 * Advance to the next result
	 *
	 * @return <tt>true</tt> if there is another result
	 */
	public boolean next() throws HibernateException {
		if ( maxPosition != null && maxPosition.intValue() <= currentPosition ) {
			currentRow = null;
			currentPosition = maxPosition.intValue() + 1;
			return false;
		}

		Object row = getLoader().loadSequentialRowsForward(
				getResultSet(),
				getSession(),
				getQueryParameters(),
				false
		);


		boolean afterLast;
		try {
			afterLast = getResultSet().isAfterLast();
		}
		catch( SQLException e ) {
			throw JDBCExceptionHelper.convert(
			        getSession().getFactory().getSQLExceptionConverter(),
			        e,
			        "exception calling isAfterLast()"
				);
		}

		currentPosition++;
		currentRow = new Object[] { row };

		if ( afterLast ) {
			if ( maxPosition == null ) {
				// we just hit the last position
				maxPosition = new Integer( currentPosition );
			}
		}

		afterScrollOperation();

		return true;
	}

	/**
	 * Retreat to the previous result
	 *
	 * @return <tt>true</tt> if there is a previous result
	 */
	public boolean previous() throws HibernateException {
		if ( currentPosition <= 1 ) {
			currentPosition = 0;
			currentRow = null;
			return false;
		}

		Object loadResult = getLoader().loadSequentialRowsReverse(
				getResultSet(),
				getSession(),
				getQueryParameters(),
				false,
		        ( maxPosition != null && currentPosition > maxPosition.intValue() )
		);

		currentRow = new Object[] { loadResult };
		currentPosition--;

		afterScrollOperation();

		return true;

	}

	/**
	 * Scroll an arbitrary number of locations
	 *
	 * @param positions a positive (forward) or negative (backward) number of rows
	 *
	 * @return <tt>true</tt> if there is a result at the new location
	 */
	public boolean scroll(int positions) throws HibernateException {
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

	/**
	 * Go to the last result
	 *
	 * @return <tt>true</tt> if there are any results
	 */
	public boolean last() throws HibernateException {
		boolean more = false;
		if ( maxPosition != null ) {
			for ( int i = currentPosition; i < maxPosition.intValue(); i++ ) {
				more = next();
			}
		}
		else {
			try {
				if ( getResultSet().isAfterLast() ) {
					// should not be able to reach last without maxPosition being set
					// unless there are no results
					return false;
				}

				while ( !getResultSet().isAfterLast() ) {
					more = next();
				}
			}
			catch( SQLException e ) {
				throw JDBCExceptionHelper.convert(
						getSession().getFactory().getSQLExceptionConverter(),
						e,
						"exception calling isAfterLast()"
					);
			}
		}

		afterScrollOperation();

		return more;
	}

	/**
	 * Go to the first result
	 *
	 * @return <tt>true</tt> if there are any results
	 */
	public boolean first() throws HibernateException {
		beforeFirst();
		boolean more = next();

		afterScrollOperation();

		return more;
	}

	/**
	 * Go to a location just before first result (this is the initial location)
	 */
	public void beforeFirst() throws HibernateException {
		try {
			getResultSet().beforeFirst();
		}
		catch( SQLException e ) {
			throw JDBCExceptionHelper.convert(
			        getSession().getFactory().getSQLExceptionConverter(),
			        e,
			        "exception calling beforeFirst()"
				);
		}
		currentRow = null;
		currentPosition = 0;
	}

	/**
	 * Go to a location just after the last result
	 */
	public void afterLast() throws HibernateException {
		// TODO : not sure the best way to handle this.
		// The non-performant way :
		last();
		next();
		afterScrollOperation();
	}

	/**
	 * Is this the first result?
	 *
	 * @return <tt>true</tt> if this is the first row of results
	 *
	 * @throws org.hibernate.HibernateException
	 */
	public boolean isFirst() throws HibernateException {
		return currentPosition == 1;
	}

	/**
	 * Is this the last result?
	 *
	 * @return <tt>true</tt> if this is the last row of results
	 *
	 * @throws org.hibernate.HibernateException
	 */
	public boolean isLast() throws HibernateException {
		if ( maxPosition == null ) {
			// we have not yet hit the last result...
			return false;
		}
		else {
			return currentPosition == maxPosition.intValue();
		}
	}

	/**
	 * Get the current location in the result set. The first row is number <tt>0</tt>, contrary to JDBC.
	 *
	 * @return the row number, numbered from <tt>0</tt>, or <tt>-1</tt> if there is no current row
	 */
	public int getRowNumber() throws HibernateException {
		return currentPosition;
	}

	/**
	 * Set the current location in the result set, numbered from either the first row (row number <tt>0</tt>), or the last
	 * row (row number <tt>-1</tt>).
	 *
	 * @param rowNumber the row number, numbered from the last row, in the case of a negative row number
	 *
	 * @return true if there is a row at that row number
	 */
	public boolean setRowNumber(int rowNumber) throws HibernateException {
		if ( rowNumber == 1 ) {
			return first();
		}
		else if ( rowNumber == -1 ) {
			return last();
		}
		else if ( maxPosition != null && rowNumber == maxPosition.intValue() ) {
			return last();
		}
		return scroll( rowNumber - currentPosition );
	}
}
