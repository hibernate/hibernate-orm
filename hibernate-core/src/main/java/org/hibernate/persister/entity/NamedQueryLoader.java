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
package org.hibernate.persister.entity;

import static org.jboss.logging.Logger.Level.DEBUG;
import java.io.Serializable;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.AbstractQueryImpl;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Not really a <tt>Loader</tt>, just a wrapper around a
 * named query.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class NamedQueryLoader implements UniqueEntityLoader {
	private final String queryName;
	private final EntityPersister persister;

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                NamedQueryLoader.class.getPackage().getName());

	public NamedQueryLoader(String queryName, EntityPersister persister) {
		super();
		this.queryName = queryName;
		this.persister = persister;
	}

	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {
        if (lockOptions != null) LOG.ignoringLockOptions();
		return load( id, optionalObject, session );
	}

	public Object load(Serializable id, Object optionalObject, SessionImplementor session) {
        LOG.loadingEntity(persister.getEntityName(), queryName);

		AbstractQueryImpl query = (AbstractQueryImpl) session.getNamedQuery(queryName);
		if ( query.hasNamedParameters() ) {
			query.setParameter(
					query.getNamedParameters()[0],
					id,
					persister.getIdentifierType()
				);
		}
		else {
			query.setParameter( 0, id, persister.getIdentifierType() );
		}
		query.setOptionalId(id);
		query.setOptionalEntityName( persister.getEntityName() );
		query.setOptionalObject(optionalObject);
		query.setFlushMode( FlushMode.MANUAL );
		query.list();

		// now look up the object we are really interested in!
		// (this lets us correctly handle proxies and multi-row
		// or multi-column queries)
		return session.getPersistenceContext()
				.getEntity( new EntityKey( id, persister, session.getEntityMode() ) );

	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = DEBUG )
        @Message( value = "Ignoring lock-options passed to named query loader" )
        void ignoringLockOptions();

        @LogMessage( level = DEBUG )
        @Message( value = "Loading entity: %s using named query: %s" )
        void loadingEntity( String entityName,
                            String queryName );
    }
}