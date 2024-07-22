/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.extralazy;

import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		annotatedClasses = { School.class, Student.class, Championship.class },
		xmlMappings =
				{
						"org/hibernate/orm/test/extralazy/UserGroup.hbm.xml",
						"org/hibernate/orm/test/extralazy/Parent.hbm.xml",
						"org/hibernate/orm/test/extralazy/Child.hbm.xml"
				}
)
@SessionFactory
public class ExtraLazyTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Group" ).executeUpdate();
					session.createQuery( "delete from Document" ).executeUpdate();
					session.createQuery( "delete from User" ).executeUpdate();
					session.createQuery( "delete from Child" ).executeUpdate();
					session.createQuery( "delete from Parent" ).executeUpdate();
					session.createQuery( "delete from Student" ).executeUpdate();
					session.createQuery( "delete from School" ).executeUpdate();
					session.createQuery( "delete from Championship" ).executeUpdate();
				}
		);
	}

	@Test
	public void testOrphanDelete(SessionFactoryScope scope) {
		User user = new User( "gavin", "secret" );
		Document hia = new Document( "HiA", "blah blah blah", user );
		Document hia2 = new Document( "HiA2", "blah blah blah blah", user );
		scope.inTransaction(
				session ->
						session.persist( user )
		);

		scope.inTransaction(
				session -> {
					User gavin = session.get( User.class, "gavin" );
					assertThat( gavin.getDocuments().size(), is( 2 ) );
					gavin.getDocuments().remove( hia2 );
					assertFalse( gavin.getDocuments().contains( hia2 ) );
					assertTrue( gavin.getDocuments().contains( hia ) );
					assertThat( gavin.getDocuments().size(), is( 1 ) );
					assertFalse( Hibernate.isInitialized( gavin.getDocuments() ) );
				}
		);

		scope.inTransaction(
				session -> {
					User gavin = session.get( User.class, "gavin" );
					assertThat( gavin.getDocuments().size(), is( 1 ) );
					assertFalse( gavin.getDocuments().contains( hia2 ) );
					assertTrue( gavin.getDocuments().contains( hia ) );
					assertFalse( Hibernate.isInitialized( gavin.getDocuments() ) );
					assertNull( session.get( Document.class, "HiA2" ) );
					gavin.getDocuments().clear();
					assertTrue( Hibernate.isInitialized( gavin.getDocuments() ) );
					session.remove( gavin );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User gavin = new User( "gavin", "secret" );
					User turin = new User( "turin", "tiger" );
					Group g = new Group( "developers" );
					g.getUsers().put( "gavin", gavin );
					g.getUsers().put( "turin", turin );
					session.persist( g );
					gavin.getSession().put( "foo", new SessionAttribute( "foo", "foo bar baz" ) );
					gavin.getSession().put( "bar", new SessionAttribute( "bar", "foo bar baz 2" ) );
				}
		);

		scope.inTransaction(
				session -> {
					Group g = session.get( Group.class, "developers" );
					User gavin = (User) g.getUsers().get( "gavin" );
					User turin = (User) g.getUsers().get( "turin" );
					assertNotNull( gavin );
					assertNotNull( turin );
					assertNull( g.getUsers().get( "emmanuel" ) );
					assertFalse( Hibernate.isInitialized( g.getUsers() ) );
					assertNotNull( gavin.getSession().get( "foo" ) );
					assertNull( turin.getSession().get( "foo" ) );
					assertFalse( Hibernate.isInitialized( gavin.getSession() ) );
					assertFalse( Hibernate.isInitialized( turin.getSession() ) );
					session.remove( gavin );
					session.remove( turin );
					session.remove( g );
				}
		);
	}

	@Test
	public void testRemoveClear(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User gavin = new User( "gavin", "secret" );
					User turin = new User( "turin", "tiger" );
					Group g = new Group( "developers" );
					g.getUsers().put( "gavin", gavin );
					g.getUsers().put( "turin", turin );
					session.persist( g );
					gavin.getSession().put( "foo", new SessionAttribute( "foo", "foo bar baz" ) );
					gavin.getSession().put( "bar", new SessionAttribute( "bar", "foo bar baz 2" ) );
				}
		);

		User turin = scope.fromTransaction(
				session -> {
					Group g = session.get( Group.class, "developers" );
					User gavin = (User) g.getUsers().get( "gavin" );
					User t = (User) g.getUsers().get( "turin" );
					assertFalse( Hibernate.isInitialized( g.getUsers() ) );
					g.getUsers().clear();
					gavin.getSession().remove( "foo" );
					assertTrue( Hibernate.isInitialized( g.getUsers() ) );
					assertTrue( Hibernate.isInitialized( gavin.getSession() ) );
					return t;
				}
		);
		scope.inTransaction(
				session -> {
					Group g = session.get( Group.class, "developers" );
					assertTrue( g.getUsers().isEmpty() );
					assertFalse( Hibernate.isInitialized( g.getUsers() ) );
					User gavin = session.get( User.class, "gavin" );
					assertFalse( gavin.getSession().containsKey( "foo" ) );
					assertFalse( Hibernate.isInitialized( gavin.getSession() ) );
					session.remove( gavin );
					session.remove( turin );
					session.remove( g );
				}
		);
	}

	@Test
	public void testIndexFormulaMap(SessionFactoryScope scope) {
		User user1 = new User( "gavin", "secret" );
		User user2 = new User( "turin", "tiger" );
		scope.inTransaction(
				session -> {
					Group g = new Group( "developers" );
					g.getUsers().put( "gavin", user1 );
					g.getUsers().put( "turin", user2 );
					session.persist( g );
					user1.getSession().put( "foo", new SessionAttribute( "foo", "foo bar baz" ) );
					user1.getSession().put( "bar", new SessionAttribute( "bar", "foo bar baz 2" ) );
				}
		);

		scope.inTransaction(
				session -> {
					Group g = session.get( Group.class, "developers" );
					assertThat( g.getUsers().size(), is( 2 ) );
					g.getUsers().remove( "turin" );
					Map smap = ( (User) g.getUsers().get( "gavin" ) ).getSession();
					assertThat( smap.size(), is( 2 ) );
					smap.remove( "bar" );
				}
		);

		scope.inTransaction(
				session -> {
					Group g = session.get( Group.class, "developers" );
					assertThat( g.getUsers().size(), is( 1 ) );
					Map smap = ( (User) g.getUsers().get( "gavin" ) ).getSession();
					assertThat( smap.size(), is( 1 ) );
					User gavin = (User) g.getUsers().put( "gavin", user2 );
					session.remove( gavin );
					assertThat(
							session.createQuery( "select count(*) from SessionAttribute" ).uniqueResult(),
							is( 0L )
					);
				}
		);

		scope.inTransaction(
				session -> {
					Group g = session.get( Group.class, "developers" );
					assertThat( g.getUsers().size(), is( 1 ) );
					User turin = (User) g.getUsers().get( "turin" );
					Map smap = turin.getSession();
					assertThat( smap.size(), is( 0 ) );
					assertThat( session.createQuery( "select count(*) from User" ).uniqueResult(), is( 1L ) );
					session.remove( g );
					session.remove( turin );
					assertThat( session.createQuery( "select count(*) from User" ).uniqueResult(), is( 0L ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.DoubleQuoteQuoting.class)
	public void testSQLQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User gavin = new User( "gavin", "secret" );
					User turin = new User( "turin", "tiger" );
					gavin.getSession().put( "foo", new SessionAttribute( "foo", "foo bar baz" ) );
					gavin.getSession().put( "bar", new SessionAttribute( "bar", "foo bar baz 2" ) );
					session.persist( gavin );
					session.persist( turin );
					session.flush();
					session.clear();
					List results = session.getNamedQuery( "userSessionData" ).setParameter( "uname", "%in" ).list();
					assertThat( results.size(), is( 2 ) );
					gavin = (User) results.get( 0 ) ;
					assertThat( gavin.getName(), is( "gavin" ) );
					Assert.assertTrue( Hibernate.isInitialized( gavin.getSession()) );
					assertThat( gavin.getSession().size(), is( 2 ) );
					session.createQuery( "delete SessionAttribute" ).executeUpdate();
					session.createQuery( "delete User" ).executeUpdate();
				}
		);
	}

	@Test
	public void testExtraLazySet(SessionFactoryScope scope) {
		User gavin = new User( "gavin", "secret" );
		Document d1 = new Document( "d1", "blah", gavin );
		Document d2 = new Document( "d2", "blah blah", gavin );
		scope.inTransaction(
				session -> {
					session.persist( gavin );
					new Document("d3", "blah blah blah", gavin);
					assertTrue(gavin.getDocuments().size() == 3);
				}
		);

		scope.inTransaction(
				session -> {
					User person = (User) session.get(User.class, gavin.getName());
					Document d4 = new Document( "d4", "blah blah blah blah", person );
					assertTrue(person.getDocuments().size() == 4);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.DoubleQuoteQuoting.class)
	public void testSQLQuery2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User gavin = new User( "gavin", "secret" );
					User turin = new User( "turin", "tiger" );
					gavin.getSession().put( "foo", new SessionAttribute( "foo", "foo bar baz" ) );
					gavin.getSession().put( "bar", new SessionAttribute( "bar", "foo bar baz 2" ) );
					session.persist( gavin );
					session.persist( turin );
					session.flush();
					session.clear();
					List results = session.getNamedQuery( "userSessionData" ).setParameter( "uname", "%in" ).list();
					assertThat( results.size(), is( 2 ) );
					gavin = (User) results.get( 0 );
					assertThat( gavin.getName(), is( "gavin" ) );
					assertThat( gavin.getSession().size(), is( 2 ) );
					session.createQuery( "delete SessionAttribute" ).executeUpdate();
					session.createQuery( "delete User" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4294")
	public void testMap(SessionFactoryScope scope) {
		Parent parent = new Parent();
		Child child = new Child();
		scope.inTransaction(
				session -> {
					child.setFirstName( "Ben" );
					parent.getChildren().put( child.getFirstName(), child );
					child.setParent( parent );
					session.persist( parent );
				}
		);

		// END PREPARE SECTION
		scope.inSession(
				session -> {
					Parent parent2 = session.get( Parent.class, parent.getId() );
					Child child2 = parent2.getChildren()
							.get( child.getFirstName() ); // causes SQLGrammarException because of wrong condition: 	where child0_.PARENT_ID=? and child0_.null=?
					assertNotNull( child2 );
				}
		);

	}

	@Test
	@TestForIssue(jiraKey = "HHH-10874")
	public void testWhereClauseOnBidirectionalCollection(SessionFactoryScope scope) {
		School school = new School( 1 );
		Student gavin = new Student( "gavin", 4 );
		Student turin = new Student( "turin", 3 );
		Student mike = new Student( "mike", 5 );
		Student fred = new Student( "fred", 2 );

		scope.inTransaction(
				session -> {
					session.persist( school );

					gavin.setSchool( school );
					turin.setSchool( school );
					mike.setSchool( school );
					fred.setSchool( school );

					session.persist( gavin );
					session.persist( turin );
					session.persist( mike );
					session.persist( fred );
				}
		);

		scope.inSession(
				session -> {
					School school2 = session.get( School.class, 1 );

					assertThat( school2.getStudents().size(), is( 4 ) );

					assertThat( school2.getTopStudents().size(), is( 2 ) );
					assertTrue( school2.getTopStudents().contains( gavin ) );
					assertTrue( school2.getTopStudents().contains( mike ) );

					assertThat( school2.getStudentsMap().size(), is( 2 ) );
					assertTrue( school2.getStudentsMap().containsKey( gavin.getId() ) );
					assertTrue( school2.getStudentsMap().containsKey( mike.getId() ) );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-3319")
	public void testWhereClauseOnUnidirectionalCollection(SessionFactoryScope scope) {
		Championship championship = new Championship( 1 );
		Student gavin = new Student( "gavin", 4 );
		Student turin = new Student( "turin", 3 );
		Student mike = new Student( "mike", 5 );
		Student fred = new Student( "fred", 2 );

		scope.inTransaction(
				session -> {
					session.persist( championship );

					championship.getStudents().add( gavin );
					championship.getStudents().add( turin );
					championship.getStudents().add( mike );
					championship.getStudents().add( fred );

					session.persist( gavin );
					session.persist( turin );
					session.persist( mike );
					session.persist( fred );
				}
		);

		scope.inSession(
				session -> {
					Championship championship2 = session.get( Championship.class, 1 );
					assertThat( championship2.getStudents().size(), is( 2 ) );
					assertTrue( championship2.getStudents().contains( gavin ) );
					assertTrue( championship2.getStudents().contains( mike ) );
				}
		);
	}

}

