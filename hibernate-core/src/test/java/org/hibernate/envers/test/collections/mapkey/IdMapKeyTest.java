/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.mapkey;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.collections.mapkey.IdMapKeyEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class IdMapKeyTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer imke_id;

	private Integer ste1_id;
	private Integer ste2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IdMapKeyEntity.class, StrTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		// todo (6.0) - Maxmimum fetch depth handling seems to be problematic with ValidityAuditStrategy.
		//		At line 92, this test would fail without the following configuration property because the
		//		navigableReferenceStack depth is 7 which exceeds the maximumDepth default of 5.
		//		This lead to the sqlSelections not being resolved and therefore a select-clause that had
		//		absolutely no selectables; thus a SQL syntax exception.
		//

		// todo (6.0) - This should be fixed in ORM and this requirement of maximum-fetch depth removed.
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, 10 );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		entityManagerFactoryScope().inTransactions(
				// Revision 1 (initially 1 mapping)
				entityManager -> {
					final IdMapKeyEntity imke = new IdMapKeyEntity();

					final StrTestEntity ste1 = new StrTestEntity( "x" );
					final StrTestEntity ste2 = new StrTestEntity( "y" );

					entityManager.persist( ste1 );
					entityManager.persist( ste2 );

					imke.getIdmap().put( ste1.getId(), ste1 );

					entityManager.persist( imke );

					this.imke_id = imke.getId();
					this.ste1_id = ste1.getId();
					this.ste2_id = ste2.getId();
				},

				// Revision 2 (sse1: adding 1 mapping)
				entityManager -> {
					final StrTestEntity ste2 = entityManager.find( StrTestEntity.class, ste2_id );
					final IdMapKeyEntity imke = entityManager.find( IdMapKeyEntity.class, imke_id );

					imke.getIdmap().put( ste2.getId(), ste2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( IdMapKeyEntity.class, imke_id ), hasItems( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfImke() {
		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final StrTestEntity ste1 = entityManager.find( StrTestEntity.class, ste1_id );
					final StrTestEntity ste2 = entityManager.find( StrTestEntity.class, ste2_id );

					final IdMapKeyEntity rev1 = getAuditReader().find( IdMapKeyEntity.class, imke_id, 1 );
					final IdMapKeyEntity rev2 = getAuditReader().find( IdMapKeyEntity.class, imke_id, 2 );

					assertThat( rev1.getIdmap().entrySet(), hasSize( 1 ) );
					assertThat( rev1.getIdmap(), hasEntry( ste1.getId(), ste1 ) );

					assertThat( rev2.getIdmap().entrySet(), hasSize( 2 ) );
					assertThat( rev2.getIdmap(), hasEntry( ste1.getId(), ste1 ) );
					assertThat( rev2.getIdmap(), hasEntry( ste2.getId(), ste2 ) );
				}
		);
	}
}