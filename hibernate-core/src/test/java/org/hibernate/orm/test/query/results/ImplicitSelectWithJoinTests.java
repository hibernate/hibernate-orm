/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory
@JiraKey( "HHH-15133" )
public class ImplicitSelectWithJoinTests {
	private static final String HQL = "from Product p join p.vendor v where v.name like '%Steve%'";
	private static final String HQL2 = "select p " + HQL;

	@Test
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

	@Test
	public void testArrayResult(SessionFactoryScope scope) {
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
			}

			try (final ScrollableResults<Object[]> results = query.scroll()) {
				assertThat( results.next() ).isTrue();
				final Object[] result = results.get();
				assertThat( results.next() ).isFalse();
				assertThat( result ).isNotNull();
				assertThat( result ).hasSize( 2 );
				assertThat( result[ 0 ] ).isNotNull();
				assertThat( result[ 1 ] ).isNotNull();
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
			final Vendor vendor = new Vendor( 1, "Steve's Curios", "Acme Corp." );
			final Product product = new Product( 10, UUID.fromString( "53886a8a-7082-4879-b430-25cb94415be8" ), vendor );
			session.persist( vendor );
			session.persist( product );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Product" ).executeUpdate();
			session.createMutationQuery( "delete Vendor" ).executeUpdate();
		} );
	}
}
