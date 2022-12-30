/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.BindableType;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.hql.internal.StandardHqlTranslator;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.internal.QueryInterpretationCacheDisabledImpl;
import org.hibernate.query.internal.QueryInterpretationCacheStandardImpl;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
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

import static java.util.Comparator.comparingInt;

/**
 * Aggregation and encapsulation of the components Hibernate uses
 * to execute queries (HQL, Criteria and native)
 *
 * @author Steve Ebersole
 */
@Incubating
public class QueryEngine implements QueryParameterBindingTypeResolver {

	/**
	 * The default soft reference count.
	 */
	public static final int DEFAULT_QUERY_PLAN_MAX_COUNT = 2048;

	private static final Logger LOG_HQL_FUNCTIONS = CoreLogging.logger( "org.hibernate.HQL_FUNCTIONS" );

	public static QueryEngine from(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata) {
		final QueryEngineOptions queryEngineOptions = sessionFactory.getSessionFactoryOptions();
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
		return new QueryEngine(
				sessionFactory,
				resolveHqlTranslator(
						queryEngineOptions,
						dialect,
						sessionFactory,
						new SqmCreationOptionsStandard( queryEngineOptions )
				),
				resolveSqmTranslatorFactory( queryEngineOptions, dialect ),
				metadata,
				dialect,
				buildCustomFunctionRegistry( queryEngineOptions )
		);
	}

	private static SqmFunctionRegistry buildCustomFunctionRegistry(QueryEngineOptions queryEngineOptions) {
		if ( queryEngineOptions.getCustomSqmFunctionRegistry() == null ) {
			final Map<String, SqmFunctionDescriptor> customSqlFunctionMap = queryEngineOptions.getCustomSqlFunctionMap();
			if ( customSqlFunctionMap == null || customSqlFunctionMap.isEmpty() ) {
				return null;
			}
			else {
				SqmFunctionRegistry customSqmFunctionRegistry = new SqmFunctionRegistry();
				customSqlFunctionMap.forEach( customSqmFunctionRegistry::register );
				return customSqmFunctionRegistry;
			}
		}
		else {
			return queryEngineOptions.getCustomSqmFunctionRegistry();
		}
	}

	private final NamedObjectRepository namedObjectRepository;
	private final SqmCriteriaNodeBuilder criteriaBuilder;
	private final HqlTranslator hqlTranslator;
	private final SqmTranslatorFactory sqmTranslatorFactory;
	private final NativeQueryInterpreter nativeQueryInterpreter;
	private final QueryInterpretationCache interpretationCache;
	private final SqmFunctionRegistry sqmFunctionRegistry;
	private final TypeConfiguration typeConfiguration;
	private final int preferredSqlTypeCodeForBoolean;
	private final QueryParameterBindingTypeResolver queryParameterBindingTypeResolver;

	private QueryEngine(
			SessionFactoryImplementor sessionFactory,
			HqlTranslator hqlTranslator,
			SqmTranslatorFactory sqmTranslatorFactory,
			MetadataImplementor metadata,
			Dialect dialect,
			SqmFunctionRegistry customFunctionRegistry) {
		this(
				sessionFactory.getUuid(),
				sessionFactory.getName(),
				sessionFactory.getSessionFactoryOptions().getJpaCompliance(),
				() -> sessionFactory.getRuntimeMetamodels().getJpaMetamodel(),
				sessionFactory.getSessionFactoryOptions().getCriteriaValueHandlingMode(),
				sessionFactory.getSessionFactoryOptions().getPreferredSqlTypeCodeForBoolean(),
				metadata.buildNamedQueryRepository( sessionFactory ),
				hqlTranslator,
				sqmTranslatorFactory,
				sessionFactory.getServiceRegistry().getService( NativeQueryInterpreter.class ),
				buildInterpretationCache( sessionFactory::getStatistics, sessionFactory.getProperties() ),
				metadata.getTypeConfiguration(),
				dialect,
				customFunctionRegistry,
				sessionFactory.getServiceRegistry(),
				sessionFactory
		);
	}

	private QueryEngine(
			String uuid,
			String name,
			JpaCompliance jpaCompliance,
			Supplier<JpaMetamodelImplementor> jpaMetamodelAccess,
			ValueHandlingMode criteriaValueHandlingMode,
			int preferredSqlTypeCodeForBoolean,
			NamedObjectRepository namedObjectRepository,
			HqlTranslator hqlTranslator,
			SqmTranslatorFactory sqmTranslatorFactory,
			NativeQueryInterpreter nativeQueryInterpreter,
			QueryInterpretationCache interpretationCache,
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			SqmFunctionRegistry userDefinedRegistry,
			ServiceRegistry serviceRegistry,
			SessionFactoryImplementor sessionFactory) {
		this.queryParameterBindingTypeResolver = sessionFactory;
		this.namedObjectRepository = Objects.requireNonNull( namedObjectRepository );
		this.sqmTranslatorFactory = sqmTranslatorFactory;
		this.nativeQueryInterpreter = Objects.requireNonNull( nativeQueryInterpreter );
		this.interpretationCache = interpretationCache;
		this.hqlTranslator = hqlTranslator;

		this.criteriaBuilder = new SqmCriteriaNodeBuilder(
				uuid,
				name,
				jpaCompliance.isJpaQueryComplianceEnabled(),
				this,
				jpaMetamodelAccess,
				serviceRegistry,
				criteriaValueHandlingMode,
				sessionFactory
		);

		this.sqmFunctionRegistry = new SqmFunctionRegistry();
		this.typeConfiguration = typeConfiguration;
		this.preferredSqlTypeCodeForBoolean = preferredSqlTypeCodeForBoolean;

		final FunctionContributions functionContributions = new FunctionContributionsImpl( serviceRegistry );

		dialect.initializeFunctionRegistry( functionContributions );
		if ( userDefinedRegistry != null ) {
			userDefinedRegistry.overlay( sqmFunctionRegistry );
		}

		for ( FunctionContributor contributor : sortedFunctionContributors( serviceRegistry ) ) {
			contributor.contributeFunctions( functionContributions );
		}

		if ( LOG_HQL_FUNCTIONS.isDebugEnabled() ) {
			sqmFunctionRegistry.getFunctionsByName().forEach(
					entry -> LOG_HQL_FUNCTIONS.debug( entry.getValue().getSignature( entry.getKey() ) )
			);
		}
	}

	/**
	 * Simplified constructor mainly meant for Quarkus use
	 */
	public QueryEngine(
			String uuid,
			String name,
			JpaMetamodelImplementor jpaMetamodel,
			ValueHandlingMode criteriaValueHandlingMode,
			int preferredSqlTypeCodeForBoolean,
			boolean useStrictJpaCompliance,
			NamedObjectRepository namedObjectRepository,
			NativeQueryInterpreter nativeQueryInterpreter,
			Dialect dialect,
			ServiceRegistry serviceRegistry) {
		this.namedObjectRepository = Objects.requireNonNull( namedObjectRepository );
		this.sqmTranslatorFactory = null;
		this.nativeQueryInterpreter = Objects.requireNonNull( nativeQueryInterpreter );

		this.sqmFunctionRegistry = new SqmFunctionRegistry();
		this.typeConfiguration = jpaMetamodel.getTypeConfiguration();
		this.preferredSqlTypeCodeForBoolean = preferredSqlTypeCodeForBoolean;

		dialect.contributeFunctions( new FunctionContributionsImpl( serviceRegistry ) );

		SessionFactoryImplementor sessionFactory = jpaMetamodel.getTypeConfiguration().getSessionFactory();
		this.queryParameterBindingTypeResolver = sessionFactory;
		this.criteriaBuilder = new SqmCriteriaNodeBuilder(
				uuid,
				name,
				false,
				this,
				() -> jpaMetamodel,
				serviceRegistry,
				criteriaValueHandlingMode,
				sessionFactory
		);

		final SqmCreationContext sqmCreationContext = new SqmCreationContext() {
			@Override
			public JpaMetamodelImplementor getJpaMetamodel() {
				return jpaMetamodel;
			}

			@Override
			public ServiceRegistry getServiceRegistry() {
				return serviceRegistry;
			}

			@Override
			public QueryEngine getQueryEngine() {
				return QueryEngine.this;
			}

			@Override
			public NodeBuilder getNodeBuilder() {
				return criteriaBuilder;
			}
		};

		//noinspection Convert2Lambda
		this.hqlTranslator = new StandardHqlTranslator(
				sqmCreationContext,
				new SqmCreationOptions() {
					@Override
					public boolean useStrictJpaCompliance() {
						return useStrictJpaCompliance;
					}
				}
		);

		this.interpretationCache = buildInterpretationCache(
				() -> serviceRegistry.getService( StatisticsImplementor.class ),
				serviceRegistry.getService( ConfigurationService.class ).getSettings()
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
		List<FunctionContributor> contributors = new ArrayList<>(
				serviceRegistry.getService( ClassLoaderService.class )
						.loadJavaServices( FunctionContributor.class ) );
		contributors.sort( comparingInt( FunctionContributor::ordinal )
				.thenComparing( a -> a.getClass().getCanonicalName() ) );
		return contributors;
	}

	private static QueryInterpretationCache buildInterpretationCache(
			Supplier<StatisticsImplementor> statisticsSupplier,
			Map<String,Object> properties) {
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

		if ( explicitUseCache || ( explicitMaxPlanSize != null && explicitMaxPlanSize > 0 ) ) {
			final int size = explicitMaxPlanSize != null
					? explicitMaxPlanSize
					: DEFAULT_QUERY_PLAN_MAX_COUNT;

			return new QueryInterpretationCacheStandardImpl( size, statisticsSupplier );
		}
		else {
			// disabled
			return new QueryInterpretationCacheDisabledImpl( statisticsSupplier );
		}
	}

	public void prepare(SessionFactoryImplementor sessionFactory, Metadata bootMetamodel) {
		namedObjectRepository.prepare( sessionFactory, bootMetamodel );
	}

	public void validateNamedQueries() {
		namedObjectRepository.validateNamedQueries( this );
	}
	public NamedObjectRepository getNamedObjectRepository() {
		return namedObjectRepository;
	}

	public SqmCriteriaNodeBuilder getCriteriaBuilder() {
		return criteriaBuilder;
	}

	public HqlTranslator getHqlTranslator() {
		return hqlTranslator;
	}

	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return sqmTranslatorFactory;
	}

	public NativeQueryInterpreter getNativeQueryInterpreter() {
		return nativeQueryInterpreter;
	}

	public QueryInterpretationCache getInterpretationCache() {
		return interpretationCache;
	}

	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return sqmFunctionRegistry;
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public void close() {
		if ( namedObjectRepository != null ) {
			namedObjectRepository.close();
		}

		if ( criteriaBuilder != null ) {
			criteriaBuilder.close();
		}

		if ( hqlTranslator != null ) {
			hqlTranslator.close();
		}

		if ( interpretationCache != null ) {
			interpretationCache.close();
		}

		if ( sqmFunctionRegistry != null ) {
			sqmFunctionRegistry.close();
		}
	}

	public int getPreferredSqlTypeCodeForBoolean() {
		return preferredSqlTypeCodeForBoolean;
	}


	@Override
	public <T> BindableType<? extends T> resolveParameterBindType(T bindValue) {
		return queryParameterBindingTypeResolver.resolveParameterBindType( bindValue );
	}

	@Override
	public <T> BindableType<T> resolveParameterBindType(Class<T> clazz) {
		return queryParameterBindingTypeResolver.resolveParameterBindType( clazz );
	}

	private class FunctionContributionsImpl implements FunctionContributions {
		private final ServiceRegistry serviceRegistry;

		public FunctionContributionsImpl(ServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}

		@Override
		public SqmFunctionRegistry getFunctionRegistry() {
			return sqmFunctionRegistry;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}
	}
}
