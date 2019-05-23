/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.components.Component1;
import org.hibernate.envers.test.support.domains.components.Component2;
import org.hibernate.envers.test.support.domains.components.ComponentTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ComponentsTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ComponentTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					ComponentTestEntity cte1 = new ComponentTestEntity( new Component1( "a", "b" ), new Component2( "x", "y" ) );
					ComponentTestEntity cte2 = new ComponentTestEntity(
							new Component1( "a2", "b2" ), new Component2(
							"x2",
							"y2"
					)
					);
					ComponentTestEntity cte3 = new ComponentTestEntity(
							new Component1( "a3", "b3" ), new Component2(
							"x3",
							"y3"
					)
					);
					ComponentTestEntity cte4 = new ComponentTestEntity( null, null );

					entityManager.persist( cte1 );
					entityManager.persist( cte2 );
					entityManager.persist( cte3 );
					entityManager.persist( cte4 );

					id1 = cte1.getId();
					id2 = cte2.getId();
					id3 = cte3.getId();
					id4 = cte4.getId();
				},

				// Revision 2
				entityManager -> {
					ComponentTestEntity cte1 = entityManager.find( ComponentTestEntity.class, id1 );
					ComponentTestEntity cte2 = entityManager.find( ComponentTestEntity.class, id2 );
					ComponentTestEntity cte3 = entityManager.find( ComponentTestEntity.class, id3 );
					ComponentTestEntity cte4 = entityManager.find( ComponentTestEntity.class, id4 );

					cte1.setComp1( new Component1( "a'", "b'" ) );
					cte2.getComp1().setStr1( "a2'" );
					cte3.getComp2().setStr6( "y3'" );
					cte4.setComp1( new Component1() );
					cte4.getComp1().setStr1( "n" );
					cte4.setComp2( new Component2() );
					cte4.getComp2().setStr5( "m" );
				},

				// Revision 3
				entityManager -> {
					ComponentTestEntity cte1 = entityManager.find( ComponentTestEntity.class, id1 );
					ComponentTestEntity cte2 = entityManager.find( ComponentTestEntity.class, id2 );
					ComponentTestEntity cte3 = entityManager.find( ComponentTestEntity.class, id3 );
					ComponentTestEntity cte4 = entityManager.find( ComponentTestEntity.class, id4 );

					cte1.setComp2( new Component2( "x'", "y'" ) );
					cte3.getComp1().setStr2( "b3'" );
					cte4.setComp1( null );
					cte4.setComp2( null );
				},

				// Revision 4
				entityManager -> {
					ComponentTestEntity cte2 = entityManager.find( ComponentTestEntity.class, id2 );
					entityManager.remove( cte2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ComponentTestEntity.class, id1 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( ComponentTestEntity.class, id2 ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( ComponentTestEntity.class, id3 ), contains( 1, 3 ) );
		assertThat( getAuditReader().getRevisions( ComponentTestEntity.class, id4 ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		ComponentTestEntity ver1 = new ComponentTestEntity( id1, new Component1( "a", "b" ), null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id1, new Component1( "a'", "b'" ), null );

		assertThat( getAuditReader().find( ComponentTestEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id1, 2 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id1, 3 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id1, 4 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfId2() {
		ComponentTestEntity ver1 = new ComponentTestEntity( id2, new Component1( "a2", "b2" ), null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id2, new Component1( "a2'", "b2" ), null );

		assertThat( getAuditReader().find( ComponentTestEntity.class, id2, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id2, 2 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id2, 3 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id2, 4 ), nullValue() );
	}

	@DynamicTest
	public void testHistoryOfId3() {
		ComponentTestEntity ver1 = new ComponentTestEntity( id3, new Component1( "a3", "b3" ), null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id3, new Component1( "a3", "b3'" ), null );

		assertThat( getAuditReader().find( ComponentTestEntity.class, id3, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id3, 2 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id3, 3 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id3, 4 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfId4() {
		ComponentTestEntity ver1 = new ComponentTestEntity( id4, null, null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id4, new Component1( "n", null ), null );
		ComponentTestEntity ver3 = new ComponentTestEntity( id4, null, null );

		assertThat( getAuditReader().find( ComponentTestEntity.class, id4, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id4, 2 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id4, 3 ), equalTo( ver3 ) );
		assertThat( getAuditReader().find( ComponentTestEntity.class, id4, 4 ), equalTo( ver3 ) );
	}
}
