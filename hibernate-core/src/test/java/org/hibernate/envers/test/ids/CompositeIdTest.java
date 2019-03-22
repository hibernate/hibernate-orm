/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.ids;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.ids.CustomEnum;
import org.hibernate.envers.test.support.domains.ids.EmbId;
import org.hibernate.envers.test.support.domains.ids.EmbIdTestEntity;
import org.hibernate.envers.test.support.domains.ids.EmbIdWithCustomType;
import org.hibernate.envers.test.support.domains.ids.EmbIdWithCustomTypeTestEntity;
import org.hibernate.envers.test.support.domains.ids.MulId;
import org.hibernate.envers.test.support.domains.ids.MulIdTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Usage of UserType extensions")
public class CompositeIdTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private EmbId id1;
	private EmbId id2;
	private MulId id3;
	private MulId id4;
	private EmbIdWithCustomType id5;
	private EmbIdWithCustomType id6;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EmbIdTestEntity.class, MulIdTestEntity.class, EmbIdWithCustomTypeTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		id1 = new EmbId( 1, 2 );
		id2 = new EmbId( 10, 20 );
		id3 = new MulId( 100, 101 );
		id4 = new MulId( 102, 103 );
		id5 = new EmbIdWithCustomType( 25, CustomEnum.NO );
		id6 = new EmbIdWithCustomType( 27, CustomEnum.YES );

		inTransactions(
				// Revision 1
				entityManager -> {
					entityManager.persist( new EmbIdTestEntity( id1, "x" ) );
					entityManager.persist( new MulIdTestEntity( id3.getId1(), id3.getId2(), "a" ) );
					entityManager.persist( new EmbIdWithCustomTypeTestEntity( id5, "c" ) );
				},

				// Revision 2
				entityManager -> {
					entityManager.persist( new EmbIdTestEntity( id2, "y" ) );
					entityManager.persist( new MulIdTestEntity( id4.getId1(), id4.getId2(), "b" ) );
					entityManager.persist( new EmbIdWithCustomTypeTestEntity( id6, "d" ) );
				},

				// Revision 3
				entityManager -> {
					EmbIdTestEntity ete1 = entityManager.find( EmbIdTestEntity.class, id1 );
					EmbIdTestEntity ete2 = entityManager.find( EmbIdTestEntity.class, id2 );
					MulIdTestEntity mte3 = entityManager.find( MulIdTestEntity.class, id3 );
					MulIdTestEntity mte4 = entityManager.find( MulIdTestEntity.class, id4 );
					EmbIdWithCustomTypeTestEntity cte5 = entityManager.find( EmbIdWithCustomTypeTestEntity.class, id5 );
					EmbIdWithCustomTypeTestEntity cte6 = entityManager.find( EmbIdWithCustomTypeTestEntity.class, id6 );

					ete1.setStr1( "x2" );
					ete2.setStr1( "y2" );
					mte3.setStr1( "a2" );
					mte4.setStr1( "b2" );
					cte5.setStr1( "c2" );
					cte6.setStr1( "d2" );
				},

				// Revision 4
				entityManager -> {
					EmbIdTestEntity ete1 = entityManager.find( EmbIdTestEntity.class, id1 );
					EmbIdTestEntity ete2 = entityManager.find( EmbIdTestEntity.class, id2 );
					MulIdTestEntity mte3 = entityManager.find( MulIdTestEntity.class, id3 );
					EmbIdWithCustomTypeTestEntity cte5 = entityManager.find( EmbIdWithCustomTypeTestEntity.class, id5 );
					EmbIdWithCustomTypeTestEntity cte6 = entityManager.find( EmbIdWithCustomTypeTestEntity.class, id6 );

					entityManager.remove( ete1 );
					entityManager.remove( mte3 );
					entityManager.remove( cte6 );

					ete2.setStr1( "y3" );
					cte5.setStr1( "c3" );
				},

				// Revision 5
				entityManager -> {
					EmbIdTestEntity ete2 = entityManager.find( EmbIdTestEntity.class, id2 );
					entityManager.remove( ete2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( EmbIdTestEntity.class, id1 ), contains( 1, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( EmbIdTestEntity.class, id2 ), contains( 2, 3, 4, 5 ) );
		assertThat( getAuditReader().getRevisions( MulIdTestEntity.class, id3 ), contains( 1, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( MulIdTestEntity.class, id4 ), contains( 2, 3 ) );
		assertThat( getAuditReader().getRevisions( EmbIdWithCustomTypeTestEntity.class, id5 ), contains( 1, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( EmbIdWithCustomTypeTestEntity.class, id6 ), contains( 2, 3, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		EmbIdTestEntity ver1 = new EmbIdTestEntity( id1, "x" );
		EmbIdTestEntity ver2 = new EmbIdTestEntity( id1, "x2" );

		assertThat( getAuditReader().find( EmbIdTestEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( EmbIdTestEntity.class, id1, 2 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( EmbIdTestEntity.class, id1, 3 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( EmbIdTestEntity.class, id1, 4 ), nullValue() );
		assertThat( getAuditReader().find( EmbIdTestEntity.class, id1, 5 ), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfId2() {
		EmbIdTestEntity ver1 = new EmbIdTestEntity( id2, "y" );
		EmbIdTestEntity ver2 = new EmbIdTestEntity( id2, "y2" );
		EmbIdTestEntity ver3 = new EmbIdTestEntity( id2, "y3" );

		assertThat( getAuditReader().find( EmbIdTestEntity.class, id2, 1 ), nullValue() );
		assertThat( getAuditReader().find( EmbIdTestEntity.class, id2, 2 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( EmbIdTestEntity.class, id2, 3 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( EmbIdTestEntity.class, id2, 4 ), equalTo( ver3 ) );
		assertThat( getAuditReader().find( EmbIdTestEntity.class, id2, 5 ), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfId3() {
		MulIdTestEntity ver1 = new MulIdTestEntity( id3.getId1(), id3.getId2(), "a" );
		MulIdTestEntity ver2 = new MulIdTestEntity( id3.getId1(), id3.getId2(), "a2" );

		assertThat( getAuditReader().find( MulIdTestEntity.class, id3, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( MulIdTestEntity.class, id3, 2 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( MulIdTestEntity.class, id3, 3 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( MulIdTestEntity.class, id3, 4 ), nullValue() );
		assertThat( getAuditReader().find( MulIdTestEntity.class, id3, 5 ), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfId4() {
		MulIdTestEntity ver1 = new MulIdTestEntity( id4.getId1(), id4.getId2(), "b" );
		MulIdTestEntity ver2 = new MulIdTestEntity( id4.getId1(), id4.getId2(), "b2" );

		assertThat( getAuditReader().find( MulIdTestEntity.class, id4, 1 ), nullValue() );
		assertThat( getAuditReader().find( MulIdTestEntity.class, id4, 2 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( MulIdTestEntity.class, id4, 3 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( MulIdTestEntity.class, id4, 4 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( MulIdTestEntity.class, id4, 5 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfId5() {
		EmbIdWithCustomTypeTestEntity ver1 = new EmbIdWithCustomTypeTestEntity( id5, "c" );
		EmbIdWithCustomTypeTestEntity ver2 = new EmbIdWithCustomTypeTestEntity( id5, "c2" );
		EmbIdWithCustomTypeTestEntity ver3 = new EmbIdWithCustomTypeTestEntity( id5, "c3" );

		assertThat( getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id5, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id5, 2 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id5, 3 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id5, 4 ), equalTo( ver3 ) );
		assertThat( getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id5, 5 ), equalTo( ver3 ) );
	}

	@DynamicTest
	public void testHistoryOfId6() {
		EmbIdWithCustomTypeTestEntity ver1 = new EmbIdWithCustomTypeTestEntity( id6, "d" );
		EmbIdWithCustomTypeTestEntity ver2 = new EmbIdWithCustomTypeTestEntity( id6, "d2" );

		assertThat( getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id6, 1 ), nullValue() );
		assertThat( getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id6, 2 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id6, 3 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id6, 4 ), nullValue() );
		assertThat( getAuditReader().find( EmbIdWithCustomTypeTestEntity.class, id6, 5 ), nullValue() );
	}
}
