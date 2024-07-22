/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.reattachment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Test of proxy reattachment semantics
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/reattachment/Mappings.hbm.xml"
)
@SessionFactory
public class ProxyReattachmentTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Parent" ).executeUpdate();
					session.createQuery( "delete from Child" ).executeUpdate();
				}
		);
	}

	@Test
	public void testUpdateAfterEvict(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( "p" );
					session.persist( p );
				}
		);

		Parent parent = scope.fromTransaction(
				session -> {
					Parent p = session.load( Parent.class, "p" );
					// evict...
					session.evict( p );
					// now try to reattach...
					return session.merge( p );
				}
		);

		scope.inTransaction(
				session ->
						session.remove( parent )
		);
	}

	@Test
	public void testUpdateAfterClear(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( "p" );
					session.persist( p );
				}
		);

		Parent parent = scope.fromTransaction(
				session -> {
					Parent p = session.load( Parent.class, "p" );
					// clear...
					session.clear();
					// now try to reattach...
					return session.merge( p );
				}
		);

		scope.inTransaction(
				session ->
						session.remove( parent )
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
					session.remove( parent );
					// re-attach
					session.persist( parent );
				}
		);
	}
}
