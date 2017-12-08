/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.selector.spi.StrategyCreator;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.service.ServiceRegistry;

import org.jboss.logging.Logger;

/**
 * Envers implementation of {@link StrategyCreator} for constructing the {@link AuditStrategy}.
 *
 * @author Chris Cranford
 */
public class StrategyCreatorAuditStrategyImpl implements StrategyCreator<AuditStrategy> {
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
	public AuditStrategy create(Class<? extends AuditStrategy> strategyClass) {
		log.debugf( "Creating AuditStrategy Impl [%s]", strategyClass.getName() );
		try {
			return performInjections( strategyClass.newInstance() );
		}
		catch ( Exception e ) {
			throw new MappingException(
					String.format(
							"Unable to create AuditStrategy [%s].",
							strategyClass.getName()
					)
			);
		}
	}

	private AuditStrategy performInjections(AuditStrategy strategy) {
		if ( ValidityAuditStrategy.class.isInstance( strategy ) ) {
			final Getter getter = ReflectionTools.getGetter( revisionInfoClass, timestampData, serviceRegistry );
			( (ValidityAuditStrategy) strategy ).setRevisionTimestampGetter( getter );
		}
		return strategy;
	}
}
