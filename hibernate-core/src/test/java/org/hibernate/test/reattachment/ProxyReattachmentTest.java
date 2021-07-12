/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.reattachment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of proxy reattachment semantics
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/test/reattachment/Mappings.hbm.xml"
)
@SessionFactory
public class ProxyReattachmentTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Parent" );
					session.createQuery( "delete from Child" );
				}
		);
	}

	@Test
	public void testUpdateAfterEvict(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( "p" );
					session.save( p );
				}
		);

		Parent parent = scope.fromTransaction(
				session -> {
					Parent p = session.load( Parent.class, "p" );
					// evict...
					session.evict( p );
					// now try to reattach...
					session.update( p );
					return p;
				}
		);

		scope.inTransaction(
				session ->
						session.delete( parent )
		);
	}

	@Test
	public void testUpdateAfterClear(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( "p" );
					session.save( p );
				}
		);

		Parent parent = scope.fromTransaction(
				session -> {
					Parent p = session.load( Parent.class, "p" );
					// clear...
					session.clear();
					// now try to reattach...
					session.update( p );
					return p;
				}
		);

		scope.inTransaction(
				session ->
						session.delete( parent )
		);
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testIterateWithClearTopOfLoop(SessionFactoryScope scope) {
		Set parents = new HashSet();
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 5; i++ ) {
						Parent p = new Parent( String.valueOf( i ) );
						Child child = new Child( "child" + i );
						child.setParent( p );
						p.getChildren().add( child );
						session.save( p );
						parents.add( p );
					}
				}
		);

		scope.inTransaction(
				session -> {
					int i = 0;
					List<Parent> fromParent = session.createQuery( "from Parent" ).list();
					for ( Parent p : fromParent ) {
						i++;
						if ( i % 2 == 0 ) {
							session.flush();
							session.clear();
						}
						assertEquals( 1, p.getChildren().size() );
					}
				}
		);

		scope.inTransaction(
				session -> {
					for ( Object parent : parents ) {
						session.delete( parent );
					}
				}
		);
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testIterateWithClearBottomOfLoop(SessionFactoryScope scope) {
		Set parents = new HashSet();
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 5; i++ ) {
						Parent p = new Parent( String.valueOf( i ) );
						Child child = new Child( "child" + i );
						child.setParent( p );
						p.getChildren().add( child );
						session.save( p );
						parents.add( p );
					}
				}
		);

		scope.inTransaction(
				session -> {
					int i = 0;
					List<Parent> fromParent = session.createQuery( "from Parent" ).list();
					for ( Parent p : fromParent ) {
						assertEquals( 1, p.getChildren().size() );
						i++;
						if ( i % 2 == 0 ) {
							session.flush();
							session.clear();
						}
					}
				}
		);

		scope.inTransaction(
				session -> {
					for ( Object parent : parents ) {
						session.delete( parent );
					}
				}
		);
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testIterateWithEvictTopOfLoop(SessionFactoryScope scope) {
		Set parents = new HashSet();
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 5; i++ ) {
						Parent p = new Parent( String.valueOf( i + 100 ) );
						Child child = new Child( "child" + i );
						child.setParent( p );
						p.getChildren().add( child );
						session.save( p );
						parents.add( p );
					}
				}
		);

		scope.inTransaction(
				session -> {
					List<Parent> fromParent = session.createQuery( "from Parent" ).list();
					for ( Parent p : fromParent ) {
						if ( p != null ) {
							session.evict( p );
						}
						assertEquals( 1, p.getChildren().size() );
					}
				}
		);

		scope.inTransaction(
				session -> {
					for ( Object parent : parents ) {
						session.delete( parent );
					}
				}
		);
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testIterateWithEvictBottomOfLoop(SessionFactoryScope scope) {
		Set parents = new HashSet();
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 5; i++ ) {
						Parent p = new Parent( String.valueOf( i + 100 ) );
						Child child = new Child( "child" + i );
						child.setParent( p );
						p.getChildren().add( child );
						session.save( p );
						parents.add( p );
					}
				}
		);

		scope.inTransaction(
				session -> {
					List<Parent> fromParent = session.createQuery( "from Parent" ).list();
					for ( Parent p : fromParent ) {
						assertEquals( 1, p.getChildren().size() );
						session.evict( p );
					}
				}
		);

		scope.inTransaction(
				session -> {
					for ( Object parent : parents ) {
						session.delete( parent );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8374")
	public void testRemoveAndReattachProxyEntity(SessionFactoryScope scope) {
		Parent p = new Parent( "foo" );
		scope.inTransaction(
				session -> {
					session.persist( p );
				}
		);

		scope.inTransaction(
				session -> {
					Parent parent = session.load( Parent.class, p.getName() );
					session.delete( parent );
					// re-attach
					session.persist( parent );
				}
		);
	}
}
