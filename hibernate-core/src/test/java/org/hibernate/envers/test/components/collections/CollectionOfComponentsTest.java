/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components.collections;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.components.Component1;
import org.hibernate.envers.test.support.domains.components.ComponentSetTestEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Felix Feisst
 */
public class CollectionOfComponentsTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ComponentSetTestEntity.class};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final ComponentSetTestEntity cte1 = new ComponentSetTestEntity();

					final ComponentSetTestEntity cte2 = new ComponentSetTestEntity();
					cte2.getComps().add( new Component1( "string1", null ) );

					entityManager.persist( cte2 );
					entityManager.persist( cte1 );

					this.id1 = cte1.getId();
					this.id2 = cte2.getId();
				},

				// Revision 2
				entityManager -> {
					final ComponentSetTestEntity cte1 = entityManager.find( ComponentSetTestEntity.class, id1 );
					cte1.getComps().add( new Component1( "a", "b" ) );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ComponentSetTestEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		final ComponentSetTestEntity rev1 = getAuditReader().find( ComponentSetTestEntity.class, id1, 1 );
		assertThat( rev1.getComps(), CollectionMatchers.isEmpty() );

		final ComponentSetTestEntity rev2 = getAuditReader().find( ComponentSetTestEntity.class, id1, 2 );
		assertThat( rev2.getComps(), contains( new Component1( "a", "b" ) ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-8968")
	public void testCollectionOfEmbeddableWithNullValue() {
		final ComponentSetTestEntity entityV1 = getAuditReader().find( ComponentSetTestEntity.class, id2, 1 );
		assertThat( entityV1.getComps(), contains( new Component1( "string1", null ) ) );
	}
}