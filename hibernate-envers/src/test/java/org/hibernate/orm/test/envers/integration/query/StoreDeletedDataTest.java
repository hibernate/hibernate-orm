/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A test which checks if the data of a deleted entity is stored when the setting is on.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings("unchecked")
@Jpa(
		annotatedClasses = {
				StrIntTestEntity.class
		},
		properties = @Setting(name = EnversSettings.STORE_DATA_AT_DELETE, value = "true")
)
@EnversTest
public class StoreDeletedDataTest {
	private Integer id1;
	private Integer id2;
	private Integer id3;


	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					// Revision 1
					StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
					entityManager.persist( site1 );
					id1 = site1.getId();
					entityManager.getTransaction().commit();

					// Revision 2
					entityManager.getTransaction().begin();
					entityManager.remove( site1 );
					entityManager.getTransaction().commit();

					// Revision 3
					entityManager.getTransaction().begin();
					StrIntTestEntity site2 = new StrIntTestEntity( "b", 20 );
					entityManager.persist( site2 );
					id2 = site2.getId();
					StrIntTestEntity site3 = new StrIntTestEntity( "c", 30 );
					entityManager.persist( site3 );
					id3 = site3.getId();
					entityManager.getTransaction().commit();

					// Revision 4
					entityManager.getTransaction().begin();
					entityManager.remove( site2 );
					entityManager.remove( site3 );
				}
		);
	}

	@Test
	public void testRevisionsPropertyEqQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					List revs_id1 = AuditReaderFactory.get( entityManager ).createQuery()
							.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
							.add( AuditEntity.id().eq( id1 ) )
							.getResultList();

					assertThat( revs_id1 ).hasSize( 2 );
					assertThat( ((Object[]) revs_id1.get( 0 ))[0] ).isEqualTo( new StrIntTestEntity( "a", 10, id1 ) );
					assertThat( ((Object[]) revs_id1.get( 1 ))[0] ).isEqualTo( new StrIntTestEntity( "a", 10, id1 ) );

				}
		);
	}

	@Test
	@JiraKey(value = "HHH-7800")
	public void testMaximizeInDisjunction(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
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

					List<?> beforeDeletionRevisions = AuditReaderFactory.get( entityManager ).createQuery()
							.forRevisionsOfEntity( StrIntTestEntity.class, false, false )
							.add( disjunction )
							.addOrder( AuditEntity.property( "id" ).asc() )
							.getResultList();

					assertThat( beforeDeletionRevisions ).hasSize( 2 );

					Object[] result1 = (Object[]) beforeDeletionRevisions.get( 0 );
					Object[] result2 = (Object[]) beforeDeletionRevisions.get( 1 );

					assertThat( result1[0] ).isEqualTo( new StrIntTestEntity( "b", 20, id2 ) );
					// Making sure that we have received an entity added at revision 3.
					assertThat( ((SequenceIdRevisionEntity) result1[1]).getId() ).isEqualTo( 3 );
					assertThat( result1[2] ).isEqualTo( RevisionType.ADD );
					assertThat( result2[0] ).isEqualTo( new StrIntTestEntity( "c", 30, id3 ) );
					// Making sure that we have received an entity added at revision 3.
					assertThat( ((SequenceIdRevisionEntity) result2[1]).getId() ).isEqualTo( 3 );
					assertThat( result2[2] ).isEqualTo( RevisionType.ADD );
				}
		);

	}
}
