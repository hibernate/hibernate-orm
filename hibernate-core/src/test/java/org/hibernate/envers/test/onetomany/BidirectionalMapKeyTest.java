/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.RefEdMapKeyEntity;
import org.hibernate.envers.test.support.domains.onetomany.RefIngMapKeyEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BidirectionalMapKeyTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { RefIngMapKeyEntity.class, RefEdMapKeyEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 (initially 1 relation: ing1 -> ed)
				entityManager -> {
					final RefEdMapKeyEntity ed = new RefEdMapKeyEntity();;
					entityManager.persist( ed );

					RefIngMapKeyEntity ing1 = new RefIngMapKeyEntity();
					ing1.setData( "a" );
					ing1.setReference( ed );

					RefIngMapKeyEntity ing2 = new RefIngMapKeyEntity();
					ing2.setData( "b" );

					entityManager.persist( ing1 );
					entityManager.persist( ing2 );

					ed_id = ed.getId();

					ing1_id = ing1.getId();
					ing2_id = ing2.getId();
				},

				// Revision 2 (adding second relation: ing2 -> ed)
				entityManager -> {
					final RefEdMapKeyEntity ed = entityManager.find( RefEdMapKeyEntity.class, ed_id );
					final RefIngMapKeyEntity ing2 = entityManager.find( RefIngMapKeyEntity.class, ing2_id );
					ing2.setReference( ed );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( RefEdMapKeyEntity.class, ed_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( RefIngMapKeyEntity.class, ing1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( RefIngMapKeyEntity.class, ing2_id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEd() {
		inTransaction(
				entityManager -> {
					final RefIngMapKeyEntity ing1 = entityManager.find( RefIngMapKeyEntity.class, ing1_id );
					final RefIngMapKeyEntity ing2 = entityManager.find( RefIngMapKeyEntity.class, ing2_id );

					final RefEdMapKeyEntity rev1 = getAuditReader().find( RefEdMapKeyEntity.class, ed_id, 1 );
					final RefEdMapKeyEntity rev2 = getAuditReader().find( RefEdMapKeyEntity.class, ed_id, 2 );

					assertThat( rev1.getIdmap().entrySet(), CollectionMatchers.hasSize( 1 ) );
					assertThat( rev1.getIdmap(), hasEntry( "a", ing1 ) );

					assertThat( rev2.getIdmap().entrySet(), CollectionMatchers.hasSize( 2 ) );
					assertThat( rev2.getIdmap(), hasEntry( "a", ing1 ) );
					assertThat( rev2.getIdmap(), hasEntry( "b", ing2 ) );
				}
		);
	}
}