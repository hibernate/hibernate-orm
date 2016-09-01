/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.hibernateFilters;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ProxyPreservingFiltersOutsideInitialSessionTest
		extends BaseEntityManagerFunctionalTestCase {
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
}
