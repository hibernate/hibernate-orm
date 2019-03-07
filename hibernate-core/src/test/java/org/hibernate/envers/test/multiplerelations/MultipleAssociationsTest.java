/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.multiplerelations;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.multiplerelations.Address;
import org.hibernate.envers.test.support.domains.multiplerelations.Person;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7073")
public class MultipleAssociationsTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private long lukaszId = 0;
	private long kingaId = 0;
	private long warsawId = 0;
	private long cracowId = 0;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Address.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		// todo (6.0) - This should be fixed in ORM and this requirement of maximum-fetch depth removed.
		//		This is currently a workaround to get the test to pass.
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, 10 );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					Person lukasz = new Person( "Lukasz" );
					Person kinga = new Person( "Kinga" );
					Address warsaw = new Address( "Warsaw" );
					warsaw.getTenants().add( lukasz );
					warsaw.setLandlord( lukasz );
					warsaw.getTenants().add( kinga );
					lukasz.getAddresses().add( warsaw );
					lukasz.getOwnedAddresses().add( warsaw );
					kinga.getAddresses().add( warsaw );

					entityManager.persist( lukasz );
					entityManager.persist( kinga );
					entityManager.persist( warsaw );
					lukaszId = lukasz.getId();
					kingaId = kinga.getId();
					warsawId = warsaw.getId();
				},

				// Revision 2
				entityManager -> {
					Person kinga = entityManager.find( Person.class, kingaId );
					Address cracow = new Address( "Cracow" );
					kinga.getAddresses().add( cracow );
					cracow.getTenants().add( kinga );
					cracow.setLandlord( kinga );

					entityManager.persist( cracow );
					cracowId = cracow.getId();
				},

				// Revision 3
				entityManager -> {
					Address cracow = entityManager.find( Address.class, cracowId );
					cracow.setCity( "Krakow" );

					entityManager.merge( cracow );
				},

				// Revision 4
				entityManager -> {
					Person lukasz = entityManager.find( Person.class, lukaszId );
					lukasz.setName( "Lucas" );

					entityManager.merge( lukasz );
				},

				// Revision 5
				entityManager -> {
					Address warsaw = entityManager.find( Address.class, warsawId );
					Person lukasz = entityManager.find( Person.class, lukaszId );
					Person kinga = entityManager.find( Person.class, kingaId );
					warsaw.setLandlord( kinga );
					kinga.getOwnedAddresses().add( warsaw );
					lukasz.getOwnedAddresses().remove( warsaw );

					entityManager.merge( warsaw );
					entityManager.merge( lukasz );
					entityManager.merge( kinga );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( Person.class, lukaszId ), contains( 1, 4, 5 ) );
		assertThat( getAuditReader().getRevisions( Person.class, kingaId ), contains( 1, 2, 5 ) );
		assertThat( getAuditReader().getRevisions( Address.class, warsawId ), contains( 1, 5 ) );
		assertThat( getAuditReader().getRevisions( Address.class, cracowId ), contains( 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfLukasz() {
		Person lukasz = new Person( "Lukasz", lukaszId );
		Address warsaw = new Address( "Warsaw", warsawId );
		lukasz.getAddresses().add( warsaw );
		lukasz.getOwnedAddresses().add( warsaw );

		Person ver1 = getAuditReader().find( Person.class, lukaszId, 1 );
		assertThat( ver1, equalTo( lukasz ) );
		assertThat( ver1.getAddresses(), equalTo( lukasz.getAddresses() ) );
		assertThat( ver1.getOwnedAddresses(), equalTo( lukasz.getOwnedAddresses() ) );

		lukasz.setName( "Lucas" );

		Person ver4 = getAuditReader().find( Person.class, lukaszId, 4 );
		assertThat( ver4, equalTo( lukasz ) );

		lukasz.getOwnedAddresses().remove( warsaw );

		Person ver5 = getAuditReader().find( Person.class, lukaszId, 5 );
		assertThat( ver5.getOwnedAddresses(), equalTo( lukasz.getOwnedAddresses() ) );
	}

	@DynamicTest
	public void testHistoryOfKinga() {
		Person kinga = new Person( "Kinga", kingaId );
		Address warsaw = new Address( "Warsaw", warsawId );
		kinga.getAddresses().add( warsaw );

		Person ver1 = getAuditReader().find( Person.class, kingaId, 1 );
		assertThat( ver1, equalTo( kinga ) );
		assertThat( ver1.getAddresses(), equalTo( kinga.getAddresses() ) );
		assertThat( ver1.getOwnedAddresses(), equalTo( kinga.getOwnedAddresses() ) );

		Address cracow = new Address( "Cracow", cracowId );
		kinga.getOwnedAddresses().add( cracow );
		kinga.getAddresses().add( cracow );

		Person ver2 = getAuditReader().find( Person.class, kingaId, 2 );
		assertThat( ver2, equalTo( kinga ) );
		assertThat( ver2.getAddresses(), equalTo( kinga.getAddresses() ) );
		assertThat( ver2.getOwnedAddresses(), equalTo( kinga.getOwnedAddresses() ) );

		kinga.getOwnedAddresses().add( warsaw );
		cracow.setCity( "Krakow" );

		Person ver5 = getAuditReader().find( Person.class, kingaId, 5 );
		assertThat( ver5.getAddresses(), equalTo( kinga.getAddresses() ) );
		assertThat( ver5.getOwnedAddresses(), equalTo( kinga.getOwnedAddresses() ) );
	}

	@DynamicTest
	public void testHistoryOfCracow() {
		Address cracow = new Address( "Cracow", cracowId );
		Person kinga = new Person( "Kinga", kingaId );
		cracow.getTenants().add( kinga );
		cracow.setLandlord( kinga );

		Address ver2 = getAuditReader().find( Address.class, cracowId, 2 );
		assertThat( ver2, equalTo( cracow ) );
		assertThat( ver2.getTenants(), equalTo( cracow.getTenants() ) );
		assertThat( ver2.getLandlord().getId(), equalTo( cracow.getLandlord().getId() ) );

		cracow.setCity( "Krakow" );

		Address ver3 = getAuditReader().find( Address.class, cracowId, 3 );
		assertThat( ver3, equalTo( cracow ) );
	}

	@DynamicTest
	public void testHistoryOfWarsaw() {
		Address warsaw = new Address( "Warsaw", warsawId );
		Person kinga = new Person( "Kinga", kingaId );
		Person lukasz = new Person( "Lukasz", lukaszId );
		warsaw.getTenants().add( lukasz );
		warsaw.getTenants().add( kinga );
		warsaw.setLandlord( lukasz );

		Address ver1 = getAuditReader().find( Address.class, warsawId, 1 );
		assertThat( ver1, equalTo( warsaw ) );
		assertThat( ver1.getTenants(), equalTo( warsaw.getTenants() ) );
		assertThat( ver1.getLandlord().getId(), equalTo( warsaw.getLandlord().getId() ) );

		warsaw.setLandlord( kinga );

		Address ver5 = getAuditReader().find( Address.class, warsawId, 5 );
		assertThat( ver5.getLandlord().getId(), equalTo( warsaw.getLandlord().getId() ) );
	}
}
