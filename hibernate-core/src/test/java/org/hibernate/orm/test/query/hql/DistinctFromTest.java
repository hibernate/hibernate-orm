/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@ServiceRegistry
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
public class DistinctFromTest {

	@BeforeAll
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityOfBasics entity1 = new EntityOfBasics();
					entity1.setId( 123 );
					em.persist( entity1 );
					EntityOfBasics entity = new EntityOfBasics();
					entity.setId( 456 );
					entity.setTheString( "abc" );
					em.persist( entity );
				}
		);
	}

	@Test
	public void testDistinctFromNullParam(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Assertions.assertEquals(
							456,
							session.createQuery(
									"select e.id from EntityOfBasics e where e.theString is distinct from :param",
									Integer.class
							)
									.setParameter( "param", null )
									.list()
									.get( 0 )
					);
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "No idea what Sybase does here, maybe it's a bug?")
	public void testDistinctFromStringParam(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Assertions.assertEquals(
							123,
							session.createQuery(
									"select e.id from EntityOfBasics e where e.theString is distinct from :param",
									Integer.class
							)
									.setParameter( "param", "abc" )
									.list()
									.get( 0 )
					);
				}
		);
	}

	@Test
	public void testNotDistinctFromNullParam(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Assertions.assertEquals(
							123,
							session.createQuery(
									"select e.id from EntityOfBasics e where e.theString is not distinct from :param",
									Integer.class
							)
									.setParameter( "param", null )
									.list()
									.get( 0 )
					);
				}
		);
	}

	@Test
	public void testNotDistinctFromStringParam(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Assertions.assertEquals(
							456,
							session.createQuery(
									"select e.id from EntityOfBasics e where e.theString is not distinct from :param",
									Integer.class
							)
									.setParameter( "param", "abc" )
									.list()
									.get( 0 )
					);
				}
		);
	}

	@Test void testNulls(SessionFactoryScope scope) {
		scope.inSession(session -> {
			assertEquals(1, session.createSelectionQuery("select 1 where 1 is distinct from 0").getResultList().size());
			assertEquals(0, session.createSelectionQuery("select 1 where 1 is distinct from 1").getResultList().size());
			assertEquals(1, session.createSelectionQuery("select 1 where 1 is distinct from null").getResultList().size());
			assertEquals(1, session.createSelectionQuery("select 1 where null is distinct from 1").getResultList().size());
			assertEquals(1, session.createSelectionQuery("select 1 where null is distinct from 0").getResultList().size());
			assertEquals(0, session.createSelectionQuery("select 1 where null is distinct from null").getResultList().size());

			assertEquals(0, session.createSelectionQuery("select 1 where 1 is not distinct from 0").getResultList().size());
			assertEquals(1, session.createSelectionQuery("select 1 where 1 is not distinct from 1").getResultList().size());
			assertEquals(0, session.createSelectionQuery("select 1 where 1 is not distinct from null").getResultList().size());
			assertEquals(0, session.createSelectionQuery("select 1 where null is not distinct from 1").getResultList().size());
			assertEquals(0, session.createSelectionQuery("select 1 where null is not distinct from 0").getResultList().size());
			assertEquals(1, session.createSelectionQuery("select 1 where null is not distinct from null").getResultList().size());
		});
	}
}
