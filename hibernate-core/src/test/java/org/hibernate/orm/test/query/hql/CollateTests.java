/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Gavin King
 */
@ServiceRegistry
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@SessionFactory
public class CollateTests {

	@BeforeAll
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityOfBasics entity = new EntityOfBasics();
					entity.setTheString("1234589");
					entity.setId(123);
					entity.setTheDate( new Date( 74, 2, 25 ) );
					entity.setTheTime( new Time( 20, 10, 8 ) );
					entity.setTheTimestamp( new Timestamp( 121, 4, 27, 13, 22, 50, 123456789 ) );
					em.persist(entity);
				}
		);
	}

	@Test @RequiresDialect(PostgreSQLDialect.class)
	public void testCollatePostgreSQL(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("from EntityOfBasics e where e.theString is not null order by collate(e.theString as `ucs_basic`)").getResultList();
					assertThat( session.createQuery("select collate('bar' as `ucs_basic`) < 'foo'").getSingleResult(), is(true) );
				}
		);
		scope.inTransaction(
				session -> {
					session.createQuery("from EntityOfBasics e where e.theString is not null order by collate(e.theString as ucs_basic)").getResultList();
					assertThat( session.createQuery("select collate('bar' as ucs_basic) < 'foo'").getSingleResult(), is(true) );
				}
		);
	}

	@Test @RequiresDialect(MySQLDialect.class)
	public void testCollateMySQL(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("from EntityOfBasics e order by collate(e.theString as utf8mb4_bin)").getResultList();
					assertThat( session.createQuery("select collate('bar' as utf8mb4_bin) < 'foo'").getSingleResult(), is(true) );
				}
		);
	}

	@Test @RequiresDialect(HSQLDialect.class)
	public void testCollateHSQL(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("from EntityOfBasics e order by collate(e.theString as SQL_TEXT_UCC)").getResultList();
					session.createQuery("from EntityOfBasics e order by collate(e.theString as English)").getResultList();
					assertThat( session.createQuery("select collate('bar' as English) < 'foo'").getSingleResult(), is(true) );
				}
		);
	}

}
