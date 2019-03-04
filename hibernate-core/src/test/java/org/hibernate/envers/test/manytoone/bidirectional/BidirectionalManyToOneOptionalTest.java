/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytoone.bidirectional;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.manytoone.bidirectional.BiRefedOptionalEntity;
import org.hibernate.envers.test.support.domains.manytoone.bidirectional.BiRefingOptionalEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-8305")
public class BidirectionalManyToOneOptionalTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer referringNoReferenceId;
	private Integer referringId;
	private Integer referencedId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				BiRefingOptionalEntity.class,
				BiRefedOptionalEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					// store refing with null refed entity
					BiRefingOptionalEntity referringNoReferenceEntity = new BiRefingOptionalEntity();
					referringNoReferenceEntity.setReference( null );
					entityManager.persist( referringNoReferenceEntity );

					// store refing with non-null refed entity
					BiRefingOptionalEntity referringWithReferenceEntity = new BiRefingOptionalEntity();
					BiRefedOptionalEntity referenceEntity = new BiRefedOptionalEntity();
					referenceEntity.getReferences().add( referringWithReferenceEntity );
					referringWithReferenceEntity.setReference( referenceEntity );
					entityManager.persist( referringWithReferenceEntity );
					entityManager.persist( referenceEntity );

					this.referringId = referringWithReferenceEntity.getId();
					this.referencedId = referenceEntity.getId();
					this.referringNoReferenceId = referringNoReferenceEntity.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( BiRefingOptionalEntity.class, referringId ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( BiRefingOptionalEntity.class, referringNoReferenceId ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( BiRefedOptionalEntity.class, referencedId ), contains( 1 ) );
	}

	@DynamicTest
	public void testRevisionHistoryNullReference() {
		BiRefingOptionalEntity rev1 = getAuditReader().find( BiRefingOptionalEntity.class, referringNoReferenceId, 1 );
		assertThat( rev1, notNullValue() );
		assertThat( rev1.getReference(), nullValue() );
	}

	@DynamicTest
	public void testRevisionHistoryWithNonNullReference() {
		BiRefingOptionalEntity referringRev1 = getAuditReader().find( BiRefingOptionalEntity.class, referringId, 1 );
		assertThat( referringRev1.getReference(), notNullValue() );

		BiRefedOptionalEntity referencedRev1 = getAuditReader().find( BiRefedOptionalEntity.class, referencedId, 1 );
		assertThat( referencedRev1.getReferences(), CollectionMatchers.hasSize( 1 ) );
	}
}
