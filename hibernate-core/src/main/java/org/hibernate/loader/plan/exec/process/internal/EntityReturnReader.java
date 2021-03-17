/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
			final Object proxy = context.getSession().getPersistenceContextInternal().proxyFor(
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
