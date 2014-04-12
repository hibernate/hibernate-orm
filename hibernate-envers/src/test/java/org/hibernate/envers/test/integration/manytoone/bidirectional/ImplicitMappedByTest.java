package org.hibernate.envers.test.integration.manytoone.bidirectional;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-4962")
@FailureExpectedWithNewMetamodel( message = "Secondary tables are not supported by envers yet.")
public class ImplicitMappedByTest extends BaseEnversJPAFunctionalTestCase {
	private Long ownedId = null;
	private Long owning1Id = null;
	private Long owning2Id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {OneToManyOwned.class, ManyToOneOwning.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		OneToManyOwned owned = new OneToManyOwned( "data", null );
		Set<ManyToOneOwning> referencing = new HashSet<ManyToOneOwning>();
		ManyToOneOwning owning1 = new ManyToOneOwning( "data1", owned );
		referencing.add( owning1 );
		ManyToOneOwning owning2 = new ManyToOneOwning( "data2", owned );
		referencing.add( owning2 );
		owned.setReferencing( referencing );

		// Revision 1
		em.getTransaction().begin();
		em.persist( owned );
		em.persist( owning1 );
		em.persist( owning2 );
		em.getTransaction().commit();

		ownedId = owned.getId();
		owning1Id = owning1.getId();
		owning2Id = owning2.getId();

		// Revision 2
		em.getTransaction().begin();
		owning1 = em.find( ManyToOneOwning.class, owning1.getId() );
		em.remove( owning1 );
		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();
		owning2 = em.find( ManyToOneOwning.class, owning2.getId() );
		owning2.setData( "data2modified" );
		em.merge( owning2 );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( OneToManyOwned.class, ownedId ) );
		Assert.assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( ManyToOneOwning.class, owning1Id ) );
		Assert.assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( ManyToOneOwning.class, owning2Id ) );
	}

	@Test
	public void testHistoryOfOwned() {
		OneToManyOwned owned = new OneToManyOwned( "data", null, ownedId );
		ManyToOneOwning owning1 = new ManyToOneOwning( "data1", owned, owning1Id );
		ManyToOneOwning owning2 = new ManyToOneOwning( "data2", owned, owning2Id );

		OneToManyOwned ver1 = getAuditReader().find( OneToManyOwned.class, ownedId, 1 );
		Assert.assertEquals( owned, ver1 );
		Assert.assertEquals( TestTools.makeSet( owning1, owning2 ), ver1.getReferencing() );

		OneToManyOwned ver2 = getAuditReader().find( OneToManyOwned.class, ownedId, 2 );
		Assert.assertEquals( owned, ver2 );
		Assert.assertEquals( TestTools.makeSet( owning2 ), ver2.getReferencing() );
	}

	@Test
	public void testHistoryOfOwning1() {
		ManyToOneOwning ver1 = new ManyToOneOwning( "data1", null, owning1Id );
		Assert.assertEquals( ver1, getAuditReader().find( ManyToOneOwning.class, owning1Id, 1 ) );
	}

	@Test
	public void testHistoryOfOwning2() {
		OneToManyOwned owned = new OneToManyOwned( "data", null, ownedId );
		ManyToOneOwning owning1 = new ManyToOneOwning( "data2", owned, owning2Id );
		ManyToOneOwning owning3 = new ManyToOneOwning( "data2modified", owned, owning2Id );

		ManyToOneOwning ver1 = getAuditReader().find( ManyToOneOwning.class, owning2Id, 1 );
		ManyToOneOwning ver3 = getAuditReader().find( ManyToOneOwning.class, owning2Id, 3 );

		Assert.assertEquals( owning1, ver1 );
		Assert.assertEquals( owned.getId(), ver1.getReferences().getId() );
		Assert.assertEquals( owning3, ver3 );
		Assert.assertEquals( owned.getId(), ver3.getReferences().getId() );
	}
}