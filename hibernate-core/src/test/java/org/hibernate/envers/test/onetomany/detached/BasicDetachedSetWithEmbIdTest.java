/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.detached;

import java.util.HashSet;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.EmbId;
import org.hibernate.envers.test.support.domains.ids.EmbIdTestEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.ids.SetRefCollEntityEmbId;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicDetachedSetWithEmbIdTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private EmbId str1_id;
	private EmbId str2_id;

	private EmbId coll1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EmbIdTestEntity.class, SetRefCollEntityEmbId.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Setup some initial data
		str1_id = new EmbId( 1, 2 );
		str2_id = new EmbId( 3, 4 );
		coll1_id = new EmbId( 5, 6 );

		inTransactions(
				// Revision 1
				entityManager -> {
					EmbIdTestEntity str1 = new EmbIdTestEntity( str1_id, "str1" );
					EmbIdTestEntity str2 = new EmbIdTestEntity( str2_id, "str2" );

					SetRefCollEntityEmbId coll1 = new SetRefCollEntityEmbId( coll1_id, "coll1" );

					entityManager.persist( str1 );
					entityManager.persist( str2 );

					coll1.setCollection( new HashSet<>() );
					coll1.getCollection().add( str1 );
					entityManager.persist( coll1 );
				},

				// Revision 2
				entityManager -> {
					EmbIdTestEntity str2 = entityManager.find( EmbIdTestEntity.class, str2_id );
					SetRefCollEntityEmbId coll1 = entityManager.find( SetRefCollEntityEmbId.class, coll1_id );
					coll1.getCollection().add( str2 );
				},

				// Revision 3
				entityManager -> {
					EmbIdTestEntity str1 = entityManager.find( EmbIdTestEntity.class, str1_id );
					SetRefCollEntityEmbId coll1 = entityManager.find( SetRefCollEntityEmbId.class, coll1_id );
					coll1.getCollection().remove( str1 );
				},

				// Revision 4
				entityManager -> {
					SetRefCollEntityEmbId coll1 = entityManager.find( SetRefCollEntityEmbId.class, coll1_id );
					coll1.getCollection().clear();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SetRefCollEntityEmbId.class, coll1_id ), contains( 1, 2, 3, 4 ) );

		assertThat( getAuditReader().getRevisions( EmbIdTestEntity.class, str1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( EmbIdTestEntity.class, str2_id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfColl1() {
		EmbIdTestEntity str1 = inTransaction( em -> { return em.find( EmbIdTestEntity.class, str1_id ); } );
		EmbIdTestEntity str2 = inTransaction( em -> { return em.find( EmbIdTestEntity.class, str2_id ); } );

		SetRefCollEntityEmbId rev1 = getAuditReader().find( SetRefCollEntityEmbId.class, coll1_id, 1 );
		SetRefCollEntityEmbId rev2 = getAuditReader().find( SetRefCollEntityEmbId.class, coll1_id, 2 );
		SetRefCollEntityEmbId rev3 = getAuditReader().find( SetRefCollEntityEmbId.class, coll1_id, 3 );
		SetRefCollEntityEmbId rev4 = getAuditReader().find( SetRefCollEntityEmbId.class, coll1_id, 4 );

		assertThat( rev1.getCollection(), containsInAnyOrder( str1 ) );
		assertThat( rev2.getCollection(), containsInAnyOrder( str1, str2 ) );
		assertThat( rev3.getCollection(), containsInAnyOrder( str2 ) );
		assertThat( rev4.getCollection(), CollectionMatchers.isEmpty() );

		assertThat( rev1.getData(), equalTo( "coll1" ) );
		assertThat( rev2.getData(), equalTo( "coll1" ) );
		assertThat( rev3.getData(), equalTo( "coll1" ) );
		assertThat( rev4.getData(), equalTo( "coll1" ) );
	}
}