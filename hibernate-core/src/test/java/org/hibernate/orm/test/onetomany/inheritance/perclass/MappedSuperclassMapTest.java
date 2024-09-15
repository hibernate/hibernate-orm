/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.onetomany.inheritance.perclass;

import java.util.List;
import java.util.Map.Entry;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey(value = "HHH-11005")
@DomainModel(
		annotatedClasses = {
				Book.class,
				Library.class,
				Product.class
		}
)
@SessionFactory
public class MappedSuperclassMapTest {

	private static final String SKU001 = "SKU001";
	private static final String SKU002 = "SKU002";
	private static final String WAR_AND_PEACE = "0140447938";
	private static final String ANNA_KARENINA = "0140449175";


	@BeforeEach
	public void init(SessionFactoryScope scope) {
		scope.inTransaction( sess -> {
			Library library = new Library();
			library.addBook( new Book( SKU001, WAR_AND_PEACE ) );
			library.addBook( new Book( SKU002, ANNA_KARENINA ) );
			sess.persist( library );
		} );
	}

	@Test
	public void lookupEntities(SessionFactoryScope scope) {
		scope.inTransaction( sess -> {
			List<Library> libraries = sess.createQuery( "FROM Library" ).list();
			assertEquals( 1, libraries.size() );
			Library library = libraries.get( 0 );
			assertNotNull( library );

			assertEquals( 2, library.getBooksOnInventory().size() );

			Book book = library.getBooksOnInventory().get( SKU001 );
			assertNotNull( book );
			Library Library = library;
			Library.getBooksOnIsbn().get( WAR_AND_PEACE );
			assertEquals( WAR_AND_PEACE, book.getIsbn() );

			book = library.getBooksOnInventory().get( SKU002 );
			assertNotNull( book );
			assertEquals( ANNA_KARENINA, book.getIsbn() );
		} );
	}

	@Test
	public void lookupEntities_entrySet(SessionFactoryScope scope) {
		scope.inTransaction( sess -> {
			List<Library> libraries = sess.createQuery( "FROM Library" ).list();
			assertEquals( 1, libraries.size() );
			Library library = libraries.get( 0 );
			assertNotNull( library );

			assertEquals( 2, library.getBooksOnInventory().size() );

			for ( Entry<String, Book> entry : library.getBooksOnInventory().entrySet() ) {
				entry.getKey();
				entry.getValue().getIsbn();
			}
		} );
	}

	@Test
	public void breakReferences(SessionFactoryScope scope) {
		scope.inTransaction( sess -> {
			List<Book> books = sess.createQuery( "FROM Book" ).list();
			assertEquals( 2, books.size() );

			for ( Book book : books ) {
				assertNotNull( book.getLibrary() );
				book.getInventoryCode();
				book.getLibrary().getEntid();
			}

			for ( Book book : books ) {
				book.getLibrary().removeBook( book );
			}
		} );

		scope.inTransaction( sess -> {
			List<Book> books = sess.createQuery( "FROM Book" ).list();
			assertEquals( 2, books.size() );

			for ( Book book : books ) {
				if ( book.getLibrary() == null ) {
					book.getInventoryCode();
				}
			}

			List<Library> libraries = sess.createQuery( "FROM Library" ).list();
			assertEquals( 1, libraries.size() );
			Library library = libraries.get( 0 );
			assertNotNull( library );

			assertEquals( 0, library.getBooksOnInventory().size() );
		} );
	}

	@AfterEach
	protected void cleanupTestData(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( sess -> {
			sess.createQuery( "delete from Book" ).executeUpdate();
			sess.createQuery( "delete from Library" ).executeUpdate();
		} );
	}
}
