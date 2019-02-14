/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class ManyOperationsInTransactionTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer e1Id;
	private Integer e2Id;
	private Integer e3Id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					BasicAuditedEntity e1 = new BasicAuditedEntity( "x", 1 );
					BasicAuditedEntity e2 = new BasicAuditedEntity( "y", 20 );
					entityManager.persist( e1 );
					entityManager.persist( e2 );

					this.e1Id = e1.getId();
					this.e2Id = e2.getId();
				},

				// Revision 2
				entityManager -> {
					BasicAuditedEntity e1 = entityManager.find( BasicAuditedEntity.class, e1Id );
					BasicAuditedEntity e2 = entityManager.find( BasicAuditedEntity.class, e2Id );
					BasicAuditedEntity e3 = new BasicAuditedEntity( "z", 300 );
					e1.setStr1( "x2" );
					e2.setLong1( 21 );
					entityManager.persist( e3 );

					this.e3Id = e3.getId();
				},

				// Revision 3
				entityManager -> {
					BasicAuditedEntity e2 = entityManager.find( BasicAuditedEntity.class, e2Id );
					BasicAuditedEntity e3 = entityManager.find( BasicAuditedEntity.class, e3Id );
					e2.setStr1( "y3" );
					e2.setLong1( 22 );
					e3.setStr1( "z3" );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e1Id ), hasItems( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e2Id ), hasItems( 1, 2, 3 ) );
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e3Id ), hasItems( 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e1Id, "x", 1 );
		BasicAuditedEntity ver2 = new BasicAuditedEntity( e1Id, "x2", 1 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 2 ), is( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity2() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e2Id, "y", 20 );
		BasicAuditedEntity ver2 = new BasicAuditedEntity( e2Id, "y", 21 );
		BasicAuditedEntity ver3 = new BasicAuditedEntity( e2Id, "y3", 22 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 2 ), is( ver2 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 3 ), is( ver3 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity3() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e3Id, "z", 300 );
		BasicAuditedEntity ver2 = new BasicAuditedEntity( e3Id, "z3", 300 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 1 ), nullValue() );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 2 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 3 ), is( ver2 ) );
	}
}
