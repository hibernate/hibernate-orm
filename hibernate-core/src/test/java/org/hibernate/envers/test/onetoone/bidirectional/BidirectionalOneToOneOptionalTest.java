/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetoone.bidirectional;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.BiRefedOptionalEntity;
import org.hibernate.envers.test.support.domains.onetoone.bidirectional.BiRefingOptionalEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
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
@Disabled("Insert into A_B_AUD binds Columns `a_id` and `REV` transposed, causing a referential integrity exception - Are FK column mappings inverted?")
public class BidirectionalOneToOneOptionalTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer refingWithNoRefedId;
	private Integer refingId;
	private Integer refedId;

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
				// Revision 1
				entityManager -> {
					// store refing with null refed entity
					BiRefingOptionalEntity refingWithNoRefed = new BiRefingOptionalEntity();
					refingWithNoRefed.setReference( null );
					entityManager.persist( refingWithNoRefed );

					// store refing with non-null refed entity
					BiRefingOptionalEntity refing = new BiRefingOptionalEntity();
					BiRefedOptionalEntity refed = new BiRefedOptionalEntity();
					refed.setReferencing( refing );
					refing.setReference( refed );
					entityManager.persist( refing );
					entityManager.persist( refed );

					this.refingId = refing.getId();
					this.refedId = refed.getId();
					this.refingWithNoRefedId = refingWithNoRefed.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( BiRefingOptionalEntity.class, refingId ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( BiRefingOptionalEntity.class, refingWithNoRefedId ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( BiRefedOptionalEntity.class, refedId ), contains( 1 ) );
	}

	@DynamicTest
	public void testRevisionHistoryNullReference() {
		assertThat( getAuditReader().find( BiRefingOptionalEntity.class, refingWithNoRefedId, 1 ).getReference(), nullValue() );
	}

	@DynamicTest
	public void testRevisionHistoryWithNonNullReference() {
		assertThat( getAuditReader().find( BiRefingOptionalEntity.class, refingId, 1).getReference(), notNullValue() );
		assertThat( getAuditReader().find( BiRefedOptionalEntity.class, refedId, 1 ).getReferencing(), notNullValue() );
	}
}
