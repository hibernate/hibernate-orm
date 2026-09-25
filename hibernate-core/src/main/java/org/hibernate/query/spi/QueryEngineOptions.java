package org.hibernate.query.spi;

import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.hql.spi.HqlTranslator;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.sql.spi.SqmTranslatorFactory;

/**
 * User configuration options related to the {@link QueryEngine}.
 * <p>
 * Custom translators, strategies, and the custom function registry are optional.
 * A {@code null} value indicates that no custom implementation was supplied.
 *
 * @author Steve Ebersole
 */
public interface QueryEngineOptions {
	/**
	 * Translator for transforming HQL (as an Antlr parse tree) into an SQM tree.
	 *
	 * @see org.hibernate.query.hql
	 *
	 * @see org.hibernate.cfg.QuerySettings#SEMANTIC_QUERY_PRODUCER
	 * @see HqlTranslator
	 */
	@SPI(SPI.Role.SUPPLY)
	@Nullable
	HqlTranslator getCustomHqlTranslator();

	/**
	 * Factory for translators transforming an SQM tree into a different form.
	 * For standard ORM implementations this will generally be some form of SQL tree.
	 *
	 * @see org.hibernate.sql.ast.spi.query
	 *
	 * @see org.hibernate.cfg.QuerySettings#SEMANTIC_QUERY_TRANSLATOR
	 * @see SqmTranslatorFactory
	 */
	@SPI(SPI.Role.SUPPLY)
	@Nullable
	SqmTranslatorFactory getCustomSqmTranslatorFactory();

	/**
	 * User defined SQM functions available for use in HQL and Criteria.
	 * Returns an empty map if no custom functions were supplied.
	 * <p>
	 * Ultimately made available to the {@link SqmTranslatorFactory} for use
	 * in translating an SQM tree.
	 * <p>
	 * Can be used in conjunction with {@link #getCustomSqmFunctionRegistry()},
	 * but generally one or the other will be used.
	 */
	@Nonnull
	Map<String, SqmFunctionDescriptor> getCustomSqlFunctionMap();

	/**
	 * User supplied registry of SQM functions available for use in HQL and Criteria
	 * <p>
	 * Can be used in conjunction with {@link #getCustomSqlFunctionMap()}, but generally
	 * one or the other will be used.
	 */
	@Nullable
	SqmFunctionRegistry getCustomSqmFunctionRegistry();

	/**
	 * Contract for handling SQM trees representing mutation (UPDATE or DELETE) queries
	 * where the target of the mutation is a multi-table entity.
	 *
	 * @see org.hibernate.cfg.QuerySettings#QUERY_MULTI_TABLE_MUTATION_STRATEGY
	 */
	@Nullable
	SqmMultiTableMutationStrategy getCustomSqmMultiTableMutationStrategy();

	/**
	 * Contract for handling SQM trees representing insertion (INSERT) queries where the
	 * target of the mutation is a multi-table entity.
	 *
	 * @see org.hibernate.cfg.QuerySettings#QUERY_MULTI_TABLE_INSERT_STRATEGY
	 */
	@Nullable
	SqmMultiTableInsertStrategy getCustomSqmMultiTableInsertStrategy();

	/**
	 * Contract for handling SQM trees representing mutation (UPDATE or DELETE) queries
	 * where the target of the mutation is a multi-table entity.
	 *
	 * @see org.hibernate.cfg.QuerySettings#QUERY_MULTI_TABLE_MUTATION_STRATEGY
	 */
	@Nullable
	SqmMultiTableMutationStrategy resolveCustomSqmMultiTableMutationStrategy(
			@Nonnull EntityMappingType rootEntityDescriptor,
			@Nonnull RuntimeModelCreationContext creationContext);

	/**
	 * Contract for handling SQM trees representing insertion (INSERT) queries where the
	 * target of the mutation is a multi-table entity.
	 *
	 * @see org.hibernate.cfg.QuerySettings#QUERY_MULTI_TABLE_INSERT_STRATEGY
	 */
	@Nullable
	SqmMultiTableInsertStrategy resolveCustomSqmMultiTableInsertStrategy(
			@Nonnull EntityMappingType rootEntityDescriptor,
			@Nonnull RuntimeModelCreationContext creationContext);

	/**
	 * @see org.hibernate.cfg.JpaComplianceSettings
	 */
	@Nonnull
	JpaCompliance getJpaCompliance();

	/**
	 * @see org.hibernate.cfg.QuerySettings#CRITERIA_VALUE_HANDLING_MODE
	 */
	@Nonnull
	ValueHandlingMode getCriteriaValueHandlingMode();

	/**
	 * @see org.hibernate.cfg.QuerySettings#IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE
	 *
	 * @deprecated Since {@link ImmutableEntityUpdateQueryHandlingMode} is deprecated.
	 *             Use {@link #allowImmutableEntityUpdate} instead.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	@Nonnull
	ImmutableEntityUpdateQueryHandlingMode getImmutableEntityUpdateQueryHandlingMode();

	/**
	 * @see org.hibernate.cfg.QuerySettings#IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE
	 */
	boolean allowImmutableEntityUpdate();

	/**
	 * Should HQL integer division HQL should produce an integer on
	 * Oracle, MySQL, and MariaDB, where the {@code /} operator produces
	 * a non-integer.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PORTABLE_INTEGER_DIVISION
	 */
	boolean isPortableIntegerDivisionEnabled();

	@Nullable
	String getSessionFactoryName();

	@Nonnull
	String getUuid();

	boolean isSafeModeEnabled();
}
