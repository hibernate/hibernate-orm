//$Id: IteratorImpl.java 9944 2006-05-24 21:14:56Z steve.ebersole@jboss.com $
package org.hibernate.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.HibernateIterator;
import org.hibernate.event.EventSource;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.hql.HolderInstantiator;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * An implementation of <tt>java.util.Iterator</tt> that is
 * returned by <tt>iterate()</tt> query execution methods.
 * @author Gavin King
 */
public final class IteratorImpl implements HibernateIterator {

	private static final Logger log = LoggerFactory.getLogger(IteratorImpl.class);

	private ResultSet rs;
	private final EventSource session;
	private final Type[] types;
	private final boolean single;
	private Object currentResult;
	private boolean hasNext;
	private final String[][] names;
	private PreparedStatement ps;
	private Object nextResult;
	private HolderInstantiator holderInstantiator;

	public IteratorImpl(
	        ResultSet rs,
	        PreparedStatement ps,
	        EventSource sess,
	        Type[] types,
	        String[][] columnNames,
	        HolderInstantiator holderInstantiator)
	throws HibernateException, SQLException {

		this.rs=rs;
		this.ps=ps;
		this.session = sess;
		this.types = types;
		this.names = columnNames;
		this.holderInstantiator = holderInstantiator;

		single = types.length==1;

		postNext();
	}

	public void close() throws JDBCException {
		if (ps!=null) {
			try {
				log.debug("closing iterator");
				nextResult = null;
				session.getBatcher().closeQueryStatement(ps, rs);
				ps = null;
				rs = null;
				hasNext = false;
			}
			catch (SQLException e) {
				log.info( "Unable to close iterator", e );
				throw JDBCExceptionHelper.convert(
				        session.getFactory().getSQLExceptionConverter(),
				        e,
				        "Unable to close iterator"
					);
			}
			finally {
				try {
					session.getPersistenceContext().getLoadContexts().cleanup( rs );
				}
				catch( Throwable ignore ) {
					// ignore this error for now
					log.trace( "exception trying to cleanup load context : " + ignore.getMessage() );
				}
			}
		}
	}

	private void postNext() throws HibernateException, SQLException {
		this.hasNext = rs.next();
		if (!hasNext) {
			log.debug("exhausted results");
			close();
		}
		else {
			log.debug("retrieving next results");
			boolean isHolder = holderInstantiator.isRequired();

			if ( single && !isHolder ) {
				nextResult = types[0].nullSafeGet( rs, names[0], session, null );
			}
			else {
				Object[] nextResults = new Object[types.length];
				for (int i=0; i<types.length; i++) {
					nextResults[i] = types[i].nullSafeGet( rs, names[i], session, null );
				}

				if (isHolder) {
					nextResult = holderInstantiator.instantiate(nextResults);
				}
				else {
					nextResult = nextResults;
				}
			}

		}
	}

	public boolean hasNext() {
		return hasNext;
	}

	public Object next() {
		if ( !hasNext ) throw new NoSuchElementException("No more results");
		try {
			currentResult = nextResult;
			postNext();
			log.debug("returning current results");
			return currentResult;
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not get next iterator result"
				);
		}
	}

	public void remove() {
		if (!single) {
			throw new UnsupportedOperationException("Not a single column hibernate query result set");
		}
		if (currentResult==null) {
			throw new IllegalStateException("Called Iterator.remove() before next()");
		}
		if ( !( types[0] instanceof EntityType ) ) {
			throw new UnsupportedOperationException("Not an entity");
		}
		
		session.delete( 
				( (EntityType) types[0] ).getAssociatedEntityName(), 
				currentResult,
				false,
		        null
			);
	}

}
