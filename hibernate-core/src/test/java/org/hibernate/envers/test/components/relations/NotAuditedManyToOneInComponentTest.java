/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components.relations;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.UnversionedStrTestEntity;
import org.hibernate.envers.test.support.domains.components.relations.NotAuditedManyToOneComponent;
import org.hibernate.envers.test.support.domains.components.relations.NotAuditedManyToOneComponentTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotAuditedManyToOneInComponentTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer mtocte_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { NotAuditedManyToOneComponentTestEntity.class, UnversionedStrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		final UnversionedStrTestEntity ste1 = new UnversionedStrTestEntity();
		ste1.setStr( "str1" );

		final UnversionedStrTestEntity ste2 = new UnversionedStrTestEntity();
		ste2.setStr( "str2" );

		inTransactions(
				// No Revision
				entityManager -> {
					entityManager.persist( ste1 );
					entityManager.persist( ste2 );
				},

				// Revision 1
				entityManager -> {
					final NotAuditedManyToOneComponentTestEntity mtocte1 = new NotAuditedManyToOneComponentTestEntity(
							new NotAuditedManyToOneComponent( ste1, "data1" )
					);

					entityManager.persist( mtocte1 );
					mtocte_id1 = mtocte1.getId();
				},

				// No Revision
				entityManager -> {
					final NotAuditedManyToOneComponentTestEntity mtocte1 = entityManager.find(
							NotAuditedManyToOneComponentTestEntity.class,
							mtocte_id1
					);
					mtocte1.getComp1().setEntity( ste2 );
				},

				// Revision 2
				entityManager -> {
					final NotAuditedManyToOneComponentTestEntity mtocte1 = entityManager.find(
							NotAuditedManyToOneComponentTestEntity.class,
							mtocte_id1
					);
					mtocte1.getComp1().setData( "data2" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		NotAuditedManyToOneComponentTestEntity ver1 = new NotAuditedManyToOneComponentTestEntity(
				mtocte_id1,
				new NotAuditedManyToOneComponent( null, "data1" )
		);
		NotAuditedManyToOneComponentTestEntity ver2 = new NotAuditedManyToOneComponentTestEntity(
				mtocte_id1,
				new NotAuditedManyToOneComponent( null, "data2" )
		);

		assertThat( getAuditReader().find( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( NotAuditedManyToOneComponentTestEntity.class, mtocte_id1, 2 ), equalTo( ver2 ) );
	}
}