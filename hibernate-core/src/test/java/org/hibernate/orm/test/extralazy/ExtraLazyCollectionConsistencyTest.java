/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.extralazy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/extralazy/UserGroup.hbm.xml"
		}
)
@SessionFactory
public class ExtraLazyCollectionConsistencyTest {

	private User user;

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			user = new User( "victor", "hugo" );
			session.persist( user );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Document" ).executeUpdate();
					session.createQuery( "delete from User" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9933")
	public void testSetSize(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			User _user = session.get( User.class, user.getName() );
			new Document( "Les Miserables", "sad", _user );
			assertThat( _user.getDocuments().size(), is( 1 ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-9933")
	public void testSetIterator(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			User _user = session.get( User.class, user.getName() );
			new Document( "Les Miserables", "sad", _user );
			assertTrue( _user.getDocuments().iterator().hasNext() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-9933")
	public void testSetIsEmpty(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			User _user = session.get( User.class, user.getName() );
			new Document( "Les Miserables", "sad", _user );
			assertFalse( _user.getDocuments().isEmpty() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-9933")
	public void testSetContains(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			User _user = session.get( User.class, user.getName() );
			Document document = new Document( "Les Miserables", "sad", _user );
			assertTrue( _user.getDocuments().contains( document ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-9933")
	public void testSetAdd(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			User _user = session.get( User.class, user.getName() );
			Document document = new Document();
			document.setTitle( "Les Miserables" );
			document.setContent( "sad" );
			document.setOwner( _user );
			assertTrue( _user.getDocuments().add( document ), "not added" );
			assertFalse( _user.getDocuments().add( document ), "added" );
		} );
	}

	@Test
	@JiraKey(value = "HHH-9933")
	public void testSetRemove(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			User _user = session.get( User.class, user.getName() );

			Document document = new Document( "Les Miserables", "sad", _user );
			assertTrue( _user.getDocuments().remove( document ), "not removed" );
		} );
	}

	@Test
	@JiraKey(value = "HHH-9933")
	public void testSetToArray(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			User _user = session.get( User.class, user.getName() );

			new Document( "Les Miserables", "sad", _user );
			assertThat( _user.getDocuments().toArray().length, is( 1 ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-9933")
	public void testSetToArrayTyped(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			User _user = session.get( User.class, user.getName() );

			new Document( "Les Miserables", "sad", _user );
			assertThat( _user.getDocuments().size(), is( 1 ) );
		} );
	}
}

