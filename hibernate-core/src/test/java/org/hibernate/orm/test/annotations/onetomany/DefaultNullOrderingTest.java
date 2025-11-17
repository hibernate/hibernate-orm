/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Root;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Lukasz Antoniak
 */
@JiraKey(value = "HHH-465")
@RequiresDialect(value = H2Dialect.class,
		comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression.")
@DomainModel(
		annotatedClasses = {
				Monkey.class,
				Troop.class,
				Soldier.class
		}
)
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.DEFAULT_NULL_ORDERING,
				provider = DefaultNullOrderingTest.NullOrderingProvider.class)
)
public class DefaultNullOrderingTest {

	public static class NullOrderingProvider implements SettingProvider.Provider<Nulls> {

		@Override
		public Nulls getSetting() {
			return Nulls.LAST;
		}
	}

	@Test
	public void testHqlDefaultNullOrdering(SessionFactoryScope scope) {
		scope.inSession( session -> {
// Populating database with test data.
			try {
				session.getTransaction().begin();
				Monkey monkey1 = new Monkey();
				monkey1.setName( null );
				Monkey monkey2 = new Monkey();
				monkey2.setName( "Warsaw ZOO" );
				session.persist( monkey1 );
				session.persist( monkey2 );
				session.getTransaction().commit();

				session.getTransaction().begin();
				List<Monkey> orderedResults = session.createQuery( "from Monkey m order by m.name", Monkey.class )
						.list(); // Should order by NULLS LAST.
				assertThat( orderedResults ).isEqualTo( Arrays.asList( monkey2, monkey1 ) );
				session.getTransaction().commit();

				session.clear();

				// Cleanup data.
				session.getTransaction().begin();
				session.remove( monkey1 );
				session.remove( monkey2 );
				session.getTransaction().commit();
			}
			finally {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
			}
		} );
	}

	@Test
	public void testAnnotationsDefaultNullOrdering(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						// Populating database with test data.
						session.getTransaction().begin();
						Troop troop = new Troop();
						troop.setName( "Alpha 1" );
						Soldier ranger = new Soldier();
						ranger.setName( "Ranger 1" );
						troop.addSoldier( ranger );
						Soldier sniper = new Soldier();
						sniper.setName( null );
						troop.addSoldier( sniper );
						session.persist( troop );
						session.getTransaction().commit();

						session.clear();

						session.getTransaction().begin();
						troop = session.find( Troop.class, troop.getId() );
						Iterator<Soldier> iterator = troop.getSoldiers().iterator(); // Should order by NULLS LAST.
						assertThat( iterator.next().getName() ).isEqualTo( ranger.getName() );
						assertThat( iterator.next().getName() ).isNull();
						session.getTransaction().commit();

						session.clear();

						// Cleanup data.
						session.getTransaction().begin();
						session.remove( troop );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testCriteriaDefaultNullOrdering(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						// Populating database with test data.
						session.getTransaction().begin();
						Monkey monkey1 = new Monkey();
						monkey1.setName( null );
						Monkey monkey2 = new Monkey();
						monkey2.setName( "Berlin ZOO" );
						session.persist( monkey1 );
						session.persist( monkey2 );
						session.getTransaction().commit();

						session.getTransaction().begin();
						CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
						CriteriaQuery<Monkey> criteria = criteriaBuilder.createQuery( Monkey.class );
						Root<Monkey> root = criteria.from( Monkey.class );
						criteria.orderBy( criteriaBuilder.asc( root.get( "name" ) ) );

						assertThat( session.createQuery( criteria ).list() )
								.isEqualTo( Arrays.asList( monkey2, monkey1 ) );

//						Criteria criteria = session.createCriteria( Monkey.class );
//						criteria.addOrder( org.hibernate.criterion.Order.asc( "name" ) ); // Should order by NULLS LAST.
//						Assert.assertEquals( Arrays.asList( monkey2, monkey1 ), criteria.list() );
						session.getTransaction().commit();

						session.clear();

						// Cleanup data.
						session.getTransaction().begin();
						session.remove( monkey1 );
						session.remove( monkey2 );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}
}
