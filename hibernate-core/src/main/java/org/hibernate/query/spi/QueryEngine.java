/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.QueryLogger;
import org.hibernate.query.internal.QueryInterpretationCacheDisabledImpl;
import org.hibernate.query.internal.QueryInterpretationCacheStandardImpl;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.hql.SemanticQueryProducer;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.internal.SemanticQueryProducerImpl;
import org.hibernate.query.sqm.produce.internal.SqmCreationOptionsStandard;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmCreationOptions;
import org.hibernate.service.ServiceRegistry;

/**
 * Aggregation and encapsulation of the components Hibernate uses
 * to execute queries (HQL, Criteria and native)
 *
 * @author Steve Ebersole
 */
@Incubating
public class QueryEngine {
	public static QueryEngine from(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata) {
		return new QueryEngine(
				sessionFactory.getMetamodel(),
				sessionFactory.getServiceRegistry(),
				sessionFactory.getSessionFactoryOptions(),
				sessionFactory,
				new SqmCreationOptionsStandard( sessionFactory ),
				sessionFactory.getProperties(),
				metadata.buildNamedQueryRepository( sessionFactory )
		);
	}

	private final NamedQueryRepository namedQueryRepository;
	private final SqmCriteriaNodeBuilder criteriaBuilder;
	private final SemanticQueryProducer semanticQueryProducer;
	private final QueryInterpretationCache interpretationCache;
	private final SqmFunctionRegistry sqmFunctionRegistry;

	public QueryEngine(
			MetamodelImplementor domainModel,
			ServiceRegistry serviceRegistry,
			SessionFactoryOptions runtimeOptions,
			SqmCreationContext sqmCreationContext,
			SqmCreationOptions sqmCreationOptions,
			Map properties,
			NamedQueryRepository namedQueryRepository) {
		this.namedQueryRepository = namedQueryRepository;
		this.semanticQueryProducer = new SemanticQueryProducerImpl( sqmCreationContext, sqmCreationOptions );
		this.criteriaBuilder = new SqmCriteriaNodeBuilder(
				this,
				domainModel.getJpaMetamodel(),
				serviceRegistry
		);

		this.interpretationCache = buildQueryPlanCache( properties );

		this.sqmFunctionRegistry = new SqmFunctionRegistry();
		serviceRegistry.getService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect()
				.initializeFunctionRegistry( this );
		runtimeOptions.getSqmFunctionRegistry().overlay( sqmFunctionRegistry );

		getNamedQueryRepository().checkNamedQueries( this );
	}

	private static QueryInterpretationCache buildQueryPlanCache(Map properties) {
		final boolean explicitUseCache = ConfigurationHelper.getBoolean(
				AvailableSettings.QUERY_PLAN_CACHE_ENABLED,
				properties,
				false
		);

		final Integer explicitMaxPlanCount = ConfigurationHelper.getInteger(
				AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE,
				properties
		);

		if ( explicitUseCache || ( explicitMaxPlanCount != null && explicitMaxPlanCount > 0 ) ) {
			return new QueryInterpretationCacheStandardImpl(
					explicitMaxPlanCount != null
							? explicitMaxPlanCount
							: QueryInterpretationCacheStandardImpl.DEFAULT_QUERY_PLAN_MAX_COUNT
			);
		}
		else {
			// disabled
			return QueryInterpretationCacheDisabledImpl.INSTANCE;
		}
	}

	public void prepare(SessionFactoryImplementor sessionFactory) {
		//checking for named queries
		if ( sessionFactory.getSessionFactoryOptions().isNamedQueryStartupCheckingEnabled() ) {
			final Map<String, HibernateException> errors = namedQueryRepository.checkNamedQueries( this );

			if ( !errors.isEmpty() ) {
				StringBuilder failingQueries = new StringBuilder( "Errors in named queries: " );
				String sep = "";
				for ( Map.Entry<String, HibernateException> entry : errors.entrySet() ) {
					QueryLogger.QUERY_LOGGER.namedQueryError( entry.getKey(), entry.getValue() );
					failingQueries.append( sep ).append( entry.getKey() );
					sep = ", ";
				}
				throw new HibernateException( failingQueries.toString() );
			}
		}
	}

	public NamedQueryRepository getNamedQueryRepository() {
		return namedQueryRepository;
	}

	public SqmCriteriaNodeBuilder getCriteriaBuilder() {
		return criteriaBuilder;
	}

	public SemanticQueryProducer getSemanticQueryProducer() {
		return semanticQueryProducer;
	}

	public QueryInterpretationCache getInterpretationCache() {
		return interpretationCache;
	}

	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return sqmFunctionRegistry;
	}

	public void close() {
		if ( namedQueryRepository != null ) {
			namedQueryRepository.close();
		}

		if ( criteriaBuilder != null ) {
			criteriaBuilder.close();
		}

		if ( semanticQueryProducer != null ) {
			semanticQueryProducer.close();
		}

		if ( interpretationCache != null ) {
			interpretationCache.close();
		}

		if ( sqmFunctionRegistry != null ) {
			sqmFunctionRegistry.close();
		}
	}
}
