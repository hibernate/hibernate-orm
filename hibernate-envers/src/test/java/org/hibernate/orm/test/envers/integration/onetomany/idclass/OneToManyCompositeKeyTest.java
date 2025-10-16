/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.idclass;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-7625")
@EnversTest
@Jpa(annotatedClasses = {OneToManyOwned.class, ManyToManyCompositeKey.class, ManyToOneOwned.class})
public class OneToManyCompositeKeyTest {
	private ManyToManyCompositeKey.ManyToManyId owning1Id = null;
	private ManyToManyCompositeKey.ManyToManyId owning2Id = null;
	private Long oneToManyId;
	private Long manyToOne1Id;
	private Long manyToOne2Id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( entityManager -> {
			OneToManyOwned oneToManyOwned = new OneToManyOwned( "data", null );
			ManyToOneOwned manyToOneOwned1 = new ManyToOneOwned( "data1" );
			ManyToOneOwned manyToOneOwned2 = new ManyToOneOwned( "data2" );
			ManyToManyCompositeKey owning1 = new ManyToManyCompositeKey( oneToManyOwned, manyToOneOwned1 );
			ManyToManyCompositeKey owning2 = new ManyToManyCompositeKey( oneToManyOwned, manyToOneOwned2 );

			entityManager.persist( oneToManyOwned );
			entityManager.persist( manyToOneOwned1 );
			entityManager.persist( manyToOneOwned2 );
			entityManager.persist( owning1 );
			entityManager.persist( owning2 );

			owning1Id = owning1.getId();
			owning2Id = owning2.getId();

			oneToManyId = oneToManyOwned.getId();
			manyToOne1Id = manyToOneOwned1.getId();
			manyToOne2Id = manyToOneOwned2.getId();
		} );

		// Revision 2
		scope.inTransaction( entityManager -> {
			ManyToManyCompositeKey owning1 = entityManager.find( ManyToManyCompositeKey.class, owning1Id );
			entityManager.remove( owning1 );
		} );

		// Revision 3
		scope.inTransaction( entityManager -> {
			ManyToManyCompositeKey owning2 = entityManager.find( ManyToManyCompositeKey.class, owning2Id );
			entityManager.remove( owning2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( ManyToManyCompositeKey.class, owning1Id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( ManyToManyCompositeKey.class, owning2Id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( OneToManyOwned.class, oneToManyId ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( ManyToOneOwned.class, manyToOne1Id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( ManyToOneOwned.class, manyToOne2Id ) );
		} );
	}

	@Test
	public void testOneToManyHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final OneToManyOwned rev1 = auditReader.find( OneToManyOwned.class, oneToManyId, 1 );
			assertEquals( "data", rev1.getData() );
			assertEquals( 2, rev1.getManyToManyCompositeKeys().size() );
		} );
	}

	@Test
	public void testManyToOne1History(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final ManyToOneOwned rev1 = auditReader.find( ManyToOneOwned.class, manyToOne1Id, 1 );
			assertEquals( "data1", rev1.getData() );
		} );
	}

	@Test
	public void testManyToOne2History(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final ManyToOneOwned rev1 = auditReader.find( ManyToOneOwned.class, manyToOne2Id, 1 );
			assertEquals( "data2", rev1.getData() );
		} );
	}

	@Test
	public void testOwning1History(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			// objects
			final OneToManyOwned oneToMany = new OneToManyOwned( 1L, "data", null );
			final ManyToOneOwned manyToOne = new ManyToOneOwned( 2L, "data1" );

			// insert revision
			final ManyToManyCompositeKey rev1 = auditReader.find( ManyToManyCompositeKey.class, owning1Id, 1 );
			assertEquals( oneToMany, rev1.getOneToMany() );
			assertEquals( manyToOne, rev1.getManyToOne() );

			// removal revision - find returns null for deleted
			assertNull( auditReader.find( ManyToManyCompositeKey.class, owning1Id, 2 ) );

			// fetch revision 2 using 'select deletions' api and verify.
			final ManyToManyCompositeKey rev2 = (ManyToManyCompositeKey) auditReader
					.createQuery()
					.forRevisionsOfEntity( ManyToManyCompositeKey.class, true, true )
					.add( AuditEntity.id().eq( owning1Id ) )
					.add( AuditEntity.revisionNumber().eq( 2 ) )
					.getSingleResult();
			assertEquals( oneToMany, rev2.getOneToMany() );
			assertEquals( manyToOne, rev2.getManyToOne() );
		} );
	}

	@Test
	public void testOwning2History(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			// objects
			final OneToManyOwned oneToMany = new OneToManyOwned( 1L, "data", null );
			final ManyToOneOwned manyToOne = new ManyToOneOwned( 3L, "data2" );

			// insert revision
			final ManyToManyCompositeKey rev1 = auditReader.find( ManyToManyCompositeKey.class, owning2Id, 1 );
			assertEquals( oneToMany, rev1.getOneToMany() );
			assertEquals( manyToOne, rev1.getManyToOne() );

			// removal revision - find returns null for deleted
			assertNull( auditReader.find( ManyToManyCompositeKey.class, owning2Id, 3 ) );

			// fetch revision 3 using 'select deletions' api and verify.
			final ManyToManyCompositeKey rev2 = (ManyToManyCompositeKey) auditReader
					.createQuery()
					.forRevisionsOfEntity( ManyToManyCompositeKey.class, true, true )
					.add( AuditEntity.id().eq( owning2Id ) )
					.add( AuditEntity.revisionNumber().eq( 3 ) )
					.getSingleResult();
			assertEquals( oneToMany, rev2.getOneToMany() );
			assertEquals( manyToOne, rev2.getManyToOne() );
		} );
	}
}
