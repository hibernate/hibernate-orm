/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.detached;

import java.util.HashSet;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.MulId;
import org.hibernate.envers.test.support.domains.ids.MulIdTestEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.ids.SetRefCollEntityMulId;
import org.junit.jupiter.api.Disabled;

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
@Disabled("ClassCastException - EntityJavaDescriptorImpl->EmbeddableJavaDescriptor in EmbeddedJavaDescriptorImpl#resolveJtd")
public class BasicDetachedSetWithMulIdTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private MulId str1_id;
	private MulId str2_id;

	private MulId coll1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MulIdTestEntity.class, SetRefCollEntityMulId.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Setup some initial data
		str1_id = new MulId( 1, 2 );
		str2_id = new MulId( 3, 4 );
		coll1_id = new MulId( 5, 6 );

		inTransactions(
				// Revision 1
				entityManager -> {
					MulIdTestEntity str1 = new MulIdTestEntity( str1_id.getId1(), str1_id.getId2(), "str1" );
					MulIdTestEntity str2 = new MulIdTestEntity( str2_id.getId1(), str2_id.getId2(), "str2" );

					SetRefCollEntityMulId coll1 = new SetRefCollEntityMulId( coll1_id.getId1(), coll1_id.getId2(), "coll1" );

					entityManager.persist( str1 );
					entityManager.persist( str2 );

					coll1.setCollection( new HashSet<>() );
					coll1.getCollection().add( str1 );
					entityManager.persist( coll1 );
				},

				// Revision 2
				entityManager -> {
					MulIdTestEntity str2 = entityManager.find( MulIdTestEntity.class, str2_id );
					SetRefCollEntityMulId coll1 = entityManager.find( SetRefCollEntityMulId.class, coll1_id );
					coll1.getCollection().add( str2 );
				},

				// Revision 3
				entityManager -> {
					MulIdTestEntity str1 = entityManager.find( MulIdTestEntity.class, str1_id );
					SetRefCollEntityMulId coll1 = entityManager.find( SetRefCollEntityMulId.class, coll1_id );
					coll1.getCollection().remove( str1 );
				},

				// Revision 4
				entityManager -> {
					SetRefCollEntityMulId coll1 = entityManager.find( SetRefCollEntityMulId.class, coll1_id );
					coll1.getCollection().clear();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( SetRefCollEntityMulId.class, coll1_id ), contains( 1, 2, 3, 4 ) );

		assertThat( getAuditReader().getRevisions( MulIdTestEntity.class, str1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( MulIdTestEntity.class, str2_id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfColl1() {
		MulIdTestEntity str1 = inTransaction( em -> { return em.find( MulIdTestEntity.class, str1_id ); } );
		MulIdTestEntity str2 = inTransaction( em -> { return em.find( MulIdTestEntity.class, str2_id ); } );

		SetRefCollEntityMulId rev1 = getAuditReader().find( SetRefCollEntityMulId.class, coll1_id, 1 );
		SetRefCollEntityMulId rev2 = getAuditReader().find( SetRefCollEntityMulId.class, coll1_id, 2 );
		SetRefCollEntityMulId rev3 = getAuditReader().find( SetRefCollEntityMulId.class, coll1_id, 3 );
		SetRefCollEntityMulId rev4 = getAuditReader().find( SetRefCollEntityMulId.class, coll1_id, 4 );

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