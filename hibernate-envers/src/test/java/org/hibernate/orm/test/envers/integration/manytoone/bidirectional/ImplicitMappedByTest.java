/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.bidirectional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
@JiraKey(value = "HHH-4962")
@EnversTest
@Jpa(annotatedClasses = { OneToManyOwned.class, ManyToOneOwning.class })
public class ImplicitMappedByTest {
	private Long ownedId = null;
	private Long owning1Id = null;
	private Long owning2Id = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		OneToManyOwned owned = new OneToManyOwned( "data", null );
		Set<ManyToOneOwning> referencing = new HashSet<ManyToOneOwning>();
		ManyToOneOwning owning1 = new ManyToOneOwning( "data1", owned );
		referencing.add( owning1 );
		ManyToOneOwning owning2 = new ManyToOneOwning( "data2", owned );
		referencing.add( owning2 );
		owned.setReferencing( referencing );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( owned );
			em.persist( owning1 );
			em.persist( owning2 );
		} );

		ownedId = owned.getId();
		owning1Id = owning1.getId();
		owning2Id = owning2.getId();

		// Revision 2
		scope.inTransaction( em -> {
			ManyToOneOwning o1 = em.find( ManyToOneOwning.class, owning1.getId() );
			em.remove( o1 );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			ManyToOneOwning o2 = em.find( ManyToOneOwning.class, owning2.getId() );
			o2.setData( "data2modified" );
			em.merge( o2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( OneToManyOwned.class, ownedId ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( ManyToOneOwning.class, owning1Id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( ManyToOneOwning.class, owning2Id ) );
		} );
	}

	@Test
	public void testHistoryOfOwned(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			OneToManyOwned owned = new OneToManyOwned( "data", null, ownedId );
			ManyToOneOwning owning1 = new ManyToOneOwning( "data1", owned, owning1Id );
			ManyToOneOwning owning2 = new ManyToOneOwning( "data2", owned, owning2Id );

			OneToManyOwned ver1 = auditReader.find( OneToManyOwned.class, ownedId, 1 );
			assertEquals( owned, ver1 );
			assertEquals( TestTools.makeSet( owning1, owning2 ), ver1.getReferencing() );

			OneToManyOwned ver2 = auditReader.find( OneToManyOwned.class, ownedId, 2 );
			assertEquals( owned, ver2 );
			assertEquals( TestTools.makeSet( owning2 ), ver2.getReferencing() );
		} );
	}

	@Test
	public void testHistoryOfOwning1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ManyToOneOwning ver1 = new ManyToOneOwning( "data1", null, owning1Id );
			assertEquals( ver1, auditReader.find( ManyToOneOwning.class, owning1Id, 1 ) );
		} );
	}

	@Test
	public void testHistoryOfOwning2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			OneToManyOwned owned = new OneToManyOwned( "data", null, ownedId );
			ManyToOneOwning owning1 = new ManyToOneOwning( "data2", owned, owning2Id );
			ManyToOneOwning owning3 = new ManyToOneOwning( "data2modified", owned, owning2Id );

			ManyToOneOwning ver1 = auditReader.find( ManyToOneOwning.class, owning2Id, 1 );
			ManyToOneOwning ver3 = auditReader.find( ManyToOneOwning.class, owning2Id, 3 );

			assertEquals( owning1, ver1 );
			assertEquals( owned.getId(), ver1.getReferences().getId() );
			assertEquals( owning3, ver3 );
			assertEquals( owned.getId(), ver3.getReferences().getId() );
		} );
	}
}
