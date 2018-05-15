/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import java.util.function.Function;

import org.hibernate.MappingException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.service.ServiceRegistry;

import org.jboss.logging.Logger;

/**
 * Envers implementation of a Function for constructing the {@link AuditStrategy}
 * as part of the call to the {@link org.hibernate.boot.registry.selector.spi.StrategySelector}
 * service
 *
 * @author Chris Cranford
 */
public class StrategyCreatorAuditStrategyImpl<I extends AuditStrategy> implements Function<Class<I>,I> {
	private static final Logger log = Logger.getLogger( StrategyCreatorAuditStrategyImpl.class );

	private final PropertyData timestampData;
	private final Class<?> revisionInfoClass;
	private final ServiceRegistry serviceRegistry;

	StrategyCreatorAuditStrategyImpl(PropertyData timestampData, Class<?> revisionInfoClass, ServiceRegistry serviceRegistry) {
		this.timestampData = timestampData;
		this.revisionInfoClass = revisionInfoClass;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public I apply(Class<I> strategyImplClass) {
		log.debugf( "Creating AuditStrategy Impl [%s]", strategyImplClass.getName() );
		try {
			I strategy = strategyImplClass.newInstance();

			strategy.postInitialize( revisionInfoClass, timestampData, serviceRegistry );

			return strategy;
		}
		catch ( Exception e ) {
			throw new MappingException(
					String.format(
							"Unable to create AuditStrategy [%s].",
							strategyImplClass.getName()
					)
			);
		}
	}
}
