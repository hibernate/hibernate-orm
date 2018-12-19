/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.util.Calendar;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
public class SimpleSelectionTest extends SessionFactoryBasedFunctionalTest {
	public static final String STATIC_FIELD = "STATIC_FIELD";

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				SimpleEntity.class,
		};
	}

	@Test
	public void testFullyQualifiedEntityNameRoot() {
		sessionFactoryScope().inTransaction(
				session -> {
					List<SimpleEntity> results = session.createQuery(
							"select s.someString from org.hibernate.orm.test.support.domains.gambit.SimpleEntity s",
							SimpleEntity.class ).list();
					assertThat( results.size(), is( 2 ) );
				}
		);

	}

	@Test
	public void testFullyQualifiedFieldName() {
		sessionFactoryScope().inTransaction(
				session -> {
					List<SimpleEntity> results = session.createQuery(
							"select s.someString from SimpleEntity s where s.someString != org.hibernate.orm.test.query.hql.SimpleSelectionTest.STATIC_FIELD",
							SimpleEntity.class ).list();
					assertThat( results.size(), is( 2 ) );
				}
		);

	}

	@Test
	public void testSubstrInsideConcat() {
		sessionFactoryScope().inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select s.someString from SimpleEntity s where s.id = :id" )
							.setParameter( "id", 1 )
							.list();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ), is( "a" ) );
				} );
	}

	@Test
	public void testLengthFunctionPredicate() {
		sessionFactoryScope().inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select s.someString from SimpleEntity s where length(s.someString) > :p1 ORDER BY s.someString" )
							.setParameter( "p1", 0L )
							.list();
					assertThat( results.size(), is( 2 ) );
					assertThat( results.get( 0 ), is( "a" ) );
				} );
	}

	@Test
	public void testLengthFunctionSelection() {
		sessionFactoryScope().inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select length(s.someString) from SimpleEntity s" )
							.list();
					assertThat( results.size(), is( 2 ) );
					results.forEach( value -> assertThat( value, is( 1L ) ) );
				} );
	}

	@Test
	public void testSubstringFunctionPredicate() {
		sessionFactoryScope().inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select s.someString from SimpleEntity s where substring(s.someString, 0, 1) = :p1" )
							.setParameter( "p1", "a" )
							.list();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ), is( "a" ) );
				} );
	}

	@Test
	public void testSubstringFunctionSelection() {
		sessionFactoryScope().inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select substring(s.someString, 0, 1) from SimpleEntity s ORDER BY s.someString" )
							.list();
					assertThat( results.size(), is( 2 ) );
					assertThat( results.get( 0 ), is( "a" ) );
				} );
	}

	@Test
	public void testSelectAnIntegerConstant() {
		sessionFactoryScope().inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select 1 from SimpleEntity s where s.id = :id" )
							.setParameter( "id", 1 )
							.list();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ), is( 1 ) );
				} );
	}

	@Test
	public void testSelectACharConstant() {
		sessionFactoryScope().inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select 'a' from SimpleEntity s where s.id = :id" )
							.setParameter( "id", 1 )
							.list();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ), is( 'a' ) );
				} );
	}

	@BeforeEach
	public void setUp() {
		sessionFactoryScope().inTransaction(
				session -> {
					SimpleEntity entity = new SimpleEntity(
							1,
							Calendar.getInstance().getTime(),
							null,
							Integer.MAX_VALUE,
							Long.MAX_VALUE,
							"a"
					);
					session.save( entity );

					SimpleEntity second_entity = new SimpleEntity(
							2,
							Calendar.getInstance().getTime(),
							null,
							Integer.MIN_VALUE,
							Long.MAX_VALUE,
							"b"
					);
					session.save( second_entity );

				} );
	}

	@AfterEach
	public void tearDown() {
		sessionFactoryScope().inTransaction(
				session -> {
					session.createQuery( "from SimpleEntity e" )
							.list()
							.forEach( simpleEntity -> session.delete( simpleEntity ) );
				} );
	}
}
