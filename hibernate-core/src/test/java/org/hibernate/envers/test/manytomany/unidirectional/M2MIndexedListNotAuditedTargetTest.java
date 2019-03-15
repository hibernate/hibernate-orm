/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany.unidirectional;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.UnversionedStrTestEntity;
import org.hibernate.envers.test.support.domains.manytomany.unidirectional.M2MIndexedListTargetNotAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;

/**
 * A test for auditing a many-to-many indexed list where the target entity is not audited.
 *
 * @author Vladimir Klyushnikov
 * @author Adam Warski
 */
public class M2MIndexedListNotAuditedTargetTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer itnae1_id;
	private Integer itnae2_id;

	private UnversionedStrTestEntity uste1;
	private UnversionedStrTestEntity uste2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { UnversionedStrTestEntity.class, M2MIndexedListTargetNotAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {
					uste1 = new UnversionedStrTestEntity( "str1" );
					uste2 = new UnversionedStrTestEntity( "str2" );

					// No revision
					entityManager.getTransaction().begin();

					entityManager.persist( uste1 );
					entityManager.persist( uste2 );

					entityManager.getTransaction().commit();

					// Revision 1
					entityManager.getTransaction().begin();

					uste1 = entityManager.find( UnversionedStrTestEntity.class, uste1.getId() );
					uste2 = entityManager.find( UnversionedStrTestEntity.class, uste2.getId() );

					M2MIndexedListTargetNotAuditedEntity itnae1 = new M2MIndexedListTargetNotAuditedEntity( 1, "tnae1" );
					itnae1.getReferences().add( uste1 );
					itnae1.getReferences().add( uste2 );

					entityManager.persist( itnae1 );

					entityManager.getTransaction().commit();

					// Revision 2
					entityManager.getTransaction().begin();

					M2MIndexedListTargetNotAuditedEntity itnae2 = new M2MIndexedListTargetNotAuditedEntity( 2, "tnae2" );
					itnae2.getReferences().add( uste2 );

					entityManager.persist( itnae2 );

					entityManager.getTransaction().commit();

					// Revision 3
					entityManager.getTransaction().begin();
					itnae1.getReferences().set( 0, uste2 );
					itnae1.getReferences().set( 1, uste1 );
					entityManager.getTransaction().commit();

					itnae1_id = itnae1.getId();
					itnae2_id = itnae2.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( M2MIndexedListTargetNotAuditedEntity.class, itnae1_id ), contains( 1, 3 ) );
		assertThat( getAuditReader().getRevisions( M2MIndexedListTargetNotAuditedEntity.class, itnae2_id ), contains( 2 ) );
	}

	@DynamicTest
	public void testHistory1() {
		M2MIndexedListTargetNotAuditedEntity rev1 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae1_id,
				1
		);
		M2MIndexedListTargetNotAuditedEntity rev2 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae1_id,
				2
		);
		M2MIndexedListTargetNotAuditedEntity rev3 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae1_id,
				3
		);

		assertThat( rev1.getReferences(), contains( uste1, uste2 ) );
		assertThat( rev2.getReferences(), contains( uste1, uste2 ) );
		assertThat( rev3.getReferences(), contains( uste2, uste1 ) );
	}

	@DynamicTest
	public void testHistory2() {
		M2MIndexedListTargetNotAuditedEntity rev1 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae2_id,
				1
		);
		M2MIndexedListTargetNotAuditedEntity rev2 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae2_id,
				2
		);
		M2MIndexedListTargetNotAuditedEntity rev3 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae2_id,
				3
		);

		assertThat( rev1, nullValue() );
		assertThat( rev2.getReferences(), contains( uste2 ) );
		assertThat( rev3.getReferences(), contains( uste2 ) );
	}
}
