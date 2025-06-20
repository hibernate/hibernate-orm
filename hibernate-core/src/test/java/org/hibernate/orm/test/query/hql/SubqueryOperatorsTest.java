/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.Calendar;
import java.util.List;

import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Gavin King
 */
@DomainModel( annotatedClasses = SimpleEntity.class )
@SessionFactory
public class SubqueryOperatorsTest {

	@Test
	public void testEvery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<SimpleEntity> results = session.createQuery(
							"from SimpleEntity o where o.someString >= every (select someString from SimpleEntity)", SimpleEntity.class )
							.list();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Test
	public void testAny(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<SimpleEntity> results = session.createQuery(
							"from SimpleEntity o where o.someString >= any (select someString from SimpleEntity)", SimpleEntity.class )
							.list();
					assertThat( results.size(), is( 2 ) );
				}
		);
	}

	@Test @SuppressWarnings("deprecation")
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase ASE does not allow a subquery in the order by clause, but we could move it to the select clause and refer to it by position", matchSubTypes = true)
	public void testSubqueryInVariousClauses(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List res0 = session.createQuery(
							"select (select cast(1 as Integer)) as one, (select cast('foo' as String)) as foo order by one, foo, (select 2)" )
							.list();
					assertThat( res0.size(), is( 1 ) );
					List res1 = session.createQuery(
							"select (select cast(1 as Integer)) as one, (select cast('foo' as String)) as foo from SimpleEntity o order by one, foo, (select 2)" )
							.list();
					assertThat( res1.size(), is( 2 ) );
					List res2 = session.createQuery(
							"select (select x.id from SimpleEntity x where x.id = o.id) as xid from SimpleEntity o order by xid" )
							.list();
					assertThat( res2.size(), is( 2 ) );
					List res3 = session.createQuery(
							"from SimpleEntity o where o.someString = (select cast('aaa' as String)) and o.id >= (select cast(0 as Integer))" )
							.list();
					assertThat( res3.size(), is( 1 ) );
					List res4 = session.createQuery(
							"from SimpleEntity o where o.id = (select y.id from SimpleEntity y where y.id = o.id)" )
							.list();
					assertThat( res4.size(), is( 2 ) );
				}
		);
	}

	@Test
	public void testExists(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<SimpleEntity> results = session.createQuery(
							"from SimpleEntity o where exists (select someString from SimpleEntity where someString>o.someString)", SimpleEntity.class )
							.list();
					assertThat( results.size(), is( 1 ) );
					results = session.createQuery(
							"from SimpleEntity o where not exists (select someString from SimpleEntity where someString>o.someString)", SimpleEntity.class )
							.list();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity entity = new SimpleEntity(
							1,
							Calendar.getInstance().getTime(),
							null,
							Integer.MAX_VALUE,
							Long.MAX_VALUE,
							"aaa"
					);
					session.persist( entity );

					SimpleEntity second_entity = new SimpleEntity(
							2,
							Calendar.getInstance().getTime(),
							null,
							Integer.MIN_VALUE,
							Long.MAX_VALUE,
							"zzz"
					);
					session.persist( second_entity );

				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
