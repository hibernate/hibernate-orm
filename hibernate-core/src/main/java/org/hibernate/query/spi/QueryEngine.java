/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Aggregation and encapsulation of the components Hibernate uses
 * to execute queries (HQL, Criteria and native)
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@Incubating
public interface QueryEngine {

	/**
	 * The default soft reference count.
	 */
	int DEFAULT_QUERY_PLAN_MAX_COUNT = 2048;

	NativeQueryInterpreter getNativeQueryInterpreter();

	QueryInterpretationCache getInterpretationCache();

	SqmFunctionRegistry getSqmFunctionRegistry();

	TypeConfiguration getTypeConfiguration();

	NodeBuilder getCriteriaBuilder();

	void close();

	void validateNamedQueries();

	NamedObjectRepository getNamedObjectRepository();

	HqlTranslator getHqlTranslator();

	SqmTranslatorFactory getSqmTranslatorFactory();

	default <R> HqlInterpretation<R> interpretHql(String hql, Class<R> resultType) {
		return getInterpretationCache().resolveHqlInterpretation( hql, resultType, getHqlTranslator() );
	}
}
