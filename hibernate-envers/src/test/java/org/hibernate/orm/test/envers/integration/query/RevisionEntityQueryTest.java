/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.List;

import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.ids.EmbId;
import org.hibernate.orm.test.envers.entities.ids.EmbIdTestEntity;
import org.hibernate.orm.test.envers.entities.ids.MulId;
import org.hibernate.orm.test.envers.entities.ids.MulIdTestEntity;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
public class RevisionEntityQueryTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrIntTestEntity.class, MulIdTestEntity.class, EmbIdTestEntity.class };
	}

	@Test
	@Priority( 10 )
	public void initData() {
		// Revision 1
		final EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
		StrIntTestEntity site2 = new StrIntTestEntity( "a", 10 );
		StrIntTestEntity site3 = new StrIntTestEntity( "b", 5 );

		em.persist( site1 );
		em.persist( site2 );
		em.persist( site3 );

		final Integer id1 = site1.getId();
		final Integer id2 = site2.getId();
		final Integer id3 = site3.getId();

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		final MulId mulId1 = new MulId( 1, 2 );
		em.persist( new MulIdTestEntity( mulId1.getId1(), mulId1.getId2(), "data" ) );

		final EmbId embId1 = new EmbId( 3, 4 );
		em.persist( new EmbIdTestEntity( embId1, "something" ) );

		site1 = em.find( StrIntTestEntity.class, id1 );
		site2 = em.find( StrIntTestEntity.class, id2 );

		site1.setStr1( "aBc" );
		site2.setNumber( 20 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		site3 = em.find( StrIntTestEntity.class, id3 );

		site3.setStr1( "a" );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		site1 = em.find( StrIntTestEntity.class, id1 );

		em.remove( site1 );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionEntityHqlQuery() {
		final EntityManager em = getEntityManager();
		em.getTransaction().begin();

		// note: simple name never worked, even before creating new dedicated entity classes, but fqn did
		final List<SequenceIdRevisionEntity> resultList = em.createQuery(
				String.format( "select e from %s e", SequenceIdRevisionEntity.class.getName() ),
				SequenceIdRevisionEntity.class
		).getResultList();

		assertThat( resultList ).hasSize( 4 );

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionEntityCriteriaQuery() {
		final EntityManager em = getEntityManager();
		em.getTransaction().begin();

		final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		final CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
		final Root<?> from = query.from( SequenceIdRevisionEntity.class );
		final List<Integer> resultList = em.createQuery( query.select( from.get( "id" ) ) ).getResultList();

		assertThat( resultList ).hasSize( 4 ).allSatisfy( Assertions::assertNotNull );

		em.getTransaction().commit();
	}

	@Test
	public void testQueryForRevisionsOfEntity() {
		final EntityManager em = getEntityManager();
		em.getTransaction().begin();

		//noinspection unchecked
		final List<Object> resultList = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true )
				.add( AuditEntity.id().eq( 1 ) )
				.add( AuditEntity.revisionNumber().between( 1, 3 ) )
				.getResultList();

		assertThat( resultList ).hasSize( 2 ).allMatch( r -> r instanceof SequenceIdRevisionEntity );

		em.getTransaction().commit();
	}
}
