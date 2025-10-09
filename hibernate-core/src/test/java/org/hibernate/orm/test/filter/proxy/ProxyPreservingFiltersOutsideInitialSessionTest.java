/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.proxy;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.TransactionSettings.ENABLE_LAZY_LOAD_NO_TRANS;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name = ENABLE_LAZY_LOAD_NO_TRANS, value = "true"))
@DomainModel(annotatedClasses = {
		AccountGroup.class,
		Account.class
})
@SessionFactory
public class ProxyPreservingFiltersOutsideInitialSessionTest {
	private final Logger log =  Logger.getLogger( ProxyPreservingFiltersOutsideInitialSessionTest.class );

	@Test
	@FailureExpected( jiraKey = "HHH-11076",
			reason = "Fix rejected, we need another approach to fix this issue!" )
	public void testPreserveFilters(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
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

		AccountGroup group = factoryScope.fromTransaction( entityManager -> {
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "US" );
			return entityManager.find( AccountGroup.class, 1L );
		} );

		Assertions.assertEquals( 1, group.getAccounts().size() );
	}

	@Test
	public void testChangeFilterBeforeInitializeInSameSession(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
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

		factoryScope.inTransaction( entityManager -> {
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "US" );
			AccountGroup accountGroup = entityManager.find( AccountGroup.class, 1L );
			// Change the filter
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "Europe" );
			Hibernate.initialize( accountGroup.getAccounts() );
			// will contain accounts with regionCode "Europe"
			Assertions.assertEquals( 2, accountGroup.getAccounts().size() );
		} );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-11076",
			reason = "Fix rejected, we need another approach to fix this issue!" )
	public void testChangeFilterBeforeInitializeInTempSession(SessionFactoryScope factoryScope) {
		factoryScope.inSession( entityManager -> {
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

		AccountGroup group = factoryScope.fromTransaction( entityManager -> {
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
		Assertions.assertEquals( 2, group.getAccounts().size() );
	}

	@Test
	public void testMergeNoFilterThenInitializeTempSession(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
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

		final AccountGroup group = factoryScope.fromTransaction( entityManager -> {
			entityManager.unwrap( Session.class ).enableFilter( "byRegion" ).setParameter( "region", "US" );
			return entityManager.find( AccountGroup.class, 1L );
		} );

		final AccountGroup mergedGroup = factoryScope.fromTransaction( entityManager -> {
			return entityManager.merge( group );
		} );

		// group.getAccounts() will be unfiltered because merge cleared AbstractCollectionPersister#enabledFilters
		Hibernate.initialize( mergedGroup.getAccounts() );
		Assertions.assertEquals( 3, mergedGroup.getAccounts().size() );
	}
}
