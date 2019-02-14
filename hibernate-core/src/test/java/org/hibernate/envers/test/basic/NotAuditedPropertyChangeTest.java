/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicPartialAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class NotAuditedPropertyChangeTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicPartialAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = inTransaction(
				entityManager -> {
					final BasicPartialAuditedEntity entity = new BasicPartialAuditedEntity( "x", "a" );
					entityManager.persist( entity );
					return entity.getId();
				}
		);

		// Should not trigger a revision
		inTransaction(
				entityManager -> {
					final BasicPartialAuditedEntity entity = entityManager.find( BasicPartialAuditedEntity.class, this.id );
					entity.setStr1( "x" );
					entity.setStr2( "a" );
				}
		);

		inTransaction(
				entityManager -> {
					final BasicPartialAuditedEntity entity = entityManager.find( BasicPartialAuditedEntity.class, this.id );
					entity.setStr1( "y" );
					entity.setStr2( "b" );
				}
		);

		// Should not trigger a revision
		inTransaction(
				entityManager -> {
					final BasicPartialAuditedEntity entity = entityManager.find( BasicPartialAuditedEntity.class, this.id );
					entity.setStr1( "y" );
					entity.setStr2( "c" );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( BasicPartialAuditedEntity.class, this.id ), hasItems( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		BasicPartialAuditedEntity ver1 = new BasicPartialAuditedEntity( id, "x", null );
		BasicPartialAuditedEntity ver2 = new BasicPartialAuditedEntity( id, "y", null );

		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id, 2 ), is( ver2 ) );
	}
}
