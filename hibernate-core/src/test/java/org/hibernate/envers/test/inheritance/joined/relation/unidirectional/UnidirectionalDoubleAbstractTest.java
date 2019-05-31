/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.inheritance.joined.relation.unidirectional;

import java.util.Set;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.joined.relation.unidirectional.AbstractContainedEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.relation.unidirectional.AbstractSetEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.relation.unidirectional.ContainedEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.relation.unidirectional.SetEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Joined Inheritance Support")
public class UnidirectionalDoubleAbstractTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long cce1_id;
	private Integer cse1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AbstractContainedEntity.class,
				AbstractSetEntity.class,
				ContainedEntity.class,
				SetEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				// Revision 1
				entityManager -> {
					ContainedEntity cce1 = new ContainedEntity();
					entityManager.persist( cce1 );

					SetEntity cse1 = new SetEntity();
					cse1.getEntities().add( cce1 );
					entityManager.persist( cse1 );

					this.cce1_id = cce1.getId();
					this.cse1_id = cse1.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ContainedEntity.class, cce1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( SetEntity.class, cse1_id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfReferencedCollection() {
		ContainedEntity cce1 = inTransaction( em -> { return em.find( ContainedEntity.class, cce1_id ); } );

		Set<AbstractContainedEntity> entities = getAuditReader().find( SetEntity.class, cse1_id, 1 ).getEntities();
		assertThat( entities, CollectionMatchers.hasSize( 1 ) );
		assertThat( entities.iterator().next(), instanceOf( ContainedEntity.class ) );
		assertThat( entities, contains( cce1 ) );
	}
}