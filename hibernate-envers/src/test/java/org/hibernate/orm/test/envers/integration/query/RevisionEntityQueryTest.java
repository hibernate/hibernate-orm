/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.ids.EmbIdTestEntity;
import org.hibernate.orm.test.envers.entities.ids.MulId;
import org.hibernate.orm.test.envers.entities.ids.MulIdTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Marco Belladelli
 */
@Jpa(annotatedClasses = {StrIntTestEntity.class, MulIdTestEntity.class, EmbIdTestEntity.class})
@EnversTest
public class RevisionEntityQueryTest {

	private Integer id1;
	private Integer id2;
	private Integer id3;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
			StrIntTestEntity site2 = new StrIntTestEntity( "a", 10 );
			StrIntTestEntity site3 = new StrIntTestEntity( "b", 5 );

			em.persist( site1 );
			em.persist( site2 );
			em.persist( site3 );

			id1 = site1.getId();
			id2 = site2.getId();
			id3 = site3.getId();

			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();

			final MulId mulId1 = new MulId( 1, 2 );
			em.persist( new MulIdTestEntity( mulId1.getId1(), mulId1.getId2(), "data" ) );

			final EmbId embId1 = new EmbId( 3, 4 );
			em.persist( new EmbIdTestEntity( embId1, "something" ) );

			StrIntTestEntity site1_2 = em.find( StrIntTestEntity.class, id1 );
			StrIntTestEntity site2_2 = em.find( StrIntTestEntity.class, id2 );

			site1_2.setStr1( "aBc" );
			site2_2.setNumber( 20 );

			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();

			StrIntTestEntity site3_3 = em.find( StrIntTestEntity.class, id3 );

			site3_3.setStr1( "a" );

			em.getTransaction().commit();

			// Revision 4
			em.getTransaction().begin();

			StrIntTestEntity site1_4 = em.find( StrIntTestEntity.class, id1 );

			em.remove( site1_4 );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testRevisionEntityHqlQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			final List<SequenceIdRevisionEntity> resultList = em.createQuery(
					"select e from SequenceIdRevisionEntity e",
					SequenceIdRevisionEntity.class
			).getResultList();

			assertThat( resultList ).hasSize( 4 );

			assertThat( em.createQuery(
					String.format( "select e from %s e", SequenceIdRevisionEntity.class.getName() ),
					SequenceIdRevisionEntity.class
			).getResultList() ).containsAll( resultList );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testRevisionEntityCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			final CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
			final Root<?> from = query.from( SequenceIdRevisionEntity.class );
			final List<Integer> resultList = em.createQuery( query.select( from.get( "id" ) ) ).getResultList();

			assertThat( resultList ).hasSize( 4 ).allSatisfy( r -> assertNotNull( r ) );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testQueryForRevisionsOfEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			//noinspection unchecked
			final List<Object> resultList = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, true )
					.add( AuditEntity.id().eq( id1 ) )
					.add( AuditEntity.revisionNumber().between( 1, 3 ) )
					.getResultList();

			assertThat( resultList ).hasSize( 2 ).allMatch( r -> r instanceof SequenceIdRevisionEntity );

			em.getTransaction().commit();
		} );
	}
}
