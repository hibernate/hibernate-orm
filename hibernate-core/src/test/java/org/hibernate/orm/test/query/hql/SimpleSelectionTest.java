/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.util.Calendar;
import java.util.List;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;

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
		inTransaction(
				session -> {
					List<SimpleEntity> results = session.createQuery(
							"select s.someString from org.hibernate.testing.orm.domain.gambit.SimpleEntity s",
							SimpleEntity.class ).list();
					assertThat( results.size(), is( 2 ) );
				}
		);

	}

	@Test
	public void testFullyQualifiedFieldName() {
		inTransaction(
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
		inTransaction(
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
		inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select s.someString from SimpleEntity s where length(s.someString) > :p1 ORDER BY s.someString" )
							.setParameter( "p1", 0 )
							.list();
					assertThat( results.size(), is( 2 ) );
					assertThat( results.get( 0 ), is( "a" ) );
				} );
	}

	@Test
	public void testLengthFunctionSelection() {
		inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select length(s.someString) from SimpleEntity s" )
							.list();
					assertThat( results.size(), is( 2 ) );
					results.forEach( value -> assertThat( value, is( 1 ) ) );
				} );
	}

	@Test
	public void testSubstringFunctionPredicate() {
		inTransaction(
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
		inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select substring(s.someString, 0, 1) from SimpleEntity s ORDER BY s.someString" )
							.list();
					assertThat( results.size(), is( 2 ) );
					assertThat( results.get( 0 ), is( "a" ) );
				} );
	}

	@Test
	public void testLowerFunctionSelection() {
		inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select lower(s.someString) from SimpleEntity s ORDER BY lower(s.someString)" )
							.list();
					assertThat( results.size(), is( 2 ) );
					assertThat( results.get( 0 ), is( "a" ) );
				} );
	}

	@Test
	public void testUpperFunctionSelection() {
		inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select upper(s.someString) from SimpleEntity s ORDER BY upper(s.someString)" )
							.list();
					assertThat( results.size(), is( 2 ) );
					assertThat( results.get( 0 ), is( "A" ) );
				} );
	}

	@Test
	public void testLocateFunctionSelection() {
		inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select locate('a', s.someString, 0) from SimpleEntity s ORDER BY s.someString" )
							.list();
					assertThat( results.size(), is( 2 ) );
					assertThat( results.get( 0 ), is( 1 ) );
				} );
	}

	@Test
	public void testSelectAnIntegerConstant() {
		inTransaction(
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
		inTransaction(
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
		inTransaction(
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
		inTransaction(
				session -> {
					session.createQuery( "from SimpleEntity e" )
							.list()
							.forEach( simpleEntity -> session.delete( simpleEntity ) );
				} );
	}
}
