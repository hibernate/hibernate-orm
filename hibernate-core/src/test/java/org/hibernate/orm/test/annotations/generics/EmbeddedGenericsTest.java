/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.generics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


@DomainModel(
		annotatedClasses = {
				Classes.Book.class,
				Classes.PopularBook.class
		}
)
@SessionFactory
public class EmbeddedGenericsTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Book" ).executeUpdate();
					session.createQuery( "delete from PopularBook" ).executeUpdate();
				}
		);
	}

	@Test
	public void testWorksWithGenericEmbedded(SessionFactoryScope scope) {
		Classes.Book b = new Classes.Book();
		scope.inTransaction(
				session -> {
					Classes.Edition<String> edition = new Classes.Edition<>();
					edition.name = "Second";
					b.edition = edition;
					session.persist( b );
				}
		);

		scope.inTransaction(
				session -> {
					Classes.Book retrieved = session.get( Classes.Book.class, b.id );
					assertEquals( "Second", retrieved.edition.name );
				}
		);

	}

	@Test
	public void testWorksWithGenericCollectionOfElements(SessionFactoryScope scope) {
		Classes.PopularBook b = new Classes.PopularBook();
		scope.inTransaction(
				session -> {
					Classes.Edition<String> edition = new Classes.Edition<>();
					edition.name = "Second";
					b.editions.add( edition );
					session.persist( b );
				}
		);

		scope.inTransaction(
				session -> {
					Classes.PopularBook retrieved = session.get( Classes.PopularBook.class, b.id );
					assertEquals( "Second", retrieved.editions.iterator().next().name );
					session.remove( retrieved );
				}
		);
	}

}
