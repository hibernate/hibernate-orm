/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sql;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17605" )
public class NativeQueryMultipleEntityResultTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new BasicEntity( 1, "entity_1" ) );
			session.persist( new BasicEntity( 2, "entity_2" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BasicEntity" ).executeUpdate() );
	}

	@Test
	public void testEntityResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BasicEntity result = session.createNativeQuery(
					"select be.id, be.data from BasicEntity be where be.id = 1",
					BasicEntity.class
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1 );
			assertThat( result.getData() ).isEqualTo( "entity_1" );
		} );
	}

	@Test
	public void testSingleAlias(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BasicEntity result = session.createNativeQuery(
					"SELECT {be1.*}  FROM BasicEntity be1 WHERE be1.id = 1",
					BasicEntity.class,
					"be1"
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1 );
			assertThat( result.getData() ).isEqualTo( "entity_1" );
		} );
	}

	@Test
	public void testMultipleAliases(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tuple result = session.createNativeQuery(
							"SELECT {be1.*}, {be2.*}  FROM BasicEntity be1, BasicEntity be2 WHERE be1.id = 1 and be2.id = 2",
							Tuple.class
					)
					.addEntity( "be1", BasicEntity.class )
					.addEntity( "be2", BasicEntity.class )
					.getSingleResult();
			assertThat( result.get( 0, BasicEntity.class ).getId() ).isEqualTo( 1 );
			assertThat( result.get( 0, BasicEntity.class ).getData() ).isEqualTo( "entity_1" );
			assertThat( result.get( 1, BasicEntity.class ).getId() ).isEqualTo( 2 );
			assertThat( result.get( 1, BasicEntity.class ).getData() ).isEqualTo( "entity_2" );
		} );
	}
}
