/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.SerObject;
import org.hibernate.envers.test.support.domains.basic.SerializableTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Serializable types")
public class SerializablesTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SerializableTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					SerializableTestEntity ste = new SerializableTestEntity( new SerObject( "d1" ) );
					entityManager.persist( ste );
					id1 = ste.getId();
				},

				entityManager -> {
					SerializableTestEntity ste = entityManager.find( SerializableTestEntity.class, id1 );
					ste.setObj( new SerObject( "d2" ) );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SerializableTestEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		SerializableTestEntity ver1 = new SerializableTestEntity( id1, new SerObject( "d1" ) );
		SerializableTestEntity ver2 = new SerializableTestEntity( id1, new SerObject( "d2" ) );

		assertThat( getAuditReader().find( SerializableTestEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( SerializableTestEntity.class, id1, 2 ), equalTo( ver2 ) );
	}
}