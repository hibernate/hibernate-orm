/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.testing.orm.domain.gambit.EntityWithOneToOne;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = { SimpleEntity.class, EntityWithOneToOne.class } )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17225" )
public class SelectForeignKeyWithMissingFromTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new SimpleEntity( 1, "test" ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from SimpleEntity" ).executeUpdate() );
	}

	@Test
	public void testRightJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Integer result = session.createQuery(
					"select o.id from EntityWithOneToOne e right join e.other o",
					Integer.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testRightJoinEntityPath(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SimpleEntity result = session.createQuery(
					"select o from EntityWithOneToOne e right join e.other o",
					SimpleEntity.class
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1 );
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsFullJoin.class )
	public void testFullJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Integer result = session.createQuery(
					"select o.id from EntityWithOneToOne e full join e.other o",
					Integer.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 1 );
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsFullJoin.class )
	public void testFullJoinEntityPath(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SimpleEntity result = session.createQuery(
					"select o from EntityWithOneToOne e full join e.other o",
					SimpleEntity.class
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1 );
		} );
	}
}
