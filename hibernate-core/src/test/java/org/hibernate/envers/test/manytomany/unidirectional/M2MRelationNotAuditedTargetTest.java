/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany.unidirectional;

import java.util.ArrayList;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.UnversionedStrTestEntity;
import org.hibernate.envers.test.support.domains.manytomany.unidirectional.M2MTargetNotAuditedEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * A test for auditing a many-to-many relation where the target entity is not audited.
 *
 * @author Adam Warski
 */
public class M2MRelationNotAuditedTargetTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer tnae1_id;
	private Integer tnae2_id;

	private Integer uste1_id;
	private Integer uste2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { M2MTargetNotAuditedEntity.class, UnversionedStrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {
					UnversionedStrTestEntity uste1 = new UnversionedStrTestEntity( "str1" );
					UnversionedStrTestEntity uste2 = new UnversionedStrTestEntity( "str2" );

					// No revision
					entityManager.getTransaction().begin();

					entityManager.persist( uste1 );
					entityManager.persist( uste2 );

					entityManager.getTransaction().commit();

					// Revision 1
					entityManager.getTransaction().begin();

					uste1 = entityManager.find( UnversionedStrTestEntity.class, uste1.getId() );
					uste2 = entityManager.find( UnversionedStrTestEntity.class, uste2.getId() );

					M2MTargetNotAuditedEntity tnae1 = new M2MTargetNotAuditedEntity( 1, "tnae1", new ArrayList<>() );
					M2MTargetNotAuditedEntity tnae2 = new M2MTargetNotAuditedEntity( 2, "tnae2", new ArrayList<>() );
					tnae2.getReferences().add( uste1 );
					tnae2.getReferences().add( uste2 );
					entityManager.persist( tnae1 );
					entityManager.persist( tnae2 );

					entityManager.getTransaction().commit();

					// Revision 2
					entityManager.getTransaction().begin();

					tnae1 = entityManager.find( M2MTargetNotAuditedEntity.class, tnae1.getId() );
					tnae2 = entityManager.find( M2MTargetNotAuditedEntity.class, tnae2.getId() );

					tnae1.getReferences().add( uste1 );
					tnae2.getReferences().remove( uste1 );

					entityManager.getTransaction().commit();

					// Revision 3
					entityManager.getTransaction().begin();

					tnae1 = entityManager.find( M2MTargetNotAuditedEntity.class, tnae1.getId() );
					tnae2 = entityManager.find( M2MTargetNotAuditedEntity.class, tnae2.getId() );

					//field not changed!!!
					tnae1.getReferences().add( uste1 );
					tnae2.getReferences().remove( uste2 );

					entityManager.getTransaction().commit();

					// Revision 4
					entityManager.getTransaction().begin();

					tnae1 = entityManager.find( M2MTargetNotAuditedEntity.class, tnae1.getId() );
					tnae2 = entityManager.find( M2MTargetNotAuditedEntity.class, tnae2.getId() );

					tnae1.getReferences().add( uste2 );
					tnae2.getReferences().add( uste1 );

					entityManager.getTransaction().commit();

					//
					tnae1_id = tnae1.getId();
					tnae2_id = tnae2.getId();
					uste1_id = uste1.getId();
					uste2_id = uste2.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( M2MTargetNotAuditedEntity.class, tnae1_id ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( M2MTargetNotAuditedEntity.class, tnae2_id ), contains( 1, 2, 3, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfTnae1_id() {
		inTransaction(
				entityManager -> {
					UnversionedStrTestEntity uste1 = entityManager.find( UnversionedStrTestEntity.class, uste1_id );
					UnversionedStrTestEntity uste2 = entityManager.find( UnversionedStrTestEntity.class, uste2_id );

					M2MTargetNotAuditedEntity rev1 = getAuditReader().find( M2MTargetNotAuditedEntity.class, tnae1_id, 1 );
					M2MTargetNotAuditedEntity rev2 = getAuditReader().find( M2MTargetNotAuditedEntity.class, tnae1_id, 2 );
					M2MTargetNotAuditedEntity rev3 = getAuditReader().find( M2MTargetNotAuditedEntity.class, tnae1_id, 3 );
					M2MTargetNotAuditedEntity rev4 = getAuditReader().find( M2MTargetNotAuditedEntity.class, tnae1_id, 4 );

					assertThat( rev1.getReferences(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReferences(), contains( uste1 ) );
					assertThat( rev3.getReferences(), contains( uste1 ) );
					assertThat( rev4.getReferences(), contains( uste1, uste2 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfTnae2_id() {
		inTransaction(
				entityManager -> {
					UnversionedStrTestEntity uste1 = entityManager.find( UnversionedStrTestEntity.class, uste1_id );
					UnversionedStrTestEntity uste2 = entityManager.find( UnversionedStrTestEntity.class, uste2_id );

					M2MTargetNotAuditedEntity rev1 = getAuditReader().find( M2MTargetNotAuditedEntity.class, tnae2_id, 1 );
					M2MTargetNotAuditedEntity rev2 = getAuditReader().find( M2MTargetNotAuditedEntity.class, tnae2_id, 2 );
					M2MTargetNotAuditedEntity rev3 = getAuditReader().find( M2MTargetNotAuditedEntity.class, tnae2_id, 3 );
					M2MTargetNotAuditedEntity rev4 = getAuditReader().find( M2MTargetNotAuditedEntity.class, tnae2_id, 4 );

					assertThat( rev1.getReferences(), contains( uste1, uste2 ) );
					assertThat( rev2.getReferences(), contains( uste2 ) );
					assertThat( rev3.getReferences(), CollectionMatchers.isEmpty() );
					assertThat( rev4.getReferences(), contains( uste1 ) );
				}
		);
	}
}
