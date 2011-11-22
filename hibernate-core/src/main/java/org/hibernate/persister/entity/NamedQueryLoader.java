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
package org.hibernate.persister.entity;

import java.io.Serializable;

import org.jboss.logging.Logger;

import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.AbstractQueryImpl;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.entity.UniqueEntityLoader;

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

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, NamedQueryLoader.class.getName());

	public NamedQueryLoader(String queryName, EntityPersister persister) {
		super();
		this.queryName = queryName;
		this.persister = persister;
	}

	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {
		if (lockOptions != null) LOG.debug("Ignoring lock-options passed to named query loader");
		return load( id, optionalObject, session );
	}

	public Object load(Serializable id, Object optionalObject, SessionImplementor session) {
        LOG.debugf("Loading entity: %s using named query: %s", persister.getEntityName(), queryName);

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
		// (this lets us correctly handle proxies and multi-row or multi-column queries)
		return session.getPersistenceContext().getEntity( session.generateEntityKey( id, persister ) );

	}
}