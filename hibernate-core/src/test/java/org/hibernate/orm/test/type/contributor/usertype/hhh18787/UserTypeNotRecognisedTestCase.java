/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.type.contributor.usertype.hhh18787;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = SomeEntity.class,
		typeContributors = TypesContributor.class
)
@SessionFactory
@JiraKey( "HHH-18787" )
class UserTypeNotRecognisedTestCase {

	@Test
	void customUserTypeWithTypeContributorRegistrationTest(SessionFactoryScope scope) {
		final var data = new CustomData( "whatever", 1L );
		scope.inTransaction( em -> {
			// creating some data, flushing and clearing context
			em.merge( new SomeEntity( new CustomData[] {data} ) );
		} );

		scope.inSession( em -> {
			// getting the data
			final var query = em.createQuery( "select s from SomeEntity s where id is not null", SomeEntity.class );
			final var resultList = query.getResultList();

			// single result should be present
			assertNotNull( resultList );
			assertEquals( 1, resultList.size() );

			// the entity shouldn't be null
			final var entity = resultList.get( 0 );
			assertNotNull( entity );

			// custom data array shouldn't be null and there should be single object present
			final var customData = entity.getCustomData();
			assertNotNull( customData );
			assertEquals( 1, customData.length );

			// custom data object shouldn't be null and all fields should be set with correct values
			final var singleCustomData = customData[0];
			assertNotNull( singleCustomData );
			assertEquals( data.getText(), singleCustomData.getText() );
			assertEquals( data.getNumber(), singleCustomData.getNumber() );
		} );
	}
}
