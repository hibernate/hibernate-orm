/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.contribution.array;

import java.util.List;

import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.assertj.core.util.Arrays;

/**
 * @author Steve Ebersole
 */
@BootstrapServiceRegistry(
		javaServices = {
				@JavaService(role = TypeContributor.class, impl = StringArrayTypeContributor.class),
				@JavaService(role = FunctionContributor.class, impl = StringArrayFunctionContributor.class)
		}
)
@DomainModel(annotatedClasses = Post.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@SkipForDialect(dialectClass = H2Dialect.class, majorVersion = 2, reason = "Array support was changed to now be typed")
public class StringArrayContributorTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// save one with tags
			session.persist( new Post( 1, "with tags", "tag1", "tag2" ) );
			// and one without (null)
			session.persist( new Post( 2, "no tags" ) );
		} );

		scope.inTransaction( (session) -> {
			session.createQuery( "select p.tags from Post p" ).list();
		} );
	}

	@Test
	public void testAsQueryParameter(SessionFactoryScope scope) {
		scope.getSessionFactory().getQueryEngine()
				.getSqmFunctionRegistry()
				.registerNamed("array_contains",
						scope.getSessionFactory().getTypeConfiguration().standardBasicTypeForJavaType(Boolean.class));
		scope.inTransaction( (session) -> {
			session.createQuery( "select p from Post p where array_contains(:arr, p.title) = true" )
					.setParameter( "arr", Arrays.array( "a", "b" ) )
					.list();
		} );
	}

	@Test
	public void testParameterInJpaCriteria(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Post> cr = cb.createQuery( Post.class );
			Root<Post> root = cr.from( Post.class );
			cr.select( root ).where(
					ArrayPredicates.equalLength( cb, root.get( "tags" ), Arrays.array( "a", "b" ) )
			);
			List<Post> resultList = session.createQuery( cr ).getResultList();

		} );
	}


	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}

class ArrayPredicates {
	public static Predicate equalLength(
			CriteriaBuilder criteriaBuilder, Expression<? extends String[]> arr1,
			String[] arr2) {
		HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) criteriaBuilder;
		return cb.equal(
				criteriaBuilder.function( "array_length", long.class, arr1 ),
				criteriaBuilder.function( "array_length", long.class, cb.value( arr2 ) )
		);
	}
}
