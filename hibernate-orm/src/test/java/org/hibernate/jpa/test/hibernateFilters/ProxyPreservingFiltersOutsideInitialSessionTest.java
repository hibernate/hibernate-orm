/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.hibernateFilters;

import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ProxyPreservingFiltersOutsideInitialSessionTest
		extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( ProxyPreservingFiltersOutsideInitialSessionTest.class );

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
			AccountGroup.class,
			Account.class
		};
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		settings.put( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		return settings;
	}

	@Test
	@FailureExpected( jiraKey = "HHH-11076", message = "Fix rejected, we need another approach to fix this issue!" )
	public void testPreserveFilters() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			AccountGroup accountGroup = new AccountGroup();
			accountGroup.setId( 1L );
			entityManager.persist( accountGroup );

			Account account = new Account();
			account.setName( "A1" );
			account.setRegionCode( "Europe" );
			entityManager.persist( account );
			accountGroup.getAccounts().add( account );

			account = new Account();
			account.setName( "A2" );
			account.setRegionCode( "Europe" );
			entityManager.persist( account );
			accountGroup.getAccounts().add( account );

			account = new Account();
			account.setName( "A3" );
			account.setRegionCode( "US" );
			entityManager.persist( account );
			accountGroup.getAccounts().add( account );
		} );

		AccountGroup group = doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "US" );
			return entityManager.find( AccountGroup.class, 1L );
		} );

		assertEquals(1, group.getAccounts().size());
	}

	@Test
	public void testChangeFilterBeforeInitializeInSameSession() {

		doInJPA(
				this::entityManagerFactory, entityManager -> {
					AccountGroup accountGroup = new AccountGroup();
					accountGroup.setId( 1L );
					entityManager.persist( accountGroup );

					Account account = new Account();
					account.setName( "A1" );
					account.setRegionCode( "Europe" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );

					account = new Account();
					account.setName( "A2" );
					account.setRegionCode( "Europe" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );

					account = new Account();
					account.setName( "A3" );
					account.setRegionCode( "US" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );
				}
		);

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "US" );
			AccountGroup accountGroup = entityManager.find( AccountGroup.class, 1L );
			// Change the filter
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "Europe" );
			Hibernate.initialize( accountGroup.getAccounts() );
			// will contain accounts with regionCode "Europe"
			assertEquals( 2, accountGroup.getAccounts().size() );
			return accountGroup;
		} );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-11076", message = "Fix rejected, we need another approach to fix this issue!" )
	public void testChangeFilterBeforeInitializeInTempSession() {

		doInJPA(
				this::entityManagerFactory, entityManager -> {
					AccountGroup accountGroup = new AccountGroup();
					accountGroup.setId( 1L );
					entityManager.persist( accountGroup );

					Account account = new Account();
					account.setName( "A1" );
					account.setRegionCode( "Europe" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );

					account = new Account();
					account.setName( "A2" );
					account.setRegionCode( "Europe" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );

					account = new Account();
					account.setName( "A3" );
					account.setRegionCode( "US" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );
				}
		);

		AccountGroup group = doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "US" );
			AccountGroup accountGroup = entityManager.find( AccountGroup.class, 1L );
			// Change the filter.
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "Europe" );
			return accountGroup;
		} );

		log.info( "Initialize accounts collection" );
		// What should group.getAccounts() contain? Should it be accounts with regionCode "Europe"
		// because that was the most recent filter used in the session?
		Hibernate.initialize( group.getAccounts() );
		// The following will fail because the collection will only contain accounts with regionCode "US"
		assertEquals(2, group.getAccounts().size());
	}

	@Test
	public void testMergeNoFilterThenInitializeTempSession() {

		doInJPA(
				this::entityManagerFactory, entityManager -> {
					AccountGroup accountGroup = new AccountGroup();
					accountGroup.setId( 1L );
					entityManager.persist( accountGroup );

					Account account = new Account();
					account.setName( "A1" );
					account.setRegionCode( "Europe" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );

					account = new Account();
					account.setName( "A2" );
					account.setRegionCode( "Europe" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );

					account = new Account();
					account.setName( "A3" );
					account.setRegionCode( "US" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );
				}
		);

		final AccountGroup group = doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "US" );
			return entityManager.find( AccountGroup.class, 1L );
		} );

		final AccountGroup mergedGroup = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.merge( group );
		} );

		// group.getAccounts() will be unfiltered because merge cleared AbstractCollectionPersister#enabledFilters
		Hibernate.initialize( mergedGroup.getAccounts() );
		assertEquals(3, mergedGroup.getAccounts().size());
	}

	@Test
	public void testSaveOrUpdateNoFilterThenInitializeTempSession() {

		doInJPA(
				this::entityManagerFactory, entityManager -> {
					AccountGroup accountGroup = new AccountGroup();
					accountGroup.setId( 1L );
					entityManager.persist( accountGroup );

					Account account = new Account();
					account.setName( "A1" );
					account.setRegionCode( "Europe" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );

					account = new Account();
					account.setName( "A2" );
					account.setRegionCode( "Europe" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );

					account = new Account();
					account.setName( "A3" );
					account.setRegionCode( "US" );
					entityManager.persist( account );
					accountGroup.getAccounts().add( account );
				}
		);

		final AccountGroup group = doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "US" );
			return entityManager.find( AccountGroup.class, 1L );
		} );

		final AccountGroup savedGroup = doInJPA( this::entityManagerFactory, entityManager -> {
			// saveOrUpdate adds the PersistenceCollection to the session "as is"
			return (AccountGroup) entityManager.unwrap( Session.class ).merge( group );
		} );

		Hibernate.initialize( savedGroup.getAccounts() );
		// group.getAccounts() should not be filtered.
		// the following fails because AbstractCollectionPersister#enabledFilters is still intact.
		assertEquals(3, savedGroup.getAccounts().size());
	}
}
