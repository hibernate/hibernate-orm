/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.List;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		EntityOfBasics.class,
		EntityOfBasics.Gender.class,
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-16861")
public class EnumTest {


	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			EntityOfBasics male = new EntityOfBasics();
			male.setId( 20_000_000 );
			male.setGender( EntityOfBasics.Gender.MALE ); // Ordinal 0
			male.setOrdinalGender( EntityOfBasics.Gender.MALE ); // Ordinal 0

			EntityOfBasics female = new EntityOfBasics();
			female.setId( 20_000_001 );
			female.setGender( EntityOfBasics.Gender.FEMALE ); // Ordinal 1
			female.setOrdinalGender( EntityOfBasics.Gender.FEMALE ); // Ordinal 1

			session.persist( male );
			session.persist( female );
		});
	}


	@Test
	public void testOrdinalFunctionOnOrdinalEnum(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			List<Integer> femaleOrdinalFunction = session.createQuery(
							"select ordinal(ordinalGender) " +
									"from EntityOfBasics e " +
									"where e.ordinalGender = :gender",
							Integer.class
					)
					.setParameter( "gender", EntityOfBasics.Gender.FEMALE )
					.getResultList();

			List<Integer> femaleWithCast = session.createQuery(
							"select cast(e.ordinalGender as Integer) " +
									"from EntityOfBasics e " +
									"where e.ordinalGender = :gender",
							Integer.class
					)
					.setParameter( "gender", EntityOfBasics.Gender.FEMALE )
					.getResultList();

			assertThat( femaleOrdinalFunction ).hasSize( 1 );
			assertThat( femaleOrdinalFunction ).hasSameElementsAs( femaleWithCast );
		} );

	}


	@Test
	public void testOrdinalFunctionOnStringEnum(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Integer> femaleOrdinalFromString = session.createQuery(
							"select ordinal(gender)" +
									"from EntityOfBasics e " +
									"where e.gender = :gender",
							Integer.class
					)
					.setParameter( "gender", EntityOfBasics.Gender.FEMALE )
					.getResultList();

			assertThat( femaleOrdinalFromString ).hasSize( 1 );
			assertThat( femaleOrdinalFromString ).hasSameElementsAs( List.of( 1 ) );
		} );

	}

}
