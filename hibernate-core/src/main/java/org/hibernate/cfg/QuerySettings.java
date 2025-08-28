/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import org.hibernate.Incubating;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.spi.QueryPlan;

import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

/**
 * @author Steve Ebersole
 *
 * @see org.hibernate.query.spi.QueryEngineOptions
 */
public interface QuerySettings {
	/**
	 * Boolean setting to control if the use of tech preview JSON functions in HQL is enabled.
	 *
	 * @settingDefault {@code false} (disabled) since the functions are still incubating.
	 *
	 * @see org.hibernate.query.spi.QueryEngineOptions#isJsonFunctionsEnabled
	 *
	 * @since 7.0
	 */
	@Incubating
	String JSON_FUNCTIONS_ENABLED = "hibernate.query.hql.json_functions_enabled";

	/**
	 * Boolean setting to control if the use of tech preview XML functions in HQL is enabled.
	 *
	 * @settingDefault {@code false} (disabled) since the functions are still incubating.
	 *
	 * @see org.hibernate.query.spi.QueryEngineOptions#isXmlFunctionsEnabled
	 *
	 * @since 7.0
	 */
	@Incubating
	String XML_FUNCTIONS_ENABLED = "hibernate.query.hql.xml_functions_enabled";

	/**
	 * Specifies that division of two integers should produce an integer on all
	 * databases. By default, integer division in HQL can produce a non-integer
	 * on Oracle, MySQL, or MariaDB.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.query.spi.QueryEngineOptions#isPortableIntegerDivisionEnabled
	 *
	 * @since 6.5
	 */
	String PORTABLE_INTEGER_DIVISION = "hibernate.query.hql.portable_integer_division";

	/**
	 * Specifies a {@link org.hibernate.query.hql.HqlTranslator} to use for HQL query
	 * translation.
	 *
	 * @see org.hibernate.query.spi.QueryEngineOptions#getCustomHqlTranslator
	 */
	String SEMANTIC_QUERY_PRODUCER = "hibernate.query.hql.translator";

	/**
	 * Specifies a {@link org.hibernate.query.sqm.sql.SqmTranslatorFactory} to use for
	 * HQL query translation.
	 *
	 * @see org.hibernate.query.spi.QueryEngineOptions#getCustomSqmTranslatorFactory
	 */
	String SEMANTIC_QUERY_TRANSLATOR = "hibernate.query.sqm.translator";

	/**
	 * Defines the "global" strategy to use for handling HQL and Criteria mutation queries.
	 * Specifies a {@link org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy}.
	 *
	 * @see org.hibernate.query.spi.QueryEngineOptions#getCustomSqmMultiTableMutationStrategy
	 */
	String QUERY_MULTI_TABLE_MUTATION_STRATEGY = "hibernate.query.mutation_strategy";

	/**
	 * Defines the "global" strategy to use for handling HQL and Criteria insert queries.
	 * Specifies a {@link org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy}.
	 *
	 * @see org.hibernate.query.spi.QueryEngineOptions#getCustomSqmMultiTableInsertStrategy
	 */
	String QUERY_MULTI_TABLE_INSERT_STRATEGY = "hibernate.query.insert_strategy";

	/**
	 * When enabled, specifies that named queries be checked during startup.
	 * <p>
	 * Mainly intended for use in test environments.
	 *
	 * @settingDefault {@code true} (enabled) - named queries are checked at startup.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyNamedQueryCheckingOnStartup(boolean)
	 */
	String QUERY_STARTUP_CHECKING = "hibernate.query.startup_check";

	/**
	 * By default, a {@linkplain jakarta.persistence.criteria.CriteriaBuilder criteria
	 * query} produces SQL with a JDBC bind parameter for any value specified via the
	 * criteria query API, except when the value is passed via
	 * {@link jakarta.persistence.criteria.CriteriaBuilder#literal(Object)}, in which
	 * case the value is "inlined" as a SQL literal.
	 * <p>
	 * This setting may be used to override this default behavior:
	 * <ul>
	 *     <li>the {@link org.hibernate.query.criteria.ValueHandlingMode#BIND "bind"}
	 *     mode uses bind parameters to pass such values to JDBC, but
	 *     <li>the {@link org.hibernate.query.criteria.ValueHandlingMode#INLINE "inline"}
	 *     mode inlines values as SQL literals.
	 * </ul>
	 * <p>
	 * In both modes:
	 * <ul>
	 * <li>values specified using {@code literal()} are inlined, and
	 * <li>values specified using
	 *     {@link jakarta.persistence.criteria.CriteriaBuilder#parameter(Class)} to create a
	 *     {@link jakarta.persistence.criteria.ParameterExpression criteria parameter} and
	 *     {@link jakarta.persistence.Query#setParameter(jakarta.persistence.Parameter,Object)}
	 *     to specify its argument are passed to JDBC using a bind parameter.
	 * </ul>
	 *
	 * @settingDefault {@link org.hibernate.query.criteria.ValueHandlingMode#BIND}.
	 *
	 * @since 6.0.0
	 *
	 * @see org.hibernate.query.criteria.ValueHandlingMode
	 * @see jakarta.persistence.criteria.CriteriaBuilder#literal(Object)
	 * @see jakarta.persistence.criteria.CriteriaBuilder#parameter(Class)
	 * @see org.hibernate.query.criteria.HibernateCriteriaBuilder#value(Object)
	 * @see org.hibernate.query.spi.QueryEngineOptions#getCriteriaValueHandlingMode
	 */
	String CRITERIA_VALUE_HANDLING_MODE = "hibernate.criteria.value_handling_mode";

	/**
	 * Specifies the default {@linkplain jakarta.persistence.criteria.Nulls precedence
	 * of null values} sorted via the HQL {@code ORDER BY} clause, either {@code none},
	 * {@code first}, or {@code last}, or an instance of the enumeration
	 * {@link jakarta.persistence.criteria.Nulls}.
	 *
	 * @settingDefault {@code none}.
	 *
	 * @see jakarta.persistence.criteria.Nulls
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyDefaultNullPrecedence(jakarta.persistence.criteria.Nulls)
	 */
	String DEFAULT_NULL_ORDERING = "hibernate.order_by.default_null_ordering";

	/**
	 * When enabled, specifies that {@linkplain org.hibernate.query.Query queries}
	 * created via {@link jakarta.persistence.EntityManager#createQuery(CriteriaQuery)},
	 * {@link jakarta.persistence.EntityManager#createQuery(CriteriaUpdate)} or
	 * {@link jakarta.persistence.EntityManager#createQuery(CriteriaDelete)} must
	 * create a copy of the passed criteria query object such that the resulting
	 * {@link jakarta.persistence.Query} object is not affected by mutation of the
	 * original {@linkplain CriteriaQuery criteria query}.
	 * <p>
	 * If disabled, it's assumed that the client does not mutate the criteria query
	 * after calling {@code createQuery()}. Thus, in the interest of performance, no
	 * copy is created.
	 * <p>
	 * The default behavior depends on how Hibernate is bootstrapped:
	 * <ul>
	 * <li>When bootstrapping Hibernate through the native bootstrap APIs, this setting
	 *     is disabled, that is, no copy of the criteria query object is made.
	 * <li>When bootstrapping Hibernate through the JPA SPI, this setting is enabled so
	 *     that criteria query objects are copied, as required by the JPA specification.
	 * </ul>
	 *
	 * @since 6.0
	 */
	String CRITERIA_COPY_TREE = "hibernate.criteria.copy_tree";

	/**
	 * When enabled, specifies that {@linkplain org.hibernate.query.Query queries}
	 * created via {@link jakarta.persistence.EntityManager#createQuery(CriteriaQuery)},
	 * {@link jakarta.persistence.EntityManager#createQuery(CriteriaUpdate)} or
	 * {@link jakarta.persistence.EntityManager#createQuery(CriteriaDelete)} cache
	 * their interpretations in the query plan cache.
	 * <p>
	 * If disabled, queries are interpreted on first access without caching.
	 *
	 * @settingDefault {@code false} (disabled) - criteria queries do not use query plan caching.
	 *
	 * @since 7.0
	 */
	String CRITERIA_PLAN_CACHE_ENABLED = "hibernate.criteria.plan_cache_enabled";

	/**
	 * When enabled, ordinal parameters (represented by the {@code ?} placeholder) in
	 * native queries will be ignored.
	 *
	 * @settingDefault {@code false} (disabled) - native queries are checked for ordinal placeholders.
	 *
	 * @see SessionFactoryOptions#getNativeJdbcParametersIgnored()
	 */
	String NATIVE_IGNORE_JDBC_PARAMETERS = "hibernate.query.native.ignore_jdbc_parameters";

	/**
	 * When enabled, native queries will return {@link java.sql.Date},
	 * {@link java.sql.Time}, and {@link java.sql.Timestamp} instead of the
	 * datetime types from {@link java.time}, recovering the behavior of
	 * native queries in Hibernate 6 and earlier.
	 *
	 * @settingDefault {@code false} (disabled) - native queries return
	 * {@link java.time.LocalDate}, {@link java.time.LocalTime}, and
	 * {@link java.time.LocalDateTime}.
	 *
	 * @since 7.0
	 */
	@Compatibility
	String NATIVE_PREFER_JDBC_DATETIME_TYPES = "hibernate.query.native.prefer_jdbc_datetime_types";

	/**
	 * When {@linkplain org.hibernate.query.Query#setMaxResults(int) pagination} is used
	 * in combination with a {@code fetch join} applied to a collection or many-valued
	 * association, the limit must be applied in-memory instead of on the database. This
	 * typically has terrible performance characteristics, and should be avoided.
	 * <p>
	 * When enabled, this setting specifies that an exception should be thrown for any
	 * query which would result in the limit being applied in-memory.
	 *
	 * @settingDefault {@code false} (disabled) - no exception is thrown and the
	 * possibility of terrible performance is left as a problem for the client to avoid.
	 *
	 * @since 5.2.13
	 */
	String FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH = "hibernate.query.fail_on_pagination_over_collection_fetch";

	/**
	 * Controls how {@linkplain org.hibernate.annotations.Immutable immutable}
	 * entities are handled when executing a bulk update or delete query. Valid
	 * options are enumerated by {@link ImmutableEntityUpdateQueryHandlingMode}:
	 * <ul>
	 *     <li>{@link ImmutableEntityUpdateQueryHandlingMode#ALLOW "allow"} specifies
	 *     that bulk updates and deletes of immutable entities are allowed, and
	 *     <li>{@link ImmutableEntityUpdateQueryHandlingMode#EXCEPTION "exception"}
	 *     specifies that a {@link org.hibernate.HibernateException} is thrown.
	 * </ul>
	 *
	 * @settingDefault {@link ImmutableEntityUpdateQueryHandlingMode#EXCEPTION "exception"}
	 *
	 * @apiNote The default for this setting was inverted in Hibernate 7.
	 *
	 * @since 5.2
	 *
	 * @see ImmutableEntityUpdateQueryHandlingMode
	 * @see org.hibernate.query.spi.QueryEngineOptions#getImmutableEntityUpdateQueryHandlingMode
	 */
	String IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE = "hibernate.query.immutable_entity_update_query_handling_mode";

	/**
	 * Determines how parameters occurring in a SQL {@code IN} predicate are expanded.
	 * By default, the {@code IN} predicate expands to include sufficient bind parameters
	 * to accommodate the specified arguments.
	 * <p>
	 * However, for database systems supporting execution plan caching, there's a
	 * better chance of hitting the cache if the number of possible {@code IN} clause
	 * parameter list lengths is smaller.
	 * <p>
	 * When this setting is enabled, we expand the number of bind parameters to an
	 * integer power of two: 4, 8, 16, 32, 64. Thus, if 5, 6, or 7 arguments are bound
	 * to a parameter, a SQL statement with 8 bind parameters in the {@code IN} clause
	 * will be used, and null will be bound to the left-over parameters.
	 *
	 * @since 5.2.17
	 */
	String IN_CLAUSE_PARAMETER_PADDING = "hibernate.query.in_clause_parameter_padding";

	/**
	 * When enabled, specifies that Hibernate should attempt to map parameter names
	 * given in a {@link org.hibernate.procedure.ProcedureCall} or
	 * {@link jakarta.persistence.StoredProcedureQuery} to named parameters of the
	 * JDBC {@link java.sql.CallableStatement}.
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#isUseOfJdbcNamedParametersEnabled()
	 *
	 * @since 6.0
	 */
	String CALLABLE_NAMED_PARAMS_ENABLED = "hibernate.query.proc.callable_named_params_enabled";

	/**
	 * When enabled, specifies that {@linkplain QueryPlan query plans} should be
	 * {@linkplain org.hibernate.query.spi.QueryInterpretationCache cached}.
	 * <p>
	 * By default, the query plan cache is enabled. It is also enabled if the configuration
	 * property {@value #QUERY_PLAN_CACHE_MAX_SIZE} is set.
	 *
	 * @settingDefault {@code true} (enabled) - query plan cache is enabled.
	 */
	String QUERY_PLAN_CACHE_ENABLED = "hibernate.query.plan_cache_enabled";

	/**
	 * The maximum number of entries in the
	 * {@linkplain org.hibernate.query.spi.QueryInterpretationCache
	 * query interpretation cache}.
	 * <p>
	 * The default maximum is
	 * {@value org.hibernate.query.spi.QueryEngine#DEFAULT_QUERY_PLAN_MAX_COUNT}.
	 *
	 * @see org.hibernate.query.spi.QueryInterpretationCache
	 */
	String QUERY_PLAN_CACHE_MAX_SIZE = "hibernate.query.plan_cache_max_size";

	/**
	 * The maximum number of {@link org.hibernate.query.ParameterMetadata} instances
	 * maintained by the {@link org.hibernate.query.spi.QueryInterpretationCache}.
	 * <p>
	 *
	 * @deprecated this setting is not currently used
	 */
	@Deprecated(since="6.0")
	String QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE = "hibernate.query.plan_parameter_metadata_max_size";

	/**
	 * For database supporting name parameters this setting allows to use named parameter is the procedure call.
	 * <p>
	 * By default, this is set to false
	 */
	String QUERY_PASS_PROCEDURE_PARAMETER_NAMES = "hibernate.query.pass_procedure_parameter_names";
}
