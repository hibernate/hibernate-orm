/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicClassAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class SimpleClassAuditedTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { BasicClassAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final BasicClassAuditedEntity entity = new BasicClassAuditedEntity( "x", "y" );
					entityManager.persist( entity );
					this.id1 = entity.getId();
				},

				// Revision 2
				entityManager -> {
					final BasicClassAuditedEntity entity = entityManager.find( BasicClassAuditedEntity.class, id1 );
					entity.setStr1( "a" );
					entity.setStr2( "b" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( BasicClassAuditedEntity.class, id1 ), hasItems( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		final BasicClassAuditedEntity ver1 = new BasicClassAuditedEntity( id1, "x", "y" );
		assertThat( getAuditReader().find( BasicClassAuditedEntity.class, id1, 1 ), equalTo( ver1 ) );

		final BasicClassAuditedEntity ver2 = new BasicClassAuditedEntity( id1, "a", "b" );
		assertThat( getAuditReader().find( BasicClassAuditedEntity.class, id1, 2 ), equalTo( ver2 ) );
	}
}