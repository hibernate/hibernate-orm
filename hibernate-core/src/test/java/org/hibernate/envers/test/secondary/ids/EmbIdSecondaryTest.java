/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.secondary.ids;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.EmbId;
import org.hibernate.envers.test.support.domains.secondary.ids.SecondaryEmbIdTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EmbIdSecondaryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private EmbId id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SecondaryEmbIdTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		id = new EmbId( 1, 2 );

		// Revision 1
		inTransactions(
				// Revision 1
				entityManager -> {
					final SecondaryEmbIdTestEntity ste = new SecondaryEmbIdTestEntity( id, "a", "1" );
					entityManager.persist( ste );
				},

				// Revision 2
				entityManager -> {
					final SecondaryEmbIdTestEntity ste = entityManager.find( SecondaryEmbIdTestEntity.class, id );
					ste.setS1( "b" );
					ste.setS2( "2" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SecondaryEmbIdTestEntity.class, id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId() {
		SecondaryEmbIdTestEntity ver1 = new SecondaryEmbIdTestEntity( id, "a", "1" );
		SecondaryEmbIdTestEntity ver2 = new SecondaryEmbIdTestEntity( id, "b", "2" );

		assertThat( getAuditReader().find( SecondaryEmbIdTestEntity.class, id, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( SecondaryEmbIdTestEntity.class, id, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	@Disabled("NYI - Requires secondary table support - this method requires additional porting")
	public void testTableNames() {
//		assert "sec_embid_versions".equals(
//				((Iterator<Join>)
//						metadata().getEntityBinding(
//								"org.hibernate.envers.test.integration.secondary.ids.SecondaryEmbIdTestEntity_AUD"
//						).getJoinIterator()).next().getTable().getName()
//		);
	}
}