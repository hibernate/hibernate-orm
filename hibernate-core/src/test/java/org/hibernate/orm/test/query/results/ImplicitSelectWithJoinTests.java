/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.results;

import java.util.List;
import java.util.UUID;

import org.hibernate.ScrollableResults;
import org.hibernate.query.SelectionQuery;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.Product;
import org.hibernate.testing.orm.domain.retail.Vendor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory
@JiraKey( "HHH-15133" )
public class ImplicitSelectWithJoinTests {
	private static final String HQL = "from Product p join p.vendor v where v.name like '%Steve%'";
	private static final String HQL0 = "from Product this join this.vendor v where v.name like '%Steve%'";
	private static final String HQL2 = "select p " + HQL;
	private static final String HQL3 = "from Product q join q.vendor w, Product p join p.vendor v where v.name like '%Steve%' and w.name like '%Gavin%'";

	@Test
	public void testNoExpectedTypeWithThis(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<?> query = session.createSelectionQuery( HQL0 );

			{
				final List<?> results = query.list();
				assertThat( results ).hasSize( 1 );
				final Object result = results.get( 0 );
				assertThat( result ).isInstanceOf( Product.class );
			}

			try (ScrollableResults<?> results = query.scroll()) {
				assertThat( results.next() ).isTrue();
				final Object result = results.get();
				assertThat( result ).isInstanceOf( Product.class );
				assertThat( results.next() ).isFalse();
			}
		} );
	}

	@Test @FailureExpected(reason = "this functionality was disabled, and an exception is now thrown")
	public void testNoExpectedType(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<?> query = session.createSelectionQuery( HQL );

			{
				final List<?> results = query.list();
				assertThat( results ).hasSize( 1 );
				final Object result = results.get( 0 );
				assertThat( result ).isInstanceOf( Product.class );
			}

			try (ScrollableResults<?> results = query.scroll()) {
				assertThat( results.next() ).isTrue();
				final Object result = results.get();
				assertThat( result ).isInstanceOf( Product.class );
				assertThat( results.next() ).isFalse();
			}
		} );
	}

	@Test
	public void testProductResult(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<Product> query = session.createSelectionQuery( HQL, Product.class );

			{
				final List<Product> results = query.list();
				assertThat( results ).hasSize( 1 );
				final Product result = results.get( 0 );
				assertThat( result ).isNotNull();
			}

			try (ScrollableResults<Product> results = query.scroll()) {
				assertThat( results.next() ).isTrue();
				final Product result = results.get();
				assertThat( result ).isNotNull();
				assertThat( results.next() ).isFalse();
			}
		} );
	}

	@Test @FailureExpected(reason = "this functionality was disabled, and an exception is now thrown")
	public void testArrayResultNoResultType(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<?> query = session.createSelectionQuery( HQL3 );

			{
				final List<?> results = query.list();
				assertThat( results ).hasSize( 1 );
				final Object result = results.get( 0 );
				assertThat( result ).isNotNull();
				assertInstanceOf( Object[].class, result );
				assertThat( (Object[]) result ).hasSize(4);
				assertThat( (Object[]) result ).hasExactlyElementsOfTypes(Product.class, Vendor.class, Product.class, Vendor.class);
			}

			try (ScrollableResults<?> results = query.scroll()) {
				assertThat( results.next() ).isTrue();
				final Object result = results.get();
				assertThat( result ).isNotNull();
				assertInstanceOf( Object[].class, result );
				assertThat( (Object[]) result ).hasSize(4);
				assertThat( (Object[]) result ).hasExactlyElementsOfTypes(Product.class, Vendor.class, Product.class, Vendor.class);
				assertThat( results.next() ).isFalse();
			}
		} );

		// frankly, this would be more consistent and more backward-compatible
//		scope.inTransaction( (session) -> {
//			final SelectionQuery<?> query = session.createSelectionQuery( HQL );
//
//			{
//				final List<?> results = query.list();
//				assertThat( results ).hasSize( 1 );
//				final Object result = results.get( 0 );
//				assertThat( result ).isNotNull();
//				assertInstanceOf( Object[].class, result );
//				assertThat( (Object[]) result ).hasSize(2);
//				assertThat( (Object[]) result ).hasExactlyElementsOfTypes(Product.class, Vendor.class);
//			}
//
//			{
//				final ScrollableResults<?> results = query.scroll();
//				assertThat( results.next() ).isTrue();
//				final Object result = results.get();
//				assertThat( result ).isNotNull();
//				assertInstanceOf( Object[].class, result );
//				assertThat( (Object[]) result ).hasSize(2);
//				assertThat( (Object[]) result ).hasExactlyElementsOfTypes(Product.class, Vendor.class);
//				assertThat( results.next() ).isFalse();
//			}
//		} );
	}

	@Test
	public void testArrayResult(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<Object[]> query = session.createSelectionQuery( HQL3, Object[].class );

			{
				final List<Object[]> results = query.list();
				assertThat( results ).hasSize( 1 );
				final Object[] result = results.get( 0 );
				assertThat( result ).isNotNull();
				assertThat( result ).hasSize( 4 );
				assertThat( result[ 0 ] ).isNotNull();
				assertThat( result[ 1 ] ).isNotNull();
				assertThat( result[ 2 ] ).isNotNull();
				assertThat( result[ 3 ] ).isNotNull();
				assertThat( result ).hasExactlyElementsOfTypes(Product.class, Vendor.class, Product.class, Vendor.class);
			}

			try (ScrollableResults<Object[]> results = query.scroll()) {
				assertThat( results.next() ).isTrue();
				final Object[] result = results.get();
				assertThat( results.next() ).isFalse();
				assertThat( result ).isNotNull();
				assertThat( result ).hasSize( 4 );
				assertThat( result[ 0 ] ).isNotNull();
				assertThat( result[ 1 ] ).isNotNull();
				assertThat( result[ 2 ] ).isNotNull();
				assertThat( result[ 3 ] ).isNotNull();
				assertThat( result ).hasExactlyElementsOfTypes(Product.class, Vendor.class, Product.class, Vendor.class);
			}
		} );

		scope.inTransaction( (session) -> {
			final SelectionQuery<Object[]> query = session.createSelectionQuery( HQL, Object[].class );

			{
				final List<Object[]> results = query.list();
				assertThat( results ).hasSize( 1 );
				final Object[] result = results.get( 0 );
				assertThat( result ).isNotNull();
				assertThat( result ).hasSize( 2 );
				assertThat( result[ 0 ] ).isNotNull();
				assertThat( result[ 1 ] ).isNotNull();
				assertThat( result ).hasExactlyElementsOfTypes(Product.class, Vendor.class);
			}

			try (final ScrollableResults<Object[]> results = query.scroll()) {
				assertThat( results.next() ).isTrue();
				final Object[] result = results.get();
				assertThat( results.next() ).isFalse();
				assertThat( result ).isNotNull();
				assertThat( result ).hasSize( 2 );
				assertThat( result[ 0 ] ).isNotNull();
				assertThat( result[ 1 ] ).isNotNull();
				assertThat( result ).hasExactlyElementsOfTypes(Product.class, Vendor.class);
			}
		} );
	}

	@Test
	public void testExplicitSingleSelectionArrayResult(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<Object[]> query = session.createSelectionQuery( HQL2, Object[].class );

			{
				final List<Object[]> results = query.list();
				assertThat( results ).hasSize( 1 );

				final Object[] result = results.get( 0 );
				assertThat( result ).isNotNull();
				assertThat( result ).hasSize( 1 );
				assertThat( result[ 0 ] ).isNotNull();
				assertThat( result[ 0 ] ).isInstanceOf( Product.class );
			}

			try (ScrollableResults<Object[]> results = query.scroll()) {
				assertThat( results.next() ).isTrue();
				final Object[] result = results.get();
				assertThat( results.next() ).isFalse();
				assertThat( result ).isNotNull();
				assertThat( result ).hasSize( 1 );
				assertThat( result[ 0 ] ).isNotNull();
				assertThat( result[ 0 ] ).isInstanceOf( Product.class );
			}
		} );
	}

	@Test
	public void testExplicitSingleSelectionProductResult(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<Product> query = session.createSelectionQuery( HQL2, Product.class );

			{
				final List<Product> results = query.list();
				assertThat( results ).hasSize( 1 );
				final Product result = results.get( 0 );
				assertThat( result ).isNotNull();
			}

			try (ScrollableResults<Product> results = query.scroll()) {
				assertThat( results.next() ).isTrue();
				final Product result = results.get();
				assertThat( result ).isNotNull();
				assertThat( results.next() ).isFalse();
			}
		} );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Vendor steve = new Vendor( 1, "Steve's Curios", "Acme Corp." );
			final Product product1 = new Product( 10, UUID.fromString( "53886a8a-7082-4879-b430-25cb94415be8" ), steve );
			final Vendor gavin = new Vendor( 2, "Gavin & Associates", "Acme Corp." );
			final Product product2 = new Product( 11, UUID.fromString( "53886a8b-3083-4879-b431-25cb95515be9" ), gavin );
			session.persist( steve );
			session.persist( product1 );
			session.persist( gavin );
			session.persist( product2 );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
