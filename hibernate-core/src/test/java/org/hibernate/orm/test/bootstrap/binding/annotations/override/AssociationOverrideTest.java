/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.PersistenceException;

import org.hibernate.Transaction;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.orm.test.util.SchemaUtil;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Employee.class,
				Location.class,
				Move.class,
				Trip.class,
				PhoneNumber.class,
				Addr.class,
				SocialSite.class,
				SocialTouchPoints.class
		}
)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "legacy-jpa")
})
@SessionFactory
public class AssociationOverrideTest {

	@Test
	public void testOverriding(SessionFactoryScope scope) {
		Location paris = new Location();
		paris.setName( "Paris" );
		Location atlanta = new Location();
		atlanta.setName( "Atlanta" );
		Trip trip = new Trip();
		trip.setFrom( paris );
		//trip.setTo( atlanta );
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					session.persist( paris );
					session.persist( atlanta );
					try {
						session.persist( trip );
						session.flush();
						fail( "Should be non nullable" );
					}
					catch (PersistenceException e) {
						//success
					}
					finally {
						tx.rollback();
					}
				}
		);
	}


	@Test
	public void testSchemaCreation(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();

		assertTrue( SchemaUtil.isTablePresent( "Employee", metadata ) );
		assertTrue(
				"Overridden @JoinColumn fails",
				SchemaUtil.isColumnPresent( "Employee", "fld_address_fk", metadata )
		);

		assertTrue( "Overridden @JoinTable name fails", SchemaUtil.isTablePresent( "tbl_empl_sites", metadata ) );
		assertTrue(
				"Overridden @JoinTable with default @JoinColumn fails",
				SchemaUtil.isColumnPresent( "tbl_empl_sites", "employee_id", metadata )
		);
		assertTrue(
				"Overridden @JoinTable.inverseJoinColumn fails",
				SchemaUtil.isColumnPresent( "tbl_empl_sites", "to_website_fk", metadata )
		);
	}

	@Test
	public void testDottedNotation(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					try {
						ContactInfo ci = new ContactInfo();
						Addr address = new Addr();
						address.setCity( "Boston" );
						address.setCountry( "USA" );
						address.setState( "MA" );
						address.setStreet( "27 School Street" );
						address.setZipcode( "02108" );
						ci.setAddr( address );
						List<PhoneNumber> phoneNumbers = new ArrayList();
						PhoneNumber num = new PhoneNumber();
						num.setNumber( 5577188 );
						Employee e = new Employee();
						Collection employeeList = new ArrayList();
						employeeList.add( e );
						e.setContactInfo( ci );
						num.setEmployees( employeeList );
						phoneNumbers.add( num );
						ci.setPhoneNumbers( phoneNumbers );
						SocialTouchPoints socialPoints = new SocialTouchPoints();
						List<SocialSite> sites = new ArrayList<>();
						SocialSite site = new SocialSite();
						site.setEmployee( employeeList );
						site.setWebsite( "www.jboss.org" );
						sites.add( site );
						socialPoints.setWebsite( sites );
						ci.setSocial( socialPoints );
						session.persist( e );
						tx.commit();

						tx.begin();
						session.clear();
						session.get( Employee.class, e.getId() );
						tx.commit();
					}
					catch (Exception e) {
						if ( tx != null && tx.isActive() ) {
							tx.rollback();
						}
						throw e;
					}
				}
		);
	}

}
