/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.hql.internal.StandardHqlTranslator;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryEngineOptions;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.internal.SqmCreationOptionsStandard;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.sql.StandardSqmTranslatorFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Comparator.comparingInt;

/**
 * Aggregation and encapsulation of the components Hibernate uses
 * to execute queries (HQL, Criteria and native)
 *
 * @author Steve Ebersole
 */
public class QueryEngineImpl implements QueryEngine {

	private static final Logger LOG_HQL_FUNCTIONS = CoreLogging.logger("org.hibernate.HQL_FUNCTIONS");

	public static QueryEngine from(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata) {
		final QueryEngineOptions options = sessionFactory.getSessionFactoryOptions();
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
		return new QueryEngineImpl(
				sessionFactory,
				metadata.getTypeConfiguration(),
				resolveHqlTranslator( options, dialect, sessionFactory, new SqmCreationOptionsStandard( options ) ),
				resolveSqmTranslatorFactory( options, dialect ),
				createFunctionRegistry( sessionFactory, metadata, options, dialect ),
				metadata.buildNamedQueryRepository( sessionFactory ),
				buildInterpretationCache( sessionFactory::getStatistics, sessionFactory.getProperties() ),
				sessionFactory.getServiceRegistry().getService(NativeQueryInterpreter.class)
		);
	}

	private static SqmFunctionRegistry createFunctionRegistry(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata,
			QueryEngineOptions queryEngineOptions,
			Dialect dialect) {
		final SqmFunctionRegistry sqmFunctionRegistry = metadata.getFunctionRegistry();

		queryEngineOptions.getCustomSqlFunctionMap().forEach( sqmFunctionRegistry::register );

		final SqmFunctionRegistry customSqmFunctionRegistry = queryEngineOptions.getCustomSqmFunctionRegistry();
		if ( customSqmFunctionRegistry != null ) {
			customSqmFunctionRegistry.overlay( sqmFunctionRegistry );
		}

		//TODO: probably better to turn this back into an anonymous class
		final FunctionContributions functionContributions = new QueryEngineImpl.FunctionContributionsImpl(
				sessionFactory.getServiceRegistry(),
				metadata.getTypeConfiguration(),
				sqmFunctionRegistry
		);
		for ( FunctionContributor contributor : sortedFunctionContributors( sessionFactory.getServiceRegistry() ) ) {
			contributor.contributeFunctions( functionContributions );
		}

		dialect.initializeFunctionRegistry( functionContributions );

		if ( LOG_HQL_FUNCTIONS.isDebugEnabled() ) {
			sqmFunctionRegistry.getFunctionsByName().forEach(
					entry -> LOG_HQL_FUNCTIONS.debug( entry.getValue().getSignature( entry.getKey() ) )
			);
		}

		return sqmFunctionRegistry;
	}

	private final TypeConfiguration typeConfiguration;
	private final NamedObjectRepository namedObjectRepository;
	private final NativeQueryInterpreter nativeQueryInterpreter;
	private final QueryInterpretationCache interpretationCache;
	private final NodeBuilder criteriaBuilder;
	private final HqlTranslator hqlTranslator;
	private final SqmTranslatorFactory sqmTranslatorFactory;
	private final SqmFunctionRegistry sqmFunctionRegistry;

	private QueryEngineImpl(
			SessionFactoryImplementor sessionFactory,
			TypeConfiguration typeConfiguration,
			HqlTranslator hqlTranslator,
			SqmTranslatorFactory sqmTranslatorFactory,
			SqmFunctionRegistry functionRegistry,
			NamedObjectRepository namedObjectRepository,
			QueryInterpretationCache interpretationCache,
			NativeQueryInterpreter nativeQueryInterpreter) {
		this.typeConfiguration = typeConfiguration;
		this.sqmFunctionRegistry = functionRegistry;
		this.sqmTranslatorFactory = sqmTranslatorFactory;
		this.hqlTranslator = hqlTranslator;
		this.namedObjectRepository = namedObjectRepository;
		this.interpretationCache = interpretationCache;
		this.nativeQueryInterpreter = nativeQueryInterpreter;
		final SessionFactoryOptions sessionFactoryOptions = sessionFactory.getSessionFactoryOptions();
		this.criteriaBuilder = new SqmCriteriaNodeBuilder(
				sessionFactory.getUuid(),
				sessionFactory.getName(),
				this,
				sessionFactoryOptions.getJpaCompliance().isJpaQueryComplianceEnabled(),
				sessionFactoryOptions.getCriteriaValueHandlingMode(),
				() -> sessionFactory
		);
	}

	private static HqlTranslator resolveHqlTranslator(
			QueryEngineOptions runtimeOptions,
			Dialect dialect,
			SqmCreationContext sqmCreationContext,
			SqmCreationOptions sqmCreationOptions) {
		if ( runtimeOptions.getCustomHqlTranslator() != null ) {
			return runtimeOptions.getCustomHqlTranslator();
		}
		else if ( dialect.getHqlTranslator() != null ) {
			return dialect.getHqlTranslator();
		}
		else {
			return new StandardHqlTranslator( sqmCreationContext, sqmCreationOptions );
		}
	}

	private static SqmTranslatorFactory resolveSqmTranslatorFactory(
			QueryEngineOptions runtimeOptions,
			Dialect dialect) {
		if ( runtimeOptions.getCustomSqmTranslatorFactory() != null ) {
			return runtimeOptions.getCustomSqmTranslatorFactory();
		}
		else if ( dialect.getSqmTranslatorFactory() != null ) {
			return dialect.getSqmTranslatorFactory();
		}
		else {
			return new StandardSqmTranslatorFactory();
		}
	}

	private static List<FunctionContributor> sortedFunctionContributors(ServiceRegistry serviceRegistry) {
		Collection<FunctionContributor> functionContributors =
				serviceRegistry.requireService(ClassLoaderService.class)
						.loadJavaServices(FunctionContributor.class);
		List<FunctionContributor> contributors = new ArrayList<>( functionContributors );
		contributors.sort(
				comparingInt( FunctionContributor::ordinal )
						.thenComparing( a -> a.getClass().getCanonicalName() )
		);
		return contributors;
	}

	private static QueryInterpretationCache buildInterpretationCache(
			Supplier<StatisticsImplementor> statisticsSupplier,
			Map<String, Object> properties) {
		final boolean explicitUseCache = ConfigurationHelper.getBoolean(
				AvailableSettings.QUERY_PLAN_CACHE_ENABLED,
				properties,
				// enabled by default
				true
		);

		final Integer explicitMaxPlanSize = ConfigurationHelper.getInteger(
				AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE,
				properties
		);

		if ( explicitUseCache || explicitMaxPlanSize != null && explicitMaxPlanSize > 0 ) {
			final int size = explicitMaxPlanSize != null
					? explicitMaxPlanSize
					: QueryEngine.DEFAULT_QUERY_PLAN_MAX_COUNT;

			return new QueryInterpretationCacheStandardImpl( size, statisticsSupplier );
		}
		else {
			// disabled
			return new QueryInterpretationCacheDisabledImpl( statisticsSupplier );
		}
	}

	@Override
	public void validateNamedQueries() {
		namedObjectRepository.validateNamedQueries( this );
	}

	@Override
	public NamedObjectRepository getNamedObjectRepository() {
		return namedObjectRepository;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public NodeBuilder getCriteriaBuilder() {
		return criteriaBuilder;
	}

	@Override
	public HqlTranslator getHqlTranslator() {
		return hqlTranslator;
	}

	@Override
	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return sqmTranslatorFactory;
	}

	@Override
	public NativeQueryInterpreter getNativeQueryInterpreter() {
		return nativeQueryInterpreter;
	}

	@Override
	public QueryInterpretationCache getInterpretationCache() {
		return interpretationCache;
	}

	@Override
	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return sqmFunctionRegistry;
	}

	@Override
	public void close() {
		if ( namedObjectRepository != null ) {
			namedObjectRepository.close();
		}

		if ( interpretationCache != null ) {
			interpretationCache.close();
		}

		if ( sqmFunctionRegistry != null ) {
			sqmFunctionRegistry.close();
		}
	}

	private static class FunctionContributionsImpl implements FunctionContributions {
		private final ServiceRegistry serviceRegistry;
		private final TypeConfiguration typeConfiguration;
		private final SqmFunctionRegistry functionRegistry;

		public FunctionContributionsImpl(
				ServiceRegistry serviceRegistry,
				TypeConfiguration typeConfiguration,
				SqmFunctionRegistry functionRegistry) {
			this.serviceRegistry = serviceRegistry;
			this.typeConfiguration = typeConfiguration;
			this.functionRegistry = functionRegistry;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}

		@Override
		public SqmFunctionRegistry getFunctionRegistry() {
			return functionRegistry;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}
	}

}
