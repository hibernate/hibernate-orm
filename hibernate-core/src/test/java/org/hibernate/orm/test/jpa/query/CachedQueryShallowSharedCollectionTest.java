/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.query;

import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = {
		CachedQueryShallowSharedCollectionTest.Account.class,
		CachedQueryShallowSharedCollectionTest.DomainAccount.class
}, properties = {
		@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.QUERY_CACHE_LAYOUT, value = "auto" )
}, generateStatistics = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18085" )
public class CachedQueryShallowSharedCollectionTest {
	private static final String ACCOUNT_BY_NAME = "from Account where name = :name";

	@Test
	public void testQueryInSameTransaction(EntityManagerFactoryScope scope) {
		final Statistics stats = getStatistics( scope );
		stats.clear();

		scope.inTransaction( entityManager -> {
			// ensure the account is in 2LC and that the query cache is populated
			executeQueryByName( entityManager, "test_account" );
		} );

		assertThat( stats.getQueryCacheHitCount() ).isEqualTo( 0 );
		assertThat( stats.getQueryCacheMissCount() ).isEqualTo( 1 );
		assertThat( stats.getQueryCachePutCount() ).isEqualTo( 1 );

		stats.clear();

		scope.inTransaction( entityManager -> {
			// execute the query multiple times, ensure the returned account is always the same
			Account old = null;
			for ( int i = 1; i <= 2; i++ ) {
				final Account account = executeQueryByName( entityManager, "test_account" );
				assertThat( account.getDomainAccounts() ).hasSize( 2 );

				assertThat( stats.getQueryCacheHitCount() ).isEqualTo( i );
				assertThat( stats.getQueryCacheMissCount() ).isEqualTo( 0 );
				assertThat( stats.getQueryCachePutCount() ).isEqualTo( 0 );

				if ( old != null ) {
					assertThat( account ).isSameAs( old );
				}
				old = account;
			}
		} );
	}

	private static Account executeQueryByName(
			EntityManager entityManager,
			@SuppressWarnings( "SameParameterValue" ) String name) {
		return entityManager.createQuery( ACCOUNT_BY_NAME, Account.class )
				.setParameter( "name", name )
				.setHint( HINT_CACHEABLE, true )
				.getSingleResult();
	}

	private static Statistics getStatistics(EntityManagerFactoryScope scope) {
		return ( (SessionFactoryImplementor) scope.getEntityManagerFactory() ).getStatistics();
	}

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Account account = new Account( 1L, "test_account" );
			entityManager.persist( account );
			entityManager.persist( new DomainAccount( 1L, account ) );
			entityManager.persist( new DomainAccount( 2L, account ) );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from DomainAccount" ).executeUpdate();
			entityManager.createQuery( "delete from Account" ).executeUpdate();
		} );
	}

	@Entity( name = "Account" )
	@Table( name = "account_table" )
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	static class Account {
		@Id
		private Long id;

		private String name;

		@OneToMany( fetch = FetchType.LAZY, mappedBy = "account" )
		@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
		private Set<DomainAccount> domainAccounts;

		public Account() {
		}

		public Account(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Set<DomainAccount> getDomainAccounts() {
			return domainAccounts;
		}
	}

	@Entity( name = "DomainAccount" )
	@Table( name = "domains_account_table" )
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	static class DomainAccount {
		@Id
		public Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		public Account account;

		public DomainAccount() {
		}

		public DomainAccount(Long id, Account account) {
			this.id = id;
			this.account = account;
		}

		public Account getAccount() {
			return account;
		}
	}
}
