/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration.criteria;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.spatial.criteria.GeolatteSpatialCriteriaBuilder;
import org.hibernate.spatial.criteria.JTSSpatialCriteriaBuilder;
import org.hibernate.spatial.criteria.SpatialCriteriaBuilder;
import org.hibernate.spatial.integration.Model;
import org.hibernate.spatial.testing.HSReflectionUtil;
import org.hibernate.spatial.testing.SpatialTestBase;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.JUnitException;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract test factory for most {@link SpatialCriteriaBuilder}
 * predicate functions
 *
 * @author Marco Belladelli
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
abstract public class SpatialCriteriaTest extends SpatialTestBase {

	@Override
	public TestSupport.TestDataPurpose purpose() {
		return TestSupport.TestDataPurpose.SpatialFunctionsData;
	}

	abstract public Stream<PredicateRegexes.PredicateRegex> getTestRegexes();

	@TestFactory
	public Stream<DynamicTest> testFactory() {
		return
				getTestRegexes()
						.flatMap( rp -> Stream.of(
								new Args( rp.predicate, rp.regex, Model.GLMODEL ),
								new Args( rp.predicate, rp.regex, Model.JTSMODEL )
						) )
						.map( args -> DynamicTest.dynamicTest(
								displayName( args ),
								testPredicate(
										args.method,
										args.regex,
										args.model
								)
						) );
	}

	public Executable testPredicate(final String key, final String regex, Model model) {
		return () -> scope.inSession( session -> {
			SQLStatementInspector inspector = SQLStatementInspector.extractFromSession( session );
			inspector.clear();
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			// unwrap CriteriaBuilder based on model
			Class spatialCriteriaBuilderClass = model == Model.JTSMODEL ?
					JTSSpatialCriteriaBuilder.class :
					GeolatteSpatialCriteriaBuilder.class;
			SpatialCriteriaBuilder criteriaBuilder = (SpatialCriteriaBuilder) cb.unwrap( spatialCriteriaBuilderClass );
			// generate adequate query for each predicate method and model
			CriteriaQuery query = getCriteriaQuery(
					key,
					criteriaBuilder,
					model
			);
			List resultList = session.createQuery( query ).getResultList();
			String stmt = inspector.getSqlQueries()
					.get( 0 )
					.toLowerCase( Locale.ROOT );
			// check that generated query matches expected regex
			assertTrue(
					stmt.matches( regex ),
					String.format( Locale.ROOT, "Statement didn't match regex:\n%s\n%s\n", stmt, regex )
			);
		} );
	}

	private CriteriaQuery getCriteriaQuery(
			String key,
			SpatialCriteriaBuilder criteriaBuilder, Model model) {
		CriteriaQuery query = criteriaBuilder.createQuery( model.entityClass );
		Root root = query.from( model.entityClass );
		Method method = predicateMethod( key );
		finalizeQuery( criteriaBuilder, model, query, root, method );
		return query;
	}

	private void finalizeQuery(
			SpatialCriteriaBuilder criteriaBuilder,
			Model model,
			CriteriaQuery query,
			Root root,
			Method method) {
		try {
			query.select( root ).where( (Expression<Boolean>) method.invoke(
					criteriaBuilder,
					root.get( "geom" ),
					model.from.apply( filterGeometry )
			) );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new JUnitException( "Failure to invoke Geometry Predicate", e );
		}
	}

	private Method predicateMethod(String key) {
		return HSReflectionUtil.getStaticMethod(
				SpatialCriteriaBuilder.class,
				key,
				Expression.class,
				Object.class
		);
	}

	private String displayName(Args args) {
		return String.format( "Predicate %s on %s", args.method, args.model.entityClass.getSimpleName() );
	}

	static class Args {
		final String method;
		final String regex;
		final Model model;

		public Args(String method, String regex, Model model) {
			this.method = method;
			this.regex = regex;
			this.model = model;
		}
	}
}
