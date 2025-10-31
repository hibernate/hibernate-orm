/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.multiplerelations;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7073")
@EnversTest
@Jpa(annotatedClasses = {Person.class, Address.class})
public class MultipleAssociationsTest {
	private long lukaszId = 0;
	private long kingaId = 0;
	private long warsawId = 0;
	private long cracowId = 0;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			Person lukasz = new Person( "Lukasz" );
			Person kinga = new Person( "Kinga" );
			Address warsaw = new Address( "Warsaw" );
			warsaw.getTenants().add( lukasz );
			warsaw.setLandlord( lukasz );
			warsaw.getTenants().add( kinga );
			lukasz.getAddresses().add( warsaw );
			lukasz.getOwnedAddresses().add( warsaw );
			kinga.getAddresses().add( warsaw );
			em.persist( lukasz );
			em.persist( kinga );
			em.persist( warsaw );
			lukaszId = lukasz.getId();
			kingaId = kinga.getId();
			warsawId = warsaw.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			Person kinga = em.find( Person.class, kingaId );
			Address cracow = new Address( "Cracow" );
			kinga.getAddresses().add( cracow );
			cracow.getTenants().add( kinga );
			cracow.setLandlord( kinga );
			em.persist( cracow );
			cracowId = cracow.getId();
		} );

		// Revision 3
		scope.inTransaction( em -> {
			Address cracow = em.find( Address.class, cracowId );
			cracow.setCity( "Krakow" );
			em.merge( cracow );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			Person lukasz = em.find( Person.class, lukaszId );
			lukasz.setName( "Lucas" );
			em.merge( lukasz );
		} );

		// Revision 5
		scope.inTransaction( em -> {
			Address warsaw = em.find( Address.class, warsawId );
			Person lukasz = em.find( Person.class, lukaszId );
			Person kinga = em.find( Person.class, kingaId );
			warsaw.setLandlord( kinga );
			kinga.getOwnedAddresses().add( warsaw );
			lukasz.getOwnedAddresses().remove( warsaw );
			em.merge( warsaw );
			em.merge( lukasz );
			em.merge( kinga );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 4, 5 ), auditReader.getRevisions( Person.class, lukaszId ) );
			assertEquals( Arrays.asList( 1, 2, 5 ), auditReader.getRevisions( Person.class, kingaId ) );
			assertEquals( Arrays.asList( 1, 5 ), auditReader.getRevisions( Address.class, warsawId ) );
			assertEquals( Arrays.asList( 2, 3 ), auditReader.getRevisions( Address.class, cracowId ) );
		} );
	}

	@Test
	public void testHistoryOfLukasz(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Person lukasz = new Person( "Lukasz", lukaszId );
			Address warsaw = new Address( "Warsaw", warsawId );
			lukasz.getAddresses().add( warsaw );
			lukasz.getOwnedAddresses().add( warsaw );

			Person ver1 = auditReader.find( Person.class, lukaszId, 1 );
			assertEquals( lukasz, ver1 );
			assertEquals( lukasz.getAddresses(), ver1.getAddresses() );
			assertEquals( lukasz.getOwnedAddresses(), ver1.getOwnedAddresses() );

			lukasz.setName( "Lucas" );

			Person ver4 = auditReader.find( Person.class, lukaszId, 4 );
			assertEquals( lukasz, ver4 );

			lukasz.getOwnedAddresses().remove( warsaw );

			Person ver5 = auditReader.find( Person.class, lukaszId, 5 );
			assertEquals( lukasz.getOwnedAddresses(), ver5.getOwnedAddresses() );
		} );
	}

	@Test
	public void testHistoryOfKinga(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Person kinga = new Person( "Kinga", kingaId );
			Address warsaw = new Address( "Warsaw", warsawId );
			kinga.getAddresses().add( warsaw );

			Person ver1 = auditReader.find( Person.class, kingaId, 1 );
			assertEquals( kinga, ver1 );
			assertEquals( kinga.getAddresses(), ver1.getAddresses() );
			assertEquals( kinga.getOwnedAddresses(), ver1.getOwnedAddresses() );

			Address cracow = new Address( "Cracow", cracowId );
			kinga.getOwnedAddresses().add( cracow );
			kinga.getAddresses().add( cracow );

			Person ver2 = auditReader.find( Person.class, kingaId, 2 );
			assertEquals( kinga, ver2 );
			assertEquals( kinga.getAddresses(), ver2.getAddresses() );
			assertEquals( kinga.getOwnedAddresses(), ver2.getOwnedAddresses() );

			kinga.getOwnedAddresses().add( warsaw );
			cracow.setCity( "Krakow" );

			Person ver5 = auditReader.find( Person.class, kingaId, 5 );
			assertEquals( TestTools.makeSet( kinga.getAddresses() ), TestTools.makeSet( ver5.getAddresses() ) );
			assertEquals(
					TestTools.makeSet( kinga.getOwnedAddresses() ),
					TestTools.makeSet( ver5.getOwnedAddresses() )
			);
		} );
	}

	@Test
	public void testHistoryOfCracow(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Address cracow = new Address( "Cracow", cracowId );
			Person kinga = new Person( "Kinga", kingaId );
			cracow.getTenants().add( kinga );
			cracow.setLandlord( kinga );

			Address ver2 = auditReader.find( Address.class, cracowId, 2 );
			assertEquals( cracow, ver2 );
			assertEquals( cracow.getTenants(), ver2.getTenants() );
			assertEquals( cracow.getLandlord().getId(), ver2.getLandlord().getId() );

			cracow.setCity( "Krakow" );

			Address ver3 = auditReader.find( Address.class, cracowId, 3 );
			assertEquals( cracow, ver3 );
		} );
	}

	@Test
	public void testHistoryOfWarsaw(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Address warsaw = new Address( "Warsaw", warsawId );
			Person kinga = new Person( "Kinga", kingaId );
			Person lukasz = new Person( "Lukasz", lukaszId );
			warsaw.getTenants().add( lukasz );
			warsaw.getTenants().add( kinga );
			warsaw.setLandlord( lukasz );

			Address ver1 = auditReader.find( Address.class, warsawId, 1 );
			assertEquals( warsaw, ver1 );
			assertEquals( warsaw.getTenants(), ver1.getTenants() );
			assertEquals( warsaw.getLandlord().getId(), ver1.getLandlord().getId() );

			warsaw.setLandlord( kinga );

			Address ver5 = auditReader.find( Address.class, warsawId, 5 );
			assertEquals( warsaw.getLandlord().getId(), ver5.getLandlord().getId() );
		} );
	}
}
