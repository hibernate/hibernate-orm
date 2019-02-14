/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicPartialNotAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class NotAuditedExplicitPropertyTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicPartialNotAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = inTransaction(
				entityManager -> {
					final BasicPartialNotAuditedEntity entity = new BasicPartialNotAuditedEntity( "a1", "b1" );
					entityManager.persist( entity );
					return entity.getId();
				}
		);

		inTransaction(
				entityManager -> {
					final BasicPartialNotAuditedEntity entity = entityManager.find(
							BasicPartialNotAuditedEntity.class,
							this.id
					);
					entity.setData1( "a2" );
					entity.setData2( "b2" );
				}
		);
	}

	@DynamicTest
	public void testRevisionCount() {
		assertThat( getAuditReader().getRevisions( BasicPartialNotAuditedEntity.class, id ), hasItems( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		BasicPartialNotAuditedEntity ver1 = new BasicPartialNotAuditedEntity( id, "a1", null );
		BasicPartialNotAuditedEntity ver2 = new BasicPartialNotAuditedEntity( id, "a2", null );

		assertThat( getAuditReader().find( BasicPartialNotAuditedEntity.class, id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicPartialNotAuditedEntity.class, id, 2 ), is( ver2 ) );
	}
}
