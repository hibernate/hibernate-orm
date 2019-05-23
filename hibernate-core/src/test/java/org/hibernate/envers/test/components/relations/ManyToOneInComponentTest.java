/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components.relations;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.components.relations.ManyToOneComponent;
import org.hibernate.envers.test.support.domains.components.relations.ManyToOneComponentTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ManyToOneInComponentTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer mtocte_id1;
	private Integer ste_id1;
	private Integer ste_id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ManyToOneComponentTestEntity.class, StrTestEntity.class };

	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final StrTestEntity ste1 = new StrTestEntity();
					ste1.setStr( "str1" );

					final StrTestEntity ste2 = new StrTestEntity();
					ste2.setStr( "str2" );

					entityManager.persist( ste1 );
					entityManager.persist( ste2 );

					this.ste_id1 = ste1.getId();
					this.ste_id2 = ste2.getId();
				},

				// Revision 2
				entityManager -> {
					final StrTestEntity ste1 = entityManager.getReference( StrTestEntity.class, ste_id1 );
					ManyToOneComponentTestEntity mtocte1 = new ManyToOneComponentTestEntity(
							new ManyToOneComponent(
									ste1,
									"data1"
							)
					);
					entityManager.persist( mtocte1 );

					this.mtocte_id1 = mtocte1.getId();
				},

				// Revision 3
				entityManager -> {
					final StrTestEntity ste2 = entityManager.getReference( StrTestEntity.class, ste_id2 );
					ManyToOneComponentTestEntity mtocte1 = entityManager.find( ManyToOneComponentTestEntity.class, mtocte_id1 );
					mtocte1.getComp1().setEntity( ste2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ManyToOneComponentTestEntity.class, mtocte_id1 ), contains( 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		final ManyToOneComponentTestEntity ver2 = inTransaction(
				entityManager -> {
					final StrTestEntity ste1 = entityManager.find( StrTestEntity.class, ste_id1 );
					final ManyToOneComponent component = new ManyToOneComponent( ste1, "data1" );
					return new ManyToOneComponentTestEntity( mtocte_id1, component );
				}
		);

		final ManyToOneComponentTestEntity ver3 = inTransaction(
				entityManager -> {
					final StrTestEntity ste2 = entityManager.find( StrTestEntity.class, ste_id2 );
					final ManyToOneComponent component = new ManyToOneComponent( ste2, "data1" );
					return new ManyToOneComponentTestEntity( mtocte_id1, component );
				}
		);

		assertThat( getAuditReader().find( ManyToOneComponentTestEntity.class, mtocte_id1, 1 ), nullValue() );
		assertThat( getAuditReader().find( ManyToOneComponentTestEntity.class, mtocte_id1, 2 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( ManyToOneComponentTestEntity.class, mtocte_id1, 3 ), equalTo( ver3 ) );
	}
}
