/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.hql.internal;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.QUERY_TRANSLATOR;

/**
 * Initiator for the QueryTranslatorFactory service
 *
 * @author Steve Ebersole
 */
public class QueryTranslatorFactoryInitiator implements StandardServiceInitiator<QueryTranslatorFactory> {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( QueryTranslatorFactoryInitiator.class );

	/**
	 * Singleton access
	 */
	public static final QueryTranslatorFactoryInitiator INSTANCE = new QueryTranslatorFactoryInitiator();

	@Override
	public QueryTranslatorFactory initiateService(
			Map configurationValues,
			ServiceRegistryImplementor registry) {
		final StrategySelector strategySelector = registry.getService( StrategySelector.class );
		final QueryTranslatorFactory factory = strategySelector.resolveDefaultableStrategy(
				QueryTranslatorFactory.class,
				configurationValues.get( QUERY_TRANSLATOR ),
				ASTQueryTranslatorFactory.INSTANCE
		);

		log.debugf( "QueryTranslatorFactory : %s", factory );
		if ( factory instanceof ASTQueryTranslatorFactory ) {
			log.usingAstQueryTranslatorFactory();
		}

		return factory;
	}

	@Override
	public Class<QueryTranslatorFactory> getServiceInitiated() {
		return QueryTranslatorFactory.class;
	}
}
