/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.SPI;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.query.hql.spi.HqlTranslator;
import org.hibernate.query.named.spi.NamedObjectRepository;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.sql.spi.SqmTranslatorFactory;
import org.hibernate.type.BindingContext;

/**
 * Aggregation and encapsulation of the components Hibernate uses
 * to execute queries (HQL, Criteria and native)
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@Incubating
public interface QueryEngine extends BindingContext {

	/**
	 * The default soft reference count.
	 */
	int DEFAULT_QUERY_PLAN_MAX_COUNT = 2048;

	NativeQueryInterpreter getNativeQueryInterpreter();

	QueryInterpretationCache getInterpretationCache();

	SqmFunctionRegistry getSqmFunctionRegistry();

	NodeBuilder getCriteriaBuilder();

	Dialect getDialect();

	void close();

	void validateNamedQueries();

	NamedObjectRepository getNamedObjectRepository();

	/**
	 * The global HQL translator selected from the explicit query-engine option or
	 * Hibernate's standard implementation.
	 *
	 * @see QueryEngineOptions#getCustomHqlTranslator()
	 * @see HqlTranslator
	 */
	@SPI({ SPI.Role.USE, SPI.Role.SUPPLY })
	HqlTranslator getHqlTranslator();

	/**
	 * The global SQM translator factory selected from the explicit query-engine
	 * option or Hibernate's standard implementation.
	 *
	 * @see QueryEngineOptions#getCustomSqmTranslatorFactory()
	 * @see SqmTranslatorFactory
	 */
	@SPI({ SPI.Role.USE, SPI.Role.SUPPLY })
	SqmTranslatorFactory getSqmTranslatorFactory();

	/**
	 * Avoid use of this, because Hibernate Processor can't do class loading
	 */
	@Internal
	ClassLoaderService getClassLoaderService();

	default <R> HqlInterpretation<R> interpretHql(String hql, Class<R> resultType) {
		return getInterpretationCache().resolveHqlInterpretation( hql, resultType, getHqlTranslator() );
	}
}
