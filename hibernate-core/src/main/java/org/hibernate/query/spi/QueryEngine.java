/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.query.QueryLogger;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.hql.internal.StandardHqlTranslator;
import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.internal.QueryInterpretationCacheDisabledImpl;
import org.hibernate.query.internal.QueryInterpretationCacheStandardImpl;
import org.hibernate.query.named.NamedQueryRepository;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.internal.SqmCreationOptionsStandard;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.sql.StandardSqmTranslatorFactory;
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
		final SqmCreationContext sqmCreationContext = sessionFactory;
		final SessionFactoryOptions queryEngineOptions = sessionFactory.getSessionFactoryOptions();
		final SqmCreationOptions sqmCreationOptions = new SqmCreationOptionsStandard( sessionFactory );

		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
		final HqlTranslator hqlTranslator = resolveHqlTranslator(
				queryEngineOptions,
				dialect,
				sqmCreationContext,
				sqmCreationOptions
		);

		final SqmTranslatorFactory sqmTranslatorFactory = resolveSqmTranslatorFactory( queryEngineOptions, dialect );

		return new QueryEngine(
				() -> sessionFactory.getRuntimeMetamodels().getJpaMetamodel(),
				metadata.buildNamedQueryRepository( sessionFactory ),
				hqlTranslator,
				sqmTranslatorFactory,
				sessionFactory.getServiceRegistry().getService( NativeQueryInterpreter.class ),
				buildInterpretationCache( sessionFactory.getProperties() ),
				dialect,
				queryEngineOptions.getSqmFunctionRegistry(),
				sessionFactory.getServiceRegistry()
		);
	}

	private final NamedQueryRepository namedQueryRepository;
	private final SqmCriteriaNodeBuilder criteriaBuilder;
	private final HqlTranslator hqlTranslator;
	private final SqmTranslatorFactory sqmTranslatorFactory;
	private final NativeQueryInterpreter nativeQueryInterpreter;
	private final QueryInterpretationCache interpretationCache;
	private final SqmFunctionRegistry sqmFunctionRegistry;

	public QueryEngine(
			Supplier<JpaMetamodel> jpaMetamodelAccess,
			NamedQueryRepository namedQueryRepository,
			HqlTranslator hqlTranslator,
			SqmTranslatorFactory sqmTranslatorFactory,
			NativeQueryInterpreter nativeQueryInterpreter,
			QueryInterpretationCache interpretationCache,
			Dialect dialect,
			SqmFunctionRegistry userDefinedRegistry,
			ServiceRegistry serviceRegistry) {
		this.namedQueryRepository = namedQueryRepository;
		this.sqmTranslatorFactory = sqmTranslatorFactory;
		this.nativeQueryInterpreter = nativeQueryInterpreter;
		this.interpretationCache = interpretationCache;
		this.hqlTranslator = hqlTranslator;

		this.criteriaBuilder = new SqmCriteriaNodeBuilder(
				this,
				jpaMetamodelAccess,
				serviceRegistry
		);

		this.sqmFunctionRegistry = new SqmFunctionRegistry();
		dialect.initializeFunctionRegistry( this );
		if ( userDefinedRegistry != null ) {
			userDefinedRegistry.overlay( sqmFunctionRegistry );
		}
	}

	/**
	 * Simplified constructor mainly meant for Quarkus use
	 */
	public QueryEngine(
			JpaMetamodel jpaMetamodel,
			boolean useStrictJpaCompliance,
			NamedQueryRepository namedQueryRepository,
			NativeQueryInterpreter nativeQueryInterpreter,
			Dialect dialect,
			ServiceRegistry serviceRegistry) {
		this.namedQueryRepository = namedQueryRepository;
		this.sqmTranslatorFactory = null;
		this.nativeQueryInterpreter = nativeQueryInterpreter;

		this.sqmFunctionRegistry = new SqmFunctionRegistry();
		dialect.initializeFunctionRegistry( this );

		this.criteriaBuilder = new SqmCriteriaNodeBuilder(
				this,
				() -> jpaMetamodel,
				serviceRegistry
		);

		final SqmCreationContext sqmCreationContext = new SqmCreationContext() {
			@Override
			public JpaMetamodel getJpaMetamodel() {
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
				serviceRegistry.getService( ConfigurationService.class ).getSettings()
		);
	}

//	public QueryEngine(
//			JpaMetamodel domainModel,
//			ServiceRegistry serviceRegistry,
//			SessionFactoryOptions runtimeOptions,
//			SqmCreationContext sqmCreationContext,
//			SqmCreationOptions sqmCreationOptions,
//			Map properties,
//			NamedQueryRepository namedQueryRepository) {
//		final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
//		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
//		final Dialect dialect = jdbcEnvironment.getDialect();
//
//		this.namedQueryRepository = namedQueryRepository;
//
//		this.hqlTranslator = resolveHqlTranslator(
//				runtimeOptions,
//				dialect,
//				sqmCreationContext,
//				sqmCreationOptions
//		);
//
//		this.sqmTranslatorFactory = resolveSqmTranslatorFactory(
//				runtimeOptions,
//				dialect,
//				sqmCreationContext,
//				sqmCreationOptions
//		);
//
//		this.criteriaBuilder = new SqmCriteriaNodeBuilder(
//				this,
//				domainModel,
//				serviceRegistry
//		);
//
//		this.nativeQueryInterpreter = serviceRegistry.getService( NativeQueryInterpreter.class );
//
//		this.interpretationCache = buildInterpretationCache( properties );
//
//		this.sqmFunctionRegistry = new SqmFunctionRegistry();
//		dialect.initializeFunctionRegistry( this );
//		if ( runtimeOptions.getSqmFunctionRegistry() != null ) {
//			runtimeOptions.getSqmFunctionRegistry().overlay( sqmFunctionRegistry );
//		}
//	}

	private static HqlTranslator resolveHqlTranslator(
			SessionFactoryOptions runtimeOptions,
			Dialect dialect,
			SqmCreationContext sqmCreationContext,
			SqmCreationOptions sqmCreationOptions) {
		if ( runtimeOptions.getHqlTranslator() != null ) {
			return runtimeOptions.getHqlTranslator();
		}

		if ( dialect.getHqlTranslator() != null ) {
			return dialect.getHqlTranslator();
		}

		return new StandardHqlTranslator( sqmCreationContext, sqmCreationOptions );
	}

	private static SqmTranslatorFactory resolveSqmTranslatorFactory(
			SessionFactoryOptions runtimeOptions,
			Dialect dialect) {
		if ( runtimeOptions.getSqmTranslatorFactory() != null ) {
			return runtimeOptions.getSqmTranslatorFactory();
		}

		if ( dialect.getSqmTranslatorFactory() != null ) {
			return dialect.getSqmTranslatorFactory();
		}

		return new StandardSqmTranslatorFactory();
	}

	private static QueryInterpretationCache buildInterpretationCache(Map properties) {
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
					: QueryInterpretationCacheStandardImpl.DEFAULT_QUERY_PLAN_MAX_COUNT;

			return new QueryInterpretationCacheStandardImpl( size );
		}
		else {
			// disabled
			return QueryInterpretationCacheDisabledImpl.INSTANCE;
		}
	}

	public void prepare(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor bootMetamodel,
			BootstrapContext bootstrapContext) {
		namedQueryRepository.prepare( sessionFactory, bootMetamodel, bootstrapContext );

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

	public void close() {
		if ( namedQueryRepository != null ) {
			namedQueryRepository.close();
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
}
