/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.multiplerelations;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7073")
public class MultipleAssociationsTest extends BaseEnversJPAFunctionalTestCase {
	private long lukaszId = 0;
	private long kingaId = 0;
	private long warsawId = 0;
	private long cracowId = 0;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class, Address.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
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
		em.getTransaction().commit();

		lukaszId = lukasz.getId();
		kingaId = kinga.getId();
		warsawId = warsaw.getId();

		// Revision 2
		em.getTransaction().begin();
		kinga = em.find( Person.class, kinga.getId() );
		Address cracow = new Address( "Cracow" );
		kinga.getAddresses().add( cracow );
		cracow.getTenants().add( kinga );
		cracow.setLandlord( kinga );
		em.persist( cracow );
		em.getTransaction().commit();

		cracowId = cracow.getId();

		// Revision 3
		em.getTransaction().begin();
		cracow = em.find( Address.class, cracow.getId() );
		cracow.setCity( "Krakow" );
		em.merge( cracow );
		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();
		lukasz = em.find( Person.class, lukasz.getId() );
		lukasz.setName( "Lucas" );
		em.merge( lukasz );
		em.getTransaction().commit();

		// Revision 5
		em.getTransaction().begin();
		warsaw = em.find( Address.class, warsaw.getId() );
		lukasz = em.find( Person.class, lukasz.getId() );
		kinga = em.find( Person.class, kinga.getId() );
		warsaw.setLandlord( kinga );
		kinga.getOwnedAddresses().add( warsaw );
		lukasz.getOwnedAddresses().remove( warsaw );
		em.merge( warsaw );
		em.merge( lukasz );
		em.merge( kinga );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1, 4, 5 ), getAuditReader().getRevisions( Person.class, lukaszId ) );
		Assert.assertEquals( Arrays.asList( 1, 2, 5 ), getAuditReader().getRevisions( Person.class, kingaId ) );
		Assert.assertEquals( Arrays.asList( 1, 5 ), getAuditReader().getRevisions( Address.class, warsawId ) );
		Assert.assertEquals( Arrays.asList( 2, 3 ), getAuditReader().getRevisions( Address.class, cracowId ) );
	}

	@Test
	public void testHistoryOfLukasz() {
		Person lukasz = new Person( "Lukasz", lukaszId );
		Address warsaw = new Address( "Warsaw", warsawId );
		lukasz.getAddresses().add( warsaw );
		lukasz.getOwnedAddresses().add( warsaw );

		Person ver1 = getAuditReader().find( Person.class, lukaszId, 1 );
		Assert.assertEquals( lukasz, ver1 );
		Assert.assertEquals( lukasz.getAddresses(), ver1.getAddresses() );
		Assert.assertEquals( lukasz.getOwnedAddresses(), ver1.getOwnedAddresses() );

		lukasz.setName( "Lucas" );

		Person ver4 = getAuditReader().find( Person.class, lukaszId, 4 );
		Assert.assertEquals( lukasz, ver4 );

		lukasz.getOwnedAddresses().remove( warsaw );

		Person ver5 = getAuditReader().find( Person.class, lukaszId, 5 );
		Assert.assertEquals( lukasz.getOwnedAddresses(), ver5.getOwnedAddresses() );
	}

	@Test
	public void testHistoryOfKinga() {
		Person kinga = new Person( "Kinga", kingaId );
		Address warsaw = new Address( "Warsaw", warsawId );
		kinga.getAddresses().add( warsaw );

		Person ver1 = getAuditReader().find( Person.class, kingaId, 1 );
		Assert.assertEquals( kinga, ver1 );
		Assert.assertEquals( kinga.getAddresses(), ver1.getAddresses() );
		Assert.assertEquals( kinga.getOwnedAddresses(), ver1.getOwnedAddresses() );

		Address cracow = new Address( "Cracow", cracowId );
		kinga.getOwnedAddresses().add( cracow );
		kinga.getAddresses().add( cracow );

		Person ver2 = getAuditReader().find( Person.class, kingaId, 2 );
		Assert.assertEquals( kinga, ver2 );
		Assert.assertEquals( kinga.getAddresses(), ver2.getAddresses() );
		Assert.assertEquals( kinga.getOwnedAddresses(), ver2.getOwnedAddresses() );

		kinga.getOwnedAddresses().add( warsaw );
		cracow.setCity( "Krakow" );

		Person ver5 = getAuditReader().find( Person.class, kingaId, 5 );
		Assert.assertEquals( TestTools.makeSet( kinga.getAddresses() ), TestTools.makeSet( ver5.getAddresses() ) );
		Assert.assertEquals(
				TestTools.makeSet( kinga.getOwnedAddresses() ),
				TestTools.makeSet( ver5.getOwnedAddresses() )
		);
	}

	@Test
	public void testHistoryOfCracow() {
		Address cracow = new Address( "Cracow", cracowId );
		Person kinga = new Person( "Kinga", kingaId );
		cracow.getTenants().add( kinga );
		cracow.setLandlord( kinga );

		Address ver2 = getAuditReader().find( Address.class, cracowId, 2 );
		Assert.assertEquals( cracow, ver2 );
		Assert.assertEquals( cracow.getTenants(), ver2.getTenants() );
		Assert.assertEquals( cracow.getLandlord().getId(), ver2.getLandlord().getId() );

		cracow.setCity( "Krakow" );

		Address ver3 = getAuditReader().find( Address.class, cracowId, 3 );
		Assert.assertEquals( cracow, ver3 );
	}

	@Test
	public void testHistoryOfWarsaw() {
		Address warsaw = new Address( "Warsaw", warsawId );
		Person kinga = new Person( "Kinga", kingaId );
		Person lukasz = new Person( "Lukasz", lukaszId );
		warsaw.getTenants().add( lukasz );
		warsaw.getTenants().add( kinga );
		warsaw.setLandlord( lukasz );

		Address ver1 = getAuditReader().find( Address.class, warsawId, 1 );
		Assert.assertEquals( warsaw, ver1 );
		Assert.assertEquals( warsaw.getTenants(), ver1.getTenants() );
		Assert.assertEquals( warsaw.getLandlord().getId(), ver1.getLandlord().getId() );

		warsaw.setLandlord( kinga );

		Address ver5 = getAuditReader().find( Address.class, warsawId, 5 );
		Assert.assertEquals( warsaw.getLandlord().getId(), ver5.getLandlord().getId() );
	}
}
