/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.access;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.access.FieldAccessTypeEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class FieldAccessTypeTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { FieldAccessTypeEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = inTransaction(
				entityManager -> {
					final FieldAccessTypeEntity entity = new FieldAccessTypeEntity( "data" );
					entityManager.persist( entity );
					return entity.getIdSafe();
				}
		);

		inTransaction(
				entityManager -> {
					final FieldAccessTypeEntity entity = entityManager.find( FieldAccessTypeEntity.class, this.id );
					entity.setDataSafe( "data2" );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( FieldAccessTypeEntity.class, id ), hasItems( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		FieldAccessTypeEntity ver1 = new FieldAccessTypeEntity( id, "data" );
		FieldAccessTypeEntity ver2 = new FieldAccessTypeEntity( id, "data2" );

		assertThat( getAuditReader().find( FieldAccessTypeEntity.class, id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( FieldAccessTypeEntity.class, id, 2 ), is( ver2 ) );
	}
}
