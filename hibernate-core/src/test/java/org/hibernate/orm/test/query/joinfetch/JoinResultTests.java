/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.joinfetch;

import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;
import org.hibernate.internal.util.StringHelper;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.Product;
import org.hibernate.testing.orm.domain.retail.Vendor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests covering how joins and return-type affect the "shape" of the domain result
 *
 * @author Steve Ebersole
 */
@DomainModel(standardModels = StandardDomainModel.RETAIL)
@SessionFactory(useCollectingStatementInspector = true)
@Jira( "https://hibernate.atlassian.net/browse/HHH-16955" )
public class JoinResultTests {
	@Test
	void testSimpleJoin(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		final String query = "from Product join vendor";

		// supports `Product.class` - the single root
		scope.inTransaction( (session) -> {
			statementInspector.clear();
			final Product product = session
					.createQuery( query, Product.class )
					.getSingleResult();
			assertThat( product ).isNotNull();
			// only the Product columns should be selected
			assertThat( extractNumberOfSelections( statementInspector.getSqlQueries().get( 0 ) ) ).isEqualTo( 5 );
		} );

		// supports `Object[].class` - array[0] == the root && array[1] == the join
		scope.inTransaction( (session) -> {
			statementInspector.clear();
			final Object[] result = session
					.createQuery( query, Object[].class )
					.getSingleResult();
			assertThat( result ).isNotNull();
			assertThat( result.length ).isEqualTo( 2 );
			// both the Product and Vendor columns should be selected
			assertThat( extractNumberOfSelections( statementInspector.getSqlQueries().get( 0 ) ) ).isGreaterThan( 5 );
			assertThat( ( (Product) result[0] ).getVendor() ).isSameAs( result[1] );
		} );
	}

	@Test
	void testSimpleJoinFetch(SessionFactoryScope scope) {
		final String query = "from Product join fetch vendor";

		// supports `Product.class` - the single root
		scope.inTransaction( (session) -> {
			final Product product = session
					.createQuery( query, Product.class )
					.getSingleResult();
			assertThat( product ).isNotNull();
		} );

		// supports `Object[].class` - array[0] == the root
		scope.inTransaction( (session) -> {
			final Object[] result = session
					.createQuery( query, Object[].class )
					.getSingleResult();
			assertThat( result ).isNotNull();
			assertThat( result.length ).isEqualTo( 1 );
		} );
	}

	@Test
	void testSimpleCrossJoin(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		final String query = "from Product cross join vendor";

		// supports `Product.class` - the single root
		scope.inTransaction( (session) -> {
			statementInspector.clear();
			final Product product = session
					.createQuery( query, Product.class )
					.getSingleResult();
			assertThat( product ).isNotNull();
			// both the Product and Vendor columns should be selected
			assertThat( extractNumberOfSelections( statementInspector.getSqlQueries().get( 0 ) ) ).isEqualTo( 5 );
		} );

		// supports `Object[].class` - array[0] == the root && array[1] == the join
		scope.inTransaction( (session) -> {
			statementInspector.clear();
			final Object[] result = session
					.createQuery( query, Object[].class )
					.getSingleResult();
			assertThat( result ).isNotNull();
			assertThat( result.length ).isEqualTo( 2 );
			// both the Product and Vendor columns should be selected
			assertThat( extractNumberOfSelections( statementInspector.getSqlQueries().get( 0 ) ) ).isGreaterThan( 5 );
			assertThat( ( (Product) result[0] ).getVendor() ).isSameAs( result[1] );
		} );
	}

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Vendor vendor = new Vendor( 1, "ACME", "acme", "Some notes" );
			final Product product = new Product( 1, SafeRandomUUIDGenerator.safeRandomUUID(), vendor );
			session.persist( vendor );
			session.persist( product );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	private static int extractNumberOfSelections(String sql) {
		final int fromClauseStartPosition = sql.indexOf( " from " );
		final String selectClause = sql.substring( 0, fromClauseStartPosition );
		return StringHelper.count( selectClause, "," ) + 1;
	}
}
