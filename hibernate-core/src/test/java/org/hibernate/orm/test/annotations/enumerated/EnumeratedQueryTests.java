/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.annotations.enumerated;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@JiraKey( "HHH-16170" )
@DomainModel(annotatedClasses = EntityOfBasics.class)
@SessionFactory
public class EnumeratedQueryTests {
	@Test
	void testHqlUpdateAssignValueEnumSimpleNameLiteral(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityOfBasics entity = new EntityOfBasics( 1 );
			entity.setGender( EntityOfBasics.Gender.OTHER );
			entity.setConvertedGender( EntityOfBasics.Gender.OTHER );
			entity.setOrdinalGender( EntityOfBasics.Gender.OTHER );
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			session.createMutationQuery( "update EntityOfBasics set gender = MALE" ).executeUpdate();
			session.createMutationQuery( "update EntityOfBasics set convertedGender = MALE" ).executeUpdate();
			session.createMutationQuery( "update EntityOfBasics set ordinalGender = MALE" ).executeUpdate();
		} );

		scope.inTransaction( (session) -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertThat( entity.getGender() ).isEqualTo( EntityOfBasics.Gender.MALE );
			assertThat( entity.getConvertedGender() ).isEqualTo( EntityOfBasics.Gender.MALE );
			assertThat( entity.getOrdinalGender() ).isEqualTo( EntityOfBasics.Gender.MALE );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createMutationQuery( "delete EntityOfBasics" ).executeUpdate() );
	}
}
