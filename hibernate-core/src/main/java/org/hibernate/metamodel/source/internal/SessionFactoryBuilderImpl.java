/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal;

import java.io.Serializable;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.proxy.EntityNotFoundDelegate;

/**
 * @author Gail Badner
 */
public class SessionFactoryBuilderImpl implements SessionFactoryBuilder {
	SessionFactoryOptionsImpl options;

	private final MetadataImplementor metadata;

	/* package-protected */
	SessionFactoryBuilderImpl(MetadataImplementor metadata) {
		this.metadata = metadata;
		options = new SessionFactoryOptionsImpl();
	}

	@Override
	public SessionFactoryBuilder with(Interceptor interceptor) {
		this.options.interceptor = interceptor;
		return this;
	}

	@Override
	public SessionFactoryBuilder with(EntityNotFoundDelegate entityNotFoundDelegate) {
		this.options.entityNotFoundDelegate = entityNotFoundDelegate;
		return this;
	}

	@Override
	public SessionFactory buildSessionFactory() {
		return new SessionFactoryImpl(metadata, options, null );
	}

	private static class SessionFactoryOptionsImpl implements SessionFactory.SessionFactoryOptions {
		private Interceptor interceptor = EmptyInterceptor.INSTANCE;

		// TODO: should there be a DefaultEntityNotFoundDelegate.INSTANCE?
		private EntityNotFoundDelegate entityNotFoundDelegate = new EntityNotFoundDelegate() {
				public void handleEntityNotFound(String entityName, Serializable id) {
					throw new ObjectNotFoundException( id, entityName );
				}
		};

		@Override
		public Interceptor getInterceptor() {
			return interceptor;
		}

		@Override
		public EntityNotFoundDelegate getEntityNotFoundDelegate() {
			return entityNotFoundDelegate;
		}
	}
}