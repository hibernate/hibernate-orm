/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.notinsertable;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.notinsertable.NotInsertableTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotInsertableTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { NotInsertableTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					final NotInsertableTestEntity dte = new NotInsertableTestEntity( "data1" );
					entityManager.persist( dte );
					id1 = dte.getId();
				},

				entityManager -> {
					final NotInsertableTestEntity dte = entityManager.find( NotInsertableTestEntity.class, id1 );
					dte.setData( "data2" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( NotInsertableTestEntity.class, id1), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		NotInsertableTestEntity ver1 = new NotInsertableTestEntity( id1, "data1", "data1" );
		NotInsertableTestEntity ver2 = new NotInsertableTestEntity( id1, "data2", "data2" );

		assertThat( getAuditReader().find( NotInsertableTestEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( NotInsertableTestEntity.class, id1, 2 ), equalTo( ver2 ) );
	}
}