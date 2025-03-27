/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = CriteriaCteOffsetFetchTest.Product.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17769" )
public class CriteriaCteOffsetFetchTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Product( "product_1", 1d ) );
			session.persist( new Product( "product_2", 100d ) );
			session.persist( new Product( "product_3", 200d ) );
			session.persist( new Product( "product_4", 30d ) );
			session.persist( new Product( "product_5", 20d ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Product" ).executeUpdate() );
	}

	@Test
	public void testMainQueryOffset(SessionFactoryScope scope) {
		executeQuery( scope, 1, null, false );
	}

	@Test
	public void testSetFirstResult(SessionFactoryScope scope) {
		executeQuery( scope, 1, null, true );
	}

	@Test
	public void testMainQueryFetch(SessionFactoryScope scope) {
		executeQuery( scope, null, 2, false );
	}

	@Test
	public void testSetMaxResults(SessionFactoryScope scope) {
		executeQuery( scope, null, 2, true );
	}

	@Test
	public void testMainQueryOffsetAndFetch(SessionFactoryScope scope) {
		executeQuery( scope, 1, 2, false );
	}

	@Test
	public void testSetFirstAndMaxResults(SessionFactoryScope scope) {
		executeQuery( scope, 1, 2, true );
	}

	private void executeQuery(
			SessionFactoryScope scope,
			Integer firstResult,
			Integer maxResults,
			boolean queryOptions) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Product> cq = cb.createQuery( Product.class );
			final JpaRoot<Product> root = cq.from( Product.class );

			final JpaCteCriteria<Tuple> first = cq.with( cteQuery( cb, "id_for_first", 1.2d ) );
			final JpaRoot<Tuple> fromFirst = cq.from( first );
			final JpaCteCriteria<Tuple> second = cq.with( cteQuery( cb, "id_for_second", 150.0d ) );
			final JpaRoot<Tuple> fromSecond = cq.from( second );

			cq.select( root ).where( cb.and(
					cb.equal( root.get( "id" ), fromFirst.get( "id_for_first" ) ),
					cb.equal( root.get( "id" ), fromSecond.get( "id_for_second" ) )
			) ).orderBy( cb.asc( root.get( "name" ) ) );
			if ( !queryOptions ) {
				if ( firstResult != null ) {
					cq.offset( firstResult );
				}
				if ( maxResults != null ) {
					cq.fetch( maxResults );
				}
			}

			final Query<Product> query = session.createQuery( cq );
			if ( queryOptions ) {
				if ( firstResult != null ) {
					query.setFirstResult( firstResult );
				}
				if ( maxResults != null ) {
					query.setMaxResults( maxResults );
				}
			}
			final List<Product> resultList = query.getResultList();
			assertThat( resultList ).hasSize( 2 );
			final List<String> names = new ArrayList<>( 3 );
			names.add( firstResult == null ? "product_2" : "product_5" );
			names.add( "product_4" );
			names.sort( String::compareTo );
			assertThat( resultList.stream().map( Product::getName ) ).containsExactly( names.toArray( new String[0] ) );
		} );
	}

	private CriteriaQuery<Tuple> cteQuery(CriteriaBuilder cb, String idAlias, double param) {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<Product> root = cq.from( Product.class );
		final Path<Double> price = root.get( "price" );
		return cq.multiselect( root.get( "id" ).alias( idAlias ) ).where( cb.and(
				cb.isNotNull( price ),
				param < 100 ? cb.greaterThan( price, param ) : cb.lessThan( price, param )
		) );
	}

	@Entity( name = "Product" )
	public static class Product {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private Double price;

		public Product() {
		}

		public Product(String name, Double price) {
			this.name = name;
			this.price = price;
		}

		public String getName() {
			return name;
		}

		public Double getPrice() {
			return price;
		}
	}
}
