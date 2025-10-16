/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A test which checks if the data of a deleted entity is stored when the setting is on.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings("unchecked")
@Jpa(
		annotatedClasses = {StrIntTestEntity.class},
		integrationSettings = {
				@org.hibernate.testing.orm.junit.Setting(name = EnversSettings.STORE_DATA_AT_DELETE, value = "true")
		}
)
@EnversTest
public class StoreDeletedData {
	private Integer id1;
	private Integer id2;
	private Integer id3;

	@Test
	@Order(0)
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
			em.persist( site1 );
			id1 = site1.getId();
			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();
			StrIntTestEntity site1_2 = em.find( StrIntTestEntity.class, id1 );
			em.remove( site1_2 );
			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();
			StrIntTestEntity site2 = new StrIntTestEntity( "b", 20 );
			em.persist( site2 );
			id2 = site2.getId();
			StrIntTestEntity site3 = new StrIntTestEntity( "c", 30 );
			em.persist( site3 );
			id3 = site3.getId();
			em.getTransaction().commit();

			// Revision 4
			em.getTransaction().begin();
			StrIntTestEntity site2_4 = em.find( StrIntTestEntity.class, id2 );
			StrIntTestEntity site3_4 = em.find( StrIntTestEntity.class, id3 );
			em.remove( site2_4 );
			em.remove( site3_4 );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testRevisionsPropertyEqQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List revs_id1 = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.add( AuditEntity.id().eq( id1 ) )
					.getResultList();

			assertEquals( 2, revs_id1.size() );
			assertEquals( new StrIntTestEntity( "a", 10, id1 ), ((Object[]) revs_id1.get( 0 ))[0] );
			assertEquals( new StrIntTestEntity( "a", 10, id1 ), ((Object[]) revs_id1.get( 1 ))[0] );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7800")
	public void testMaximizeInDisjunction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<Integer> queryIds = Arrays.asList( id2, id3 );

			AuditDisjunction disjunction = AuditEntity.disjunction();
			for ( Integer id : queryIds ) {
				AuditCriterion crit = AuditEntity.revisionNumber().maximize()
						.add( AuditEntity.id().eq( id ) )
						.add( AuditEntity.revisionType().ne( RevisionType.DEL ) );
				disjunction.add( crit );
				// Workaround: using this line instead works correctly:
				// disjunction.add(AuditEntity.conjunction().add(crit));
			}

			List<?> beforeDeletionRevisions = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, false )
					.add( disjunction )
					.addOrder( AuditEntity.property( "id" ).asc() )
					.getResultList();

			assertEquals( 2, beforeDeletionRevisions.size() );

			Object[] result1 = (Object[]) beforeDeletionRevisions.get( 0 );
			Object[] result2 = (Object[]) beforeDeletionRevisions.get( 1 );

			assertEquals( new StrIntTestEntity( "b", 20, id2 ), result1[0] );
			// Making sure that we have received an entity added at revision 3.
			assertEquals( 3, ((SequenceIdRevisionEntity) result1[1]).getId() );
			assertEquals( RevisionType.ADD, result1[2] );
			assertEquals( new StrIntTestEntity( "c", 30, id3 ), result2[0] );
			// Making sure that we have received an entity added at revision 3.
			assertEquals( 3, ((SequenceIdRevisionEntity) result2[1]).getId() );
			assertEquals( RevisionType.ADD, result2[2] );
		} );
	}
}
