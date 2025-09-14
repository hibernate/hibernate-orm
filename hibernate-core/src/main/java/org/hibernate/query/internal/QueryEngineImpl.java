/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.type.BindingContext;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.hql.internal.StandardHqlTranslator;
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
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparingInt;
import static org.hibernate.cfg.QuerySettings.QUERY_PLAN_CACHE_ENABLED;
import static org.hibernate.cfg.QuerySettings.QUERY_PLAN_CACHE_MAX_SIZE;

/**
 * Aggregation and encapsulation of the components Hibernate uses
 * to execute queries (HQL, Criteria and native)
 *
 * @author Steve Ebersole
 */
public class QueryEngineImpl implements QueryEngine {

	private static final Logger LOG_HQL_FUNCTIONS = Logger.getLogger( "org.hibernate.HQL_FUNCTIONS" );

	private final TypeConfiguration typeConfiguration;
	private final NamedObjectRepository namedObjectRepository;
	private final NativeQueryInterpreter nativeQueryInterpreter;
	private final BindingContext bindingContext;
	private final ClassLoaderService classLoaderService;
	private final QueryInterpretationCache interpretationCache;
	private final NodeBuilder nodeBuilder;
	private final HqlTranslator hqlTranslator;
	private final SqmTranslatorFactory sqmTranslatorFactory;
	private final SqmFunctionRegistry sqmFunctionRegistry;
	private final Dialect dialect;

	public QueryEngineImpl(
			MetadataImplementor metadata,
			QueryEngineOptions options,
			BindingContext context,
			ServiceRegistryImplementor serviceRegistry,
			Map<String,Object> properties,
			String name) {
		dialect = serviceRegistry.requireService( JdbcServices.class ).getDialect();
		bindingContext = context;
		typeConfiguration = metadata.getTypeConfiguration();
		sqmFunctionRegistry = createFunctionRegistry( serviceRegistry, metadata, options, dialect );
		sqmTranslatorFactory = resolveSqmTranslatorFactory( options, dialect );
		namedObjectRepository = metadata.buildNamedQueryRepository();
		interpretationCache = buildInterpretationCache( serviceRegistry, properties );
		nativeQueryInterpreter = serviceRegistry.getService( NativeQueryInterpreter.class );
		classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		// here we have something nasty: we need to pass a reference to the current object to
		// create the NodeBuilder, but then we need the NodeBuilder to create the HqlTranslator
		// and that's only because we're using the NodeBuilder as the SqmCreationContext
		nodeBuilder = createCriteriaBuilder( context, this, options, options.getUuid(), name );
		hqlTranslator = resolveHqlTranslator( options, dialect, nodeBuilder );
	}

	private static SqmCriteriaNodeBuilder createCriteriaBuilder(
			BindingContext context, QueryEngine engine, QueryEngineOptions options,
			String uuid, String name) {
		return new SqmCriteriaNodeBuilder( uuid, name, engine, options, context );
	}

	private static HqlTranslator resolveHqlTranslator(
			QueryEngineOptions options,
			Dialect dialect,
			SqmCreationContext sqmCreationContext) {
		final var sqmCreationOptions = new SqmCreationOptionsStandard( options );
		if ( options.getCustomHqlTranslator() != null ) {
			return options.getCustomHqlTranslator();
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

	private static SqmFunctionRegistry createFunctionRegistry(
			ServiceRegistry serviceRegistry,
			MetadataImplementor metadata,
			QueryEngineOptions queryEngineOptions,
			Dialect dialect) {
		final var sqmFunctionRegistry = metadata.getFunctionRegistry();

		queryEngineOptions.getCustomSqlFunctionMap().forEach( sqmFunctionRegistry::register );

		final var customSqmFunctionRegistry = queryEngineOptions.getCustomSqmFunctionRegistry();
		if ( customSqmFunctionRegistry != null ) {
			customSqmFunctionRegistry.overlay( sqmFunctionRegistry );
		}

		//TODO: probably better to turn this back into an anonymous class
		final var functionContributions =
				new FunctionContributionsImpl( serviceRegistry, metadata.getTypeConfiguration(), sqmFunctionRegistry );
		for ( var contributor : sortedFunctionContributors( serviceRegistry ) ) {
			contributor.contributeFunctions( functionContributions );
		}
		dialect.initializeFunctionRegistry( functionContributions );

		if ( LOG_HQL_FUNCTIONS.isDebugEnabled() ) {
			var list = new StringBuilder("Available HQL Functions:\n");
			sqmFunctionRegistry.getFunctionsByName()
					.forEach( entry -> list.append('\t')
											.append( entry.getValue().getSignature( entry.getKey() ) )
											.append('\n') );
			LOG_HQL_FUNCTIONS.debug( list.toString() );
		}

		return sqmFunctionRegistry;
	}

	private static List<FunctionContributor> sortedFunctionContributors(ServiceRegistry serviceRegistry) {
		final var functionContributors =
				serviceRegistry.requireService(ClassLoaderService.class)
						.loadJavaServices(FunctionContributor.class);
		final List<FunctionContributor> contributors = new ArrayList<>( functionContributors );
		contributors.sort(
				comparingInt( FunctionContributor::ordinal )
						.thenComparing( a -> a.getClass().getCanonicalName() )
		);
		return contributors;
	}

	public static QueryInterpretationCache buildInterpretationCache(
			ServiceRegistry serviceRegistry, Map<String, Object> properties) {
		final boolean useCache = ConfigurationHelper.getBoolean(
				QUERY_PLAN_CACHE_ENABLED,
				properties,
				// enabled by default
				true
		);

		final Integer explicitMaxPlanSize = ConfigurationHelper.getInteger(
				QUERY_PLAN_CACHE_MAX_SIZE,
				properties
		);

		//Let's avoid some confusion and check settings consistency:
		final int appliedMaxPlanSize =
				explicitMaxPlanSize == null
						? QueryEngine.DEFAULT_QUERY_PLAN_MAX_COUNT
						: explicitMaxPlanSize;
		if ( !useCache && explicitMaxPlanSize != null && appliedMaxPlanSize > 0 ) {
			throw new ConfigurationException( "Inconsistent configuration: '" + QUERY_PLAN_CACHE_MAX_SIZE
												+ "' can only be set to a greater than zero value when '"
												+ QUERY_PLAN_CACHE_ENABLED + "' is enabled" );
		}

		if ( appliedMaxPlanSize < 0 ) {
			throw new ConfigurationException( "Inconsistent configuration: '" + QUERY_PLAN_CACHE_MAX_SIZE
												+ "' can't be set to a negative value. To disable the query plan cache set '"
												+ QUERY_PLAN_CACHE_ENABLED + "' to 'false'" );
		}

		return useCache
				? new QueryInterpretationCacheStandardImpl( appliedMaxPlanSize, serviceRegistry )
				: new QueryInterpretationCacheDisabledImpl( serviceRegistry ); // disabled
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
		return nodeBuilder;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
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
	public JpaMetamodel getJpaMetamodel() {
		return bindingContext.getJpaMetamodel();
	}

	@Override
	public MappingMetamodel getMappingMetamodel() {
		return bindingContext.getMappingMetamodel();
	}

	@Override
	public Dialect getDialect() {
		return dialect;
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
