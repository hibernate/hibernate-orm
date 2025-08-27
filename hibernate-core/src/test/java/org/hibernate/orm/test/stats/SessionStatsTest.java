/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stats;

import java.util.HashSet;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/stats/Continent2.hbm.xml"
)
@SessionFactory
public class SessionStatsTest {

	@Test
	public void testSessionStatistics(SessionFactoryScope scope) {
		boolean isStatsEnabled = scope.fromSession(
				session -> {
					try {
						Transaction tx = session.beginTransaction();
						Statistics stats = scope.getSessionFactory().getStatistics();
						stats.clear();
						boolean isStats = stats.isStatisticsEnabled();
						stats.setStatisticsEnabled( true );
						Continent europe = fillDb( session );
						tx.commit();
						session.clear();
						tx = session.beginTransaction();
						SessionStatistics sessionStats = session.getStatistics();
						assertEquals( 0, sessionStats.getEntityKeys().size() );
						assertEquals( 0, sessionStats.getEntityCount() );
						assertEquals( 0, sessionStats.getCollectionKeys().size() );
						assertEquals( 0, sessionStats.getCollectionCount() );
						europe = (Continent) session.get( Continent.class, europe.getId() );
						Hibernate.initialize( europe.getCountries() );
						Hibernate.initialize( europe.getCountries().iterator().next() );
						assertEquals( 2, sessionStats.getEntityKeys().size() );
						assertEquals( 2, sessionStats.getEntityCount() );
						assertEquals( 1, sessionStats.getCollectionKeys().size() );
						assertEquals( 1, sessionStats.getCollectionCount() );
						tx.commit();
						return isStats;
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		scope.getSessionFactory().getStatistics().setStatisticsEnabled( isStatsEnabled );

	}

	private Continent fillDb(Session s) {
		Continent europe = new Continent();
		europe.setName( "Europe" );
		Country france = new Country();
		france.setName( "France" );
		europe.setCountries( new HashSet() );
		europe.getCountries().add( france );
		s.persist( france );
		s.persist( europe );
		return europe;
	}

}
