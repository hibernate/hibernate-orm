/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.secondary.ids;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.MulId;
import org.hibernate.envers.test.support.domains.secondary.ids.SecondaryMulIdTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("ClassCastException - EntityJavaDescriptorImpl->EmbeddableJavaDescriptor in EmbeddedJavaDescriptorImpl#resolveJtd")
public class MulIdSecondaryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private MulId id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SecondaryMulIdTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		id = new MulId( 1, 2 );

		inTransactions(
				// Revision 1
				entityManager -> {
					final SecondaryMulIdTestEntity ste = new SecondaryMulIdTestEntity( id, "a", "1" );
					entityManager.persist( ste );
				},

				// Revision 2
				entityManager -> {
					final SecondaryMulIdTestEntity ste = entityManager.find( SecondaryMulIdTestEntity.class, id );
					ste.setS1( "b" );
					ste.setS2( "2" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SecondaryMulIdTestEntity.class, id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId() {
		SecondaryMulIdTestEntity ver1 = new SecondaryMulIdTestEntity( id, "a", "1" );
		SecondaryMulIdTestEntity ver2 = new SecondaryMulIdTestEntity( id, "b", "2" );

		assertThat( getAuditReader().find( SecondaryMulIdTestEntity.class, id, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( SecondaryMulIdTestEntity.class, id, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	@Disabled("NYI - Requires secondary table support - this method requires additional porting")
	public void testTableNames() {
//		assert "sec_mulid_versions".equals(
//				((Iterator<Join>)
//						metadata().getEntityBinding(
//								"org.hibernate.envers.test.integration.secondary.ids.SecondaryMulIdTestEntity_AUD"
//						)
//								.getJoinIterator())
//						.next().getTable().getName()
//		);
	}
}