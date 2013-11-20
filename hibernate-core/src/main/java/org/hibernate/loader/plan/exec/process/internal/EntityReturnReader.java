/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.exec.process.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.loader.plan.exec.process.spi.ReturnReader;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.proxy.HibernateProxy;

import static org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext.EntityReferenceProcessingState;

/**
 * @author Steve Ebersole
 */
public class EntityReturnReader implements ReturnReader {
	private final EntityReturn entityReturn;

	public EntityReturnReader(EntityReturn entityReturn) {
		this.entityReturn = entityReturn;
	}

	public EntityReferenceProcessingState getIdentifierResolutionContext(ResultSetProcessingContext context) {
		final EntityReferenceProcessingState entityReferenceProcessingState = context.getProcessingState( entityReturn );

		if ( entityReferenceProcessingState == null ) {
			throw new AssertionFailure(
					String.format(
							"Could not locate EntityReferenceProcessingState for root entity return [%s (%s)]",
							entityReturn.getPropertyPath().getFullPath(),
							entityReturn.getEntityPersister().getEntityName()
					)
			);
		}

		return entityReferenceProcessingState;
	}

	@Override
	public Object read(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		final EntityReferenceProcessingState processingState = getIdentifierResolutionContext( context );

		final EntityKey entityKey = processingState.getEntityKey();
		final Object entityInstance = context.getProcessingState( entityReturn ).getEntityInstance();

		if ( context.shouldReturnProxies() ) {
			final Object proxy = context.getSession().getPersistenceContext().proxyFor(
					entityReturn.getEntityPersister(),
					entityKey,
					entityInstance
			);
			if ( proxy != entityInstance ) {
				( (HibernateProxy) proxy ).getHibernateLazyInitializer().setImplementation( proxy );
				return proxy;
			}
		}

		return entityInstance;
	}
}
