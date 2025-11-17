/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@JiraKey(value = "HHH-11735")
@Jpa(annotatedClasses = {
		AssociationToManyJoinQueryTest.EntityA.class,
		AssociationToManyJoinQueryTest.EntityB.class,
		AssociationToManyJoinQueryTest.EntityC.class
})
@EnversTest
public class AssociationToManyJoinQueryTest {

	private EntityA aEmpty;
	private EntityA aOneToMany;
	private EntityA aManyToMany;
	private EntityA aBidiOneToManyInverse;
	private EntityA aBidiManyToManyOwning;
	private EntityA aBidiManyToManyInverse;
	private EntityB b1;
	private EntityB b2;
	private EntityB b3;
	private EntityC c1;
	private EntityC c2;
	private EntityC c3;

	@Entity(name = "EntityA")
	@Audited
	public static class EntityA {

		@Id
		private Long id;

		private String name;

		@OneToMany
		@AuditJoinTable(name = "entitya_onetomany_entityb_aud")
		private Set<EntityB> bOneToMany = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "entitya_manytomany_entityb")
		private Set<EntityB> bManyToMany = new HashSet<>();

		@OneToMany(mappedBy = "bidiAManyToOneOwning")
		private Set<EntityC> bidiCOneToManyInverse = new HashSet<>();

		@ManyToMany
		@AuditJoinTable(name = "entitya_entityc_bidi_aud")
		private Set<EntityC> bidiCManyToManyOwning = new HashSet<>();

		@ManyToMany(mappedBy = "bidiAManyToManyOwning")
		private Set<EntityC> bidiCManyToManyInverse = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<EntityB> getbOneToMany() {
			return bOneToMany;
		}

		public void setbOneToMany(Set<EntityB> bOneToMany) {
			this.bOneToMany = bOneToMany;
		}

		public Set<EntityB> getbManyToMany() {
			return bManyToMany;
		}

		public void setbManyToMany(Set<EntityB> bManyToMany) {
			this.bManyToMany = bManyToMany;
		}

		public Set<EntityC> getBidiCOneToManyInverse() {
			return bidiCOneToManyInverse;
		}

		public void setBidiCOneToManyInverse(Set<EntityC> bidiCOneToManyInverse) {
			this.bidiCOneToManyInverse = bidiCOneToManyInverse;
		}

		public Set<EntityC> getBidiCManyToManyOwning() {
			return bidiCManyToManyOwning;
		}

		public void setBidiCManyToManyOwning(Set<EntityC> bidiCManyToManyOwning) {
			this.bidiCManyToManyOwning = bidiCManyToManyOwning;
		}

		public Set<EntityC> getBidiCManyToManyInverse() {
			return bidiCManyToManyInverse;
		}

		public void setBidiCManyToManyInverse(Set<EntityC> bidiCManyToManyInverse) {
			this.bidiCManyToManyInverse = bidiCManyToManyInverse;
		}

	}

	@Entity(name = "EntityB")
	@Audited
	public static class EntityB {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityC")
	@Audited
	public static class EntityC {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		private EntityA bidiAManyToOneOwning;

		@ManyToMany
		@JoinTable(name = "entityc_entitya_bidi")
		private Set<EntityA> bidiAManyToManyOwning = new HashSet<>();

		@ManyToMany(mappedBy = "bidiCManyToManyOwning")
		private Set<EntityA> bidiAManyToManyInverse = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public EntityA getBidiAManyToOneOwning() {
			return bidiAManyToOneOwning;
		}

		public void setBidiAManyToOneOwning(EntityA bidiAManyToOneOwning) {
			this.bidiAManyToOneOwning = bidiAManyToOneOwning;
		}

		public Set<EntityA> getBidiAManyToManyOwning() {
			return bidiAManyToManyOwning;
		}

		public void setBidiAManyToManyOwning(Set<EntityA> bidiAManyToManyOwning) {
			this.bidiAManyToManyOwning = bidiAManyToManyOwning;
		}

		public Set<EntityA> getBidiAManyToManyInverse() {
			return bidiAManyToManyInverse;
		}

		public void setBidiAManyToManyInverse(Set<EntityA> bidiAManyToManyInverse) {
			this.bidiAManyToManyInverse = bidiAManyToManyInverse;
		}

	}

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// revision 1:
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			b1 = new EntityB();
			b1.setId( 21L );
			b1.setName( "B1" );
			em.persist( b1 );
			b2 = new EntityB();
			b2.setId( 22L );
			b2.setName( "B2" );
			em.persist( b2 );
			b3 = new EntityB();
			b3.setId( 23L );
			b3.setName( "B3" );
			em.persist( b3 );
			c1 = new EntityC();
			c1.setId( 31L );
			c1.setName( "C1" );
			em.persist( c1 );
			c2 = new EntityC();
			c2.setId( 32L );
			c2.setName( "C2" );
			em.persist( c2 );
			c3 = new EntityC();
			c3.setId( 33L );
			c3.setName( "C3" );
			em.persist( c3 );
			aEmpty = new EntityA();
			aEmpty.setId( 1L );
			aEmpty.setName( "aEmpty" );
			em.persist( aEmpty );
			aOneToMany = new EntityA();
			aOneToMany.setId( 2L );
			aOneToMany.setName( "aOneToMany" );
			aOneToMany.getbOneToMany().add( b1 );
			aOneToMany.getbOneToMany().add( b3 );
			em.persist( aOneToMany );
			aManyToMany = new EntityA();
			aManyToMany.setId( 3L );
			aManyToMany.setName( "aManyToMany" );
			aManyToMany.getbManyToMany().add( b1 );
			aManyToMany.getbManyToMany().add( b3 );
			em.persist( aManyToMany );
			aBidiOneToManyInverse = new EntityA();
			aBidiOneToManyInverse.setId( 4L );
			aBidiOneToManyInverse.setName( "aBidiOneToManyInverse" );
			aBidiOneToManyInverse.getBidiCOneToManyInverse().add( c1 );
			c1.setBidiAManyToOneOwning( aBidiOneToManyInverse );
			aBidiOneToManyInverse.getBidiCOneToManyInverse().add( c3 );
			c3.setBidiAManyToOneOwning( aBidiOneToManyInverse );
			em.persist( aBidiOneToManyInverse );
			aBidiManyToManyOwning = new EntityA();
			aBidiManyToManyOwning.setId( 5L );
			aBidiManyToManyOwning.setName( "aBidiManyToManyOwning" );
			aBidiManyToManyOwning.getBidiCManyToManyOwning().add( c1 );
			c1.getBidiAManyToManyInverse().add( aBidiManyToManyOwning );
			aBidiManyToManyOwning.getBidiCManyToManyOwning().add( c3 );
			c3.getBidiAManyToManyInverse().add( aBidiManyToManyOwning );
			em.persist( aBidiManyToManyOwning );
			aBidiManyToManyInverse = new EntityA();
			aBidiManyToManyInverse.setId( 6L );
			aBidiManyToManyInverse.setName( "aBidiManyToManyInverse" );
			aBidiManyToManyInverse.getBidiCManyToManyInverse().add( c1 );
			c1.getBidiAManyToManyOwning().add( aBidiManyToManyInverse );
			aBidiManyToManyInverse.getBidiCManyToManyInverse().add( c3 );
			c3.getBidiAManyToManyOwning().add( aBidiManyToManyInverse );
			em.persist( aBidiManyToManyInverse );
			em.getTransaction().commit();

			em.getTransaction().begin();
			aOneToMany.getbOneToMany().remove( b1 );
			aOneToMany.getbOneToMany().add( b2 );
			aManyToMany.getbManyToMany().remove( b1 );
			aManyToMany.getbManyToMany().add( b2 );
			aBidiOneToManyInverse.getBidiCOneToManyInverse().remove( c1 );
			c1.setBidiAManyToOneOwning( null );
			aBidiOneToManyInverse.getBidiCOneToManyInverse().add( c2 );
			c2.setBidiAManyToOneOwning( aBidiOneToManyInverse );
			aBidiManyToManyOwning.getBidiCManyToManyOwning().remove( c1 );
			c1.getBidiAManyToManyInverse().remove( aBidiManyToManyOwning );
			aBidiManyToManyOwning.getBidiCManyToManyOwning().add( c2 );
			c2.getBidiAManyToManyInverse().add( aBidiManyToManyOwning );
			aBidiManyToManyInverse.getBidiCManyToManyInverse().remove( c1 );
			c1.getBidiAManyToManyOwning().remove( aBidiManyToManyInverse );
			aBidiManyToManyInverse.getBidiCManyToManyInverse().add( c2 );
			c2.getBidiAManyToManyOwning().add( aBidiManyToManyInverse );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testOneToManyInnerJoin(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list1 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bOneToMany", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "B1" ) ).getResultList();
			assertEquals( 1, list1.size(), "Expected exactly one entity" );
			EntityA entityA1 = (EntityA) list1.get( 0 );
			assertEquals( aOneToMany.getId(), entityA1.getId(), "Expected the correct entity to be resolved" );

			List<?> list2 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bOneToMany", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "B2" ) ).getResultList();
			assertTrue( list2.isEmpty(), "Expected no entities to be returned, since B2 has been added in revision 2" );

			List<?> list3 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 2 ).traverseRelation( "bOneToMany", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "B2" ) ).getResultList();
			assertEquals( 1, list3.size(), "Expected exactly one entity" );
			EntityA entityA3 = (EntityA) list3.get( 0 );
			assertEquals( aOneToMany.getId(), entityA3.getId(), "Expected the correct entity to be resolved" );

			List<?> list4 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 2 ).traverseRelation( "bOneToMany", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "B1" ) ).getResultList();
			assertTrue( list4.isEmpty(), "Expected no entities to be returned, since B1 has been removed in revision 2" );
		} );
	}

	@Test
	public void testOneToManyLeftJoin(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bOneToMany", JoinType.LEFT, "b" ).up()
					.add( AuditEntity.or( AuditEntity.property( "name" ).eq( "aEmpty" ), AuditEntity.property( "b", "name" ).eq( "B1" ) ) )
					.getResultList();
			assertTrue( listContainsIds( list, aEmpty.getId(), aOneToMany.getId() ), "Expected the correct entities to be resolved" );
		} );
	}

	@Test
	public void testManyToManyInnerJoin(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list1 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bManyToMany", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "B1" ) ).getResultList();
			assertEquals( 1, list1.size(), "Expected exactly one entity" );
			EntityA entityA1 = (EntityA) list1.get( 0 );
			assertEquals( aManyToMany.getId(), entityA1.getId(), "Expected the correct entity to be resolved" );

			List<?> list2 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bManyToMany", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "B2" ) ).getResultList();
			assertTrue( list2.isEmpty(), "Expected no entities to be returned, since B2 has been added in revision 2" );

			List<?> list3 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 2 ).traverseRelation( "bManyToMany", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "B2" ) ).getResultList();
			assertEquals( 1, list3.size(), "Expected exactly one entity" );
			EntityA entityA3 = (EntityA) list3.get( 0 );
			assertEquals( aManyToMany.getId(), entityA3.getId(), "Expected the correct entity to be resolved" );

			List<?> list4 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 2 ).traverseRelation( "bManyToMany", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "B1" ) ).getResultList();
			assertTrue( list4.isEmpty(), "Expected no entities to be returned, since B1 has been removed in revision 2" );
		} );
	}

	@Test
	public void testManyToManyLeftJoin(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bManyToMany", JoinType.LEFT, "b" ).up()
					.add( AuditEntity.or( AuditEntity.property( "name" ).eq( "aEmpty" ), AuditEntity.property( "b", "name" ).eq( "B1" ) ) )
					.getResultList();
			assertTrue( listContainsIds( list, aEmpty.getId(), aManyToMany.getId() ), "Expected the correct entities to be resolved" );
		} );
	}

	@Test
	public void testBidiOneToManyInnerJoin(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list1 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bidiCOneToManyInverse", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C1" ) ).getResultList();
			assertEquals( 1, list1.size(), "Expected exactly one entity" );
			EntityA entityA1 = (EntityA) list1.get( 0 );
			assertEquals( aBidiOneToManyInverse.getId(), entityA1.getId(), "Expected the correct entity to be resolved" );

			List<?> list2 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bidiCOneToManyInverse", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C2" ) ).getResultList();
			assertTrue( list2.isEmpty(), "Expected no entities to be returned, since C2 has been added in revision 2" );

			List<?> list3 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 2 ).traverseRelation( "bidiCOneToManyInverse", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C2" ) ).getResultList();
			assertEquals( 1, list3.size(), "Expected exactly one entity" );
			EntityA entityA3 = (EntityA) list3.get( 0 );
			assertEquals( aBidiOneToManyInverse.getId(), entityA3.getId(), "Expected the correct entity to be resolved" );

			List<?> list4 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 2 ).traverseRelation( "bidiCOneToManyInverse", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C1" ) ).getResultList();
			assertTrue( list4.isEmpty(), "Expected no entities to be returned, since C1 has been removed in revision 2" );
		} );
	}

	@Test
	public void testBidiOneToManyLeftJoin(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bidiCOneToManyInverse", JoinType.LEFT, "c" )
					.up()
					.add( AuditEntity.or( AuditEntity.property( "name" ).eq( "aEmpty" ), AuditEntity.property( "c", "name" ).eq( "C1" ) ) )
					.getResultList();
			assertTrue( listContainsIds( list, aEmpty.getId(), aBidiOneToManyInverse.getId() ), "Expected the correct entities to be resolved" );
		} );
	}

	@Test
	public void testBidiManyToManyOwningInnerJoin(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list1 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bidiCManyToManyOwning", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C1" ) ).getResultList();
			assertEquals( 1, list1.size(), "Expected exactly one entity" );
			EntityA entityA1 = (EntityA) list1.get( 0 );
			assertEquals( aBidiManyToManyOwning.getId(), entityA1.getId(), "Expected the correct entity to be resolved" );

			List<?> list2 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bidiCManyToManyOwning", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C2" ) ).getResultList();
			assertTrue( list2.isEmpty(), "Expected no entities to be returned, since C2 has been added in revision 2" );

			List<?> list3 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 2 ).traverseRelation( "bidiCManyToManyOwning", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C2" ) ).getResultList();
			assertEquals( 1, list3.size(), "Expected exactly one entity" );
			EntityA entityA3 = (EntityA) list3.get( 0 );
			assertEquals( aBidiManyToManyOwning.getId(), entityA3.getId(), "Expected the correct entity to be resolved" );

			List<?> list4 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 2 ).traverseRelation( "bidiCManyToManyOwning", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C1" ) ).getResultList();
			assertTrue( list4.isEmpty(), "Expected no entities to be returned, since C1 has been removed in revision 2" );
		} );
	}

	@Test
	public void testBidiManyToManyOwningLeftJoin(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bidiCManyToManyOwning", JoinType.LEFT, "c" )
					.up()
					.add( AuditEntity.or( AuditEntity.property( "name" ).eq( "aEmpty" ), AuditEntity.property( "c", "name" ).eq( "C1" ) ) )
					.getResultList();
			assertTrue( listContainsIds( list, aEmpty.getId(), aBidiManyToManyOwning.getId() ), "Expected the correct entities to be resolved" );
		} );
	}

	@Test
	public void testBidiManyToManyInverseInnerJoin(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list1 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bidiCManyToManyInverse", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C1" ) ).getResultList();
			assertEquals( 1, list1.size(), "Expected exactly one entity" );
			EntityA entityA1 = (EntityA) list1.get( 0 );
			assertEquals( aBidiManyToManyInverse.getId(), entityA1.getId(), "Expected the correct entity to be resolved" );

			List<?> list2 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bidiCManyToManyInverse", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C2" ) ).getResultList();
			assertTrue( list2.isEmpty(), "Expected no entities to be returned, since C2 has been added in revision 2" );

			List<?> list3 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 2 ).traverseRelation( "bidiCManyToManyInverse", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C2" ) ).getResultList();
			assertEquals( 1, list3.size(), "Expected exactly one entity" );
			EntityA entityA3 = (EntityA) list3.get( 0 );
			assertEquals( aBidiManyToManyInverse.getId(), entityA3.getId(), "Expected the correct entity to be resolved" );

			List<?> list4 = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 2 ).traverseRelation( "bidiCManyToManyInverse", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "C1" ) ).getResultList();
			assertTrue( list4.isEmpty(), "Expected no entities to be returned, since C1 has been removed in revision 2" );
		} );
	}

	@Test
	public void testBidiManyToManyInverseLeftJoin(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> list = AuditReaderFactory.get( em ).createQuery().forEntitiesAtRevision( EntityA.class, 1 ).traverseRelation( "bidiCManyToManyInverse", JoinType.LEFT, "c" )
					.up()
					.add( AuditEntity.or( AuditEntity.property( "name" ).eq( "aEmpty" ), AuditEntity.property( "c", "name" ).eq( "C1" ) ) )
					.getResultList();
			assertTrue( listContainsIds( list, aEmpty.getId(), aBidiManyToManyInverse.getId() ), "Expected the correct entities to be resolved" );
		} );
	}

	private boolean listContainsIds(final Collection<?> entities, final long... ids) {
		final Set<Long> idSet = new HashSet<>();
		for ( final Object entity : entities ) {
			idSet.add( ( (EntityA) entity ).getId() );
		}
		boolean result = true;
		for ( final long id : ids ) {
			if ( !idSet.contains( id ) ) {
				result = false;
				break;
			}
		}
		return result;
	}

}
