/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;

/**
 * Maintains a Stack of processing state related to performing load operations.
 * The state is defined by {@link JdbcValuesSourceProcessingState} which
 * encapsulates the data to be processed by the load whether the data comes from
 * a ResultSet or second-level cache hit.
 *
 * @author Steve Ebersole
 */
public class LoadContexts {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( LoadContexts.class );

	private final PersistenceContext persistenceContext;
	private final StandardStack<JdbcValuesSourceProcessingState> jdbcValuesSourceProcessingStateStack = new StandardStack<>( JdbcValuesSourceProcessingState.class );

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

	public boolean isLoadingFinished() {
		return jdbcValuesSourceProcessingStateStack.getRoot() == null;
	}

	public LoadingCollectionEntry findLoadingCollectionEntry(final CollectionKey collectionKey) {
		return jdbcValuesSourceProcessingStateStack.findCurrentFirstWithParameter( collectionKey, JdbcValuesSourceProcessingState::findLoadingCollectionLocally );
	}

	/**
	 * Retrieves the persistence context to which this is bound.
	 *
	 * @return The persistence context to which this is bound.
	 */
	public PersistenceContext getPersistenceContext() {
		return persistenceContext;
	}

	/**
	 * Release internal state associated with *all* result sets.
	 * <p>
	 * This is intended as a "failsafe" process to make sure we get everything
	 * cleaned up and released.
	 */
	public void cleanup() {
		if ( ! jdbcValuesSourceProcessingStateStack.isEmpty() ) {
			log.debug( "LoadContexts still contained JdbcValuesSourceProcessingState registrations on cleanup" );
		}
		jdbcValuesSourceProcessingStateStack.clear();
	}

}
