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
package org.hibernate.internal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.HibernateIterator;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.HolderInstantiator;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * An implementation of <tt>java.util.Iterator</tt> that is
 * returned by <tt>iterate()</tt> query execution methods.
 * @author Gavin King
 */
public final class IteratorImpl implements HibernateIterator {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, IteratorImpl.class.getName());

	private ResultSet rs;
	private final EventSource session;
	private boolean readOnly;
	private final Type[] types;
	private final boolean single;
	private Object currentResult;
	private boolean hasNext;
	private final String[][] names;
	private PreparedStatement ps;
	private HolderInstantiator holderInstantiator;

	public IteratorImpl(
	        ResultSet rs,
	        PreparedStatement ps,
	        EventSource sess,
	        boolean readOnly,
	        Type[] types,
	        String[][] columnNames,
	        HolderInstantiator holderInstantiator)
	throws HibernateException, SQLException {

		this.rs=rs;
		this.ps=ps;
		this.session = sess;
		this.readOnly = readOnly;
		this.types = types;
		this.names = columnNames;
		this.holderInstantiator = holderInstantiator;

		single = types.length==1;

		postNext();
	}

	public void close() throws JDBCException {
		if (ps!=null) {
			try {
				LOG.debug("Closing iterator");
				ps.close();
				ps = null;
				rs = null;
				hasNext = false;
			}
			catch (SQLException e) {
                LOG.unableToCloseIterator(e);
				throw session.getFactory().getSQLExceptionHelper().convert(
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
                    LOG.debugf("Exception trying to cleanup load context : %s", ignore.getMessage());
				}
			}
		}
	}

	private void postNext() throws SQLException {
		LOG.debug("Attempting to retrieve next results");
		this.hasNext = rs.next();
		if (!hasNext) {
			LOG.debug("Exhausted results");
			close();
		} else LOG.debug("Retrieved next results");
	}

	public boolean hasNext() {
		return hasNext;
	}

	public Object next() throws HibernateException {
		if ( !hasNext ) throw new NoSuchElementException("No more results");
		boolean sessionDefaultReadOnlyOrig = session.isDefaultReadOnly();
		session.setDefaultReadOnly( readOnly );
		try {
			boolean isHolder = holderInstantiator.isRequired();

			LOG.debugf( "Assembling results" );
			if ( single && !isHolder ) {
				currentResult = types[0].nullSafeGet( rs, names[0], session, null );
			}
			else {
				Object[] currentResults = new Object[types.length];
				for (int i=0; i<types.length; i++) {
					currentResults[i] = types[i].nullSafeGet( rs, names[i], session, null );
				}

				if (isHolder) {
					currentResult = holderInstantiator.instantiate(currentResults);
				}
				else {
					currentResult = currentResults;
				}
			}

			postNext();
			LOG.debugf( "Returning current results" );
			return currentResult;
		}
		catch (SQLException sqle) {
			throw session.getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not get next iterator result"
				);
		}
		finally {
			session.setDefaultReadOnly( sessionDefaultReadOnlyOrig );
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
