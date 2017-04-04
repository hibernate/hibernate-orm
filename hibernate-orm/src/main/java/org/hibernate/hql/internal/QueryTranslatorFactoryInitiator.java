/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

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
