/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.Map;

import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;

/**
 * User configuration options related to the {@link QueryEngine}.
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
	 */
	HqlTranslator getCustomHqlTranslator();

	/**
	 * Factory for translators transforming an SQM tree into a different form.
	 * For standard ORM implementations this will generally be some form of SQL tree.
	 *
	 * @see org.hibernate.sql.ast.tree
	 *
	 * @see org.hibernate.cfg.QuerySettings#SEMANTIC_QUERY_TRANSLATOR
	 */
	SqmTranslatorFactory getCustomSqmTranslatorFactory();

	/**
	 * User defined SQM functions available for use in HQL and Criteria.
	 * <p>
	 * Ultimately made available to the {@link SqmTranslatorFactory} for use
	 * in translating an SQM tree.
	 * <p>
	 * Can be used in conjunction with {@link #getCustomSqmFunctionRegistry()},
	 * but generally one or the other will be used.
	 */
	Map<String, SqmFunctionDescriptor> getCustomSqlFunctionMap();

	/**
	 * User supplied registry of SQM functions available for use in HQL and Criteria
	 * <p>
	 * Can be used in conjunction with {@link #getCustomSqlFunctionMap()}, but generally
	 * one or the other will be used.
	 */
	SqmFunctionRegistry getCustomSqmFunctionRegistry();

	/**
	 * Contract for handling SQM trees representing mutation (UPDATE or DELETE) queries
	 * where the target of the mutation is a multi-table entity.
	 *
	 * @see org.hibernate.cfg.QuerySettings#QUERY_MULTI_TABLE_MUTATION_STRATEGY
	 */
	SqmMultiTableMutationStrategy getCustomSqmMultiTableMutationStrategy();

	/**
	 * Contract for handling SQM trees representing insertion (INSERT) queries where the
	 * target of the mutation is a multi-table entity.
	 *
	 * @see org.hibernate.cfg.QuerySettings#QUERY_MULTI_TABLE_INSERT_STRATEGY
	 */
	SqmMultiTableInsertStrategy getCustomSqmMultiTableInsertStrategy();

	/**
	 * Contract for handling SQM trees representing mutation (UPDATE or DELETE) queries
	 * where the target of the mutation is a multi-table entity.
	 *
	 * @see org.hibernate.cfg.QuerySettings#QUERY_MULTI_TABLE_MUTATION_STRATEGY
	 */
	SqmMultiTableMutationStrategy resolveCustomSqmMultiTableMutationStrategy(EntityMappingType rootEntityDescriptor, RuntimeModelCreationContext creationContext);

	/**
	 * Contract for handling SQM trees representing insertion (INSERT) queries where the
	 * target of the mutation is a multi-table entity.
	 *
	 * @see org.hibernate.cfg.QuerySettings#QUERY_MULTI_TABLE_INSERT_STRATEGY
	 */
	SqmMultiTableInsertStrategy resolveCustomSqmMultiTableInsertStrategy(EntityMappingType rootEntityDescriptor, RuntimeModelCreationContext creationContext);

	/**
	 * @see org.hibernate.cfg.JpaComplianceSettings
	 */
	JpaCompliance getJpaCompliance();

	/**
	 * @see org.hibernate.cfg.QuerySettings#CRITERIA_VALUE_HANDLING_MODE
	 */
	ValueHandlingMode getCriteriaValueHandlingMode();

	/**
	 * @see org.hibernate.cfg.QuerySettings#IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE
	 *
	 * @deprecated Since {@link ImmutableEntityUpdateQueryHandlingMode} is deprecated.
	 *             Use {@link #allowImmutableEntityUpdate} instead.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	ImmutableEntityUpdateQueryHandlingMode getImmutableEntityUpdateQueryHandlingMode();

	/**
	 * @see org.hibernate.cfg.QuerySettings#IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE
	 */
	boolean allowImmutableEntityUpdate();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#JSON_FUNCTIONS_ENABLED
	 */
	boolean isJsonFunctionsEnabled();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#XML_FUNCTIONS_ENABLED
	 */
	boolean isXmlFunctionsEnabled();

	/**
	 * Should HQL integer division HQL should produce an integer on
	 * Oracle, MySQL, and MariaDB, where the {@code /} operator produces
	 * a non-integer.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PORTABLE_INTEGER_DIVISION
	 */
	boolean isPortableIntegerDivisionEnabled();

	String getSessionFactoryName();

	String getUuid();
}
