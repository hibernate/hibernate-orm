/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.map;

import java.util.List;
import java.util.Map;

import org.hibernate.dialect.MariaDBDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = { "org/hibernate/orm/test/map/UserGroup.hbm.xml" }
)
@SessionFactory
public class MapIndexFormulaTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testIndexFunctionOnManyToManyMap(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Group g join g.users u where g.name = 'something' and index(u) = 'nada'", Group.class )
							.list();
					session.createQuery(
									"from Group g where g.name = 'something' and minindex(g.users) = 'nada'", Group.class )
							.list();
					session.createQuery(
									"from Group g where g.name = 'something' and maxindex(g.users) = 'nada'", Group.class )
							.list();
					session.createQuery(
									"from Group g where g.name = 'something' and maxindex(g.users) = 'nada' and maxindex(g.users) = 'nada'", Group.class )
							.list();
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = MariaDBDialect.class, reason = "HHH-18433")
	public void testIndexFormulaMap(SessionFactoryScope scope) {
		User turin = new User( "turin", "tiger" );
		scope.inTransaction(
				session -> {
					User gavin = new User( "gavin", "secret" );
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
					assertEquals( 2, g.getUsers().size() );
					g.getUsers().remove( "turin" );
					Map smap = ( (User) g.getUsers().get( "gavin" ) ).getSession();
					assertEquals( 2, smap.size() );
					smap.remove( "bar" );
				}
		);

		scope.inTransaction(
				session -> {
					Group g = session.get( Group.class, "developers" );
					assertEquals( 1, g.getUsers().size() );
					Map smap = ( (User) g.getUsers().get( "gavin" ) ).getSession();
					assertEquals( 1, smap.size() );
					User gavin = (User) g.getUsers().put( "gavin", session.merge( turin ) );
					session.remove( gavin );
					assertEquals(
							0l,
							session.createQuery( "select count(*) from SessionAttribute" ).uniqueResult()
					);

				}
		);

		scope.inTransaction(
				session -> {
					Group g = session.get( Group.class, "developers" );
					assertEquals( g.getUsers().size(), 1 );
					User t = (User) g.getUsers().get( "turin" );
					Map smap = t.getSession();
					assertEquals( smap.size(), 0 );
					assertEquals(
							1l,
							session.createQuery( "select count(*) from User" ).uniqueResult()
					);
					session.remove( g );
					session.remove( t );
					assertEquals(
							0l,
							session.createQuery( "select count(*) from User" ).uniqueResult()
					);

				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
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
					assertEquals( results.size(), 2 );
					gavin = (User) results.get( 0 );
					assertEquals( "gavin", gavin.getName() );
					assertEquals( 2, gavin.getSession().size() );
					session.createMutationQuery( "delete SessionAttribute" ).executeUpdate();
					session.createMutationQuery( "delete User" ).executeUpdate();
				}
		);
	}

}
