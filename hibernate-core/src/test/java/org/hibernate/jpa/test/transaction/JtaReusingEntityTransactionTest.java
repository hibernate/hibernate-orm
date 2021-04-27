/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.orm.test.jpa.transaction.SynchronizationTypeTest;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@Jpa(
		annotatedClasses = { JtaReusingEntityTransactionTest.TestEntity.class },
		integrationSettings = {
				@Setting(name = org.hibernate.jpa.AvailableSettings.TRANSACTION_TYPE, value = "JTA"),
				@Setting(name = org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.testing.jta.JtaAwareConnectionProviderImpl"),

		},
		nonStringValueSettingProviders = { SynchronizationTypeTest.JtaPlatformNonStringValueSettingProvider.class }
)
public class JtaReusingEntityTransactionTest {

	@Test
	public void entityTransactionShouldBeReusableTest(EntityManagerFactoryScope scope) {
		EntityManager em = createEntityManager( scope );
		EntityTransaction transaction = null;
		try {
			transaction = em.getTransaction();
			em.persist( new TestEntity() );
			transaction.begin();
			transaction.commit();
			transaction.begin();
			em.persist( new TestEntity() );
			transaction.commit();
		}
		finally {
			if ( transaction != null && transaction.isActive() ) {
				transaction.rollback();
			}
			em.close();
		}
		em = createEntityManager( scope );
		try {
			transaction = em.getTransaction();
			transaction.begin();
			List<TestEntity> results = em.createQuery( "from TestEntity" ).getResultList();
			assertThat( results.size(), is( 2 ) );
			transaction.commit();
		}
		finally {
			if ( transaction != null && transaction.isActive() ) {
				transaction.rollback();
			}
			em.close();
		}
	}

	private EntityManager createEntityManager(EntityManagerFactoryScope scope) {
		return scope.getEntityManagerFactory().createEntityManager();
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;
	}
}
