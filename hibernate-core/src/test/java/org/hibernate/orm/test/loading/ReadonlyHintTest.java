/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.loading;

import java.util.Collections;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;

@DomainModel(
		annotatedClasses = {
				ReadonlyHintTest.SimpleEntity.class
		}
)
@SessionFactory
@JiraKey( value = "HHH-11958" )
public class ReadonlyHintTest {

	private static final String ORIGINAL_NAME = "original";
	private static final String CHANGED_NAME = "changed";

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SimpleEntity entity = new SimpleEntity();
			entity.id = 1L;
			entity.name = ORIGINAL_NAME;
			session.persist( entity );
		} );
	}

	@Test
	void testWithReadOnlyHint(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SimpleEntity fetchedEntity = session.find( SimpleEntity.class, 1L, Collections.singletonMap( HINT_READ_ONLY, true ) );
			fetchedEntity.name = CHANGED_NAME;
		} );

		scope.inTransaction( session -> {
			SimpleEntity fetchedEntity = session.find( SimpleEntity.class, 1L );
			assertThat(fetchedEntity.name, is( ORIGINAL_NAME ) );
		} );
	}

	@Test
	void testWithoutReadOnlyHint(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SimpleEntity fetchedEntity = session.find( SimpleEntity.class, 1L );
			fetchedEntity.name = CHANGED_NAME;
		} );

		scope.inTransaction( session -> {
			SimpleEntity fetchedEntity = session.find( SimpleEntity.class, 1L );
			assertThat(fetchedEntity.name, is( CHANGED_NAME ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createQuery( "delete from SimpleEntity" ).executeUpdate() );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Long id;

		private String name;
	}
}
