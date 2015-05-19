/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.io.Serializable;

import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.AbstractQueryImpl;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.entity.UniqueEntityLoader;

/**
 * Not really a Loader, just a wrapper around a named query.  Used when the metadata has named a query to use for
 * loading an entity (using {@link org.hibernate.annotations.Loader} or {@code <loader/>}).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class NamedQueryLoader implements UniqueEntityLoader {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( NamedQueryLoader.class );

	private final String queryName;
	private final EntityPersister persister;

	/**
	 * Constructs the NamedQueryLoader
	 *
	 * @param queryName The name of the named query to use
	 * @param persister The corresponding persister for the entity we are loading
	 */
	public NamedQueryLoader(String queryName, EntityPersister persister) {
		super();
		this.queryName = queryName;
		this.persister = persister;
	}

	@Override
	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {
		if ( lockOptions != null ) {
			LOG.debug( "Ignoring lock-options passed to named query loader" );
		}
		return load( id, optionalObject, session );
	}

	@Override
	public Object load(Serializable id, Object optionalObject, SessionImplementor session) {
		LOG.debugf( "Loading entity: %s using named query: %s", persister.getEntityName(), queryName );

		// IMPL NOTE: essentially we perform the named query (which loads the entity into the PC), and then
		// do an internal lookup of the entity from the PC.

		final AbstractQueryImpl query = (AbstractQueryImpl) session.getNamedQuery( queryName );
		if ( query.hasNamedParameters() ) {
			query.setParameter( query.getNamedParameters()[0], id, persister.getIdentifierType() );
		}
		else {
			query.setParameter( 0, id, persister.getIdentifierType() );
		}

		query.setOptionalId( id );
		query.setOptionalEntityName( persister.getEntityName() );
		query.setOptionalObject( optionalObject );
		query.setFlushMode( FlushMode.MANUAL );
		query.list();

		// now look up the object we are really interested in!
		// (this lets us correctly handle proxies and multi-row or multi-column queries)
		return session.getPersistenceContext().getEntity( session.generateEntityKey( id, persister ) );

	}
}
