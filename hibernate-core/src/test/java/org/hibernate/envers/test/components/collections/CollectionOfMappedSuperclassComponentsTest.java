/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components.collections;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.components.collections.Code;
import org.hibernate.envers.test.support.domains.components.collections.MappedSuperclassComponentSetTestEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;
import org.hibernate.testing.orm.junit.FailureExpected;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9193" )
public class CollectionOfMappedSuperclassComponentsTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MappedSuperclassComponentSetTestEntity.class, Code.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					MappedSuperclassComponentSetTestEntity cte1 = new MappedSuperclassComponentSetTestEntity();
					entityManager.persist( cte1 );

					this.id1 = cte1.getId();
				},

				// Revision 2
				entityManager -> {
					MappedSuperclassComponentSetTestEntity cte1 = entityManager.find( MappedSuperclassComponentSetTestEntity.class, id1 );
					cte1.getComps().add( new Code( 1 ) );
					cte1.getCompsNotAudited().add( new Code( 100 ) );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( MappedSuperclassComponentSetTestEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	@FailureExpected(jiraKey = "HHH-9193")
	public void testHistoryOfId1() {
		MappedSuperclassComponentSetTestEntity entity = getAuditReader().find(
				MappedSuperclassComponentSetTestEntity.class,
				id1,
				1
		);
		assertThat( entity.getComps(), CollectionMatchers.isEmpty() );
		assertThat( entity.getCompsNotAudited(), CollectionMatchers.isEmpty() );

		entity = getAuditReader().find( MappedSuperclassComponentSetTestEntity.class, id1, 2 );

		// TODO: what is the expectation here? The collection is audited, but the embeddable class
		// has no data and it extends a mapped-superclass that is not audited.
		// currently the collection has 1 element that has value AbstractCode.UNDEFINED
		// (which seems wrong). I changed the expected size to 0 which currently fails; is that what
		// should be expected?
		assertThat( entity.getComps(), CollectionMatchers.isEmpty() );

		// The contents of entity.getCompsNotAudited() is unspecified, so no need to test.
	}
}