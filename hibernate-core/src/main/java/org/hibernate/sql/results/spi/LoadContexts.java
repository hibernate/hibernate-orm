/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.sql.ResultSet;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.StandardStack;

/**
 * Maps {@link ResultSet result-sets} to specific contextual data related to processing that result set
 * <p/>
 * Considering the JDBC-redesign work, would further like this contextual info not mapped separately, but available
 * based on the result set being processed.  This would also allow maintaining a single mapping as we could reliably
 * get notification of the result-set closing...
 *
 * @author Steve Ebersole
 */
public class LoadContexts {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( LoadContexts.class );

	private final PersistenceContext persistenceContext;
	private final StandardStack<JdbcValuesSourceProcessingState> jdbcValuesSourceProcessingStateStack = new StandardStack<>();

	public LoadContexts(PersistenceContext persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	public void register(JdbcValuesSourceProcessingState state) {
		jdbcValuesSourceProcessingStateStack.push( state );
	}

	public void deregister(JdbcValuesSourceProcessingState state) {
		final JdbcValuesSourceProcessingState previous = jdbcValuesSourceProcessingStateStack.pop();
		if ( previous != state ) {
			throw new IllegalStateException( "Illegal pop() with non-matching JdbcValuesSourceProcessingState" );
		}
	}

	public LoadingEntityEntry findLoadingEntityEntry(EntityKey entityKey) {
		return jdbcValuesSourceProcessingStateStack.findCurrentFirst(
				state -> state.findLoadingEntityLocally( entityKey )
		);
	}

	public LoadingCollectionEntry findLoadingCollectionEntry(CollectionKey collectionKey) {
		return jdbcValuesSourceProcessingStateStack.findCurrentFirst(
				state -> state.findLoadingCollectionLocally( collectionKey )
		);
	}

	/**
	 * Retrieves the persistence context to which this is bound.
	 *
	 * @return The persistence context to which this is bound.
	 */
	public PersistenceContext getPersistenceContext() {
		return persistenceContext;
	}

	private SharedSessionContractImplementor getSession() {
		return getPersistenceContext().getSession();
	}


	/**
	 * Release internal state associated with *all* result sets.
	 * <p/>
	 * This is intended as a "failsafe" process to make sure we get everything
	 * cleaned up and released.
	 */
	public void cleanup() {
		if ( ! jdbcValuesSourceProcessingStateStack.isEmpty() ) {
			log.debugf( "LoadContexts still contained JdbcValuesSourceProcessingState registrations on cleanup" );
		}
		jdbcValuesSourceProcessingStateStack.clear();
	}
}
