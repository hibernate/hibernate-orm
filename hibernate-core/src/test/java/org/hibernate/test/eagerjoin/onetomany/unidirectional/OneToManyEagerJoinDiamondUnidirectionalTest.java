/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.eagerjoin.onetomany.unidirectional;

import org.hibernate.stat.Statistics;

import org.hibernate.test.eagerjoin.onetomany.OneToManyEagerJoinDiamondTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Pit Humke
 */
public class OneToManyEagerJoinDiamondUnidirectionalTest extends OneToManyEagerJoinDiamondTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Top.class,
				Left.class,
				Right.class,
				Bottom.class
		};
	}

	/**
	 * Entity-Relationship-Diagram:
	 *
	 *            +-----+
	 *   1  +-----+ TOP +------+  1
	 *      |     +-----+      |
	 *   N  |                  |  N
	 *      v                  v
	 *   +----+             +-----+
	 *   |LEFT|             |RIGHT|
	 *   +----+             +-----+
	 *      |                  |
	 *   1  |                  |  1
	 *      |     +------+     |
	 *   N  +---> |BOTTOM| <---+  N
	 *            +------+
	 */
	@Before
	public void createTestData() {
		runInTransaction( session -> {

			Top top = new Top();
			top.id = "TOP";

			Left left = new Left();
			left.id = "LEFT";
			top.left.add( left );

			Bottom bottom_left = new Bottom();
			bottom_left.id = "BOTTOM_LEFT";
			left.bottom.add( bottom_left );

			Right right = new Right();
			right.id = "RIGHT";
			top.right.add( right );

			Bottom bottom_right = new Bottom();
			bottom_right.id = "BOTTOM_RIGHT";
			right.bottom.add( bottom_right );

			session.save( top );
		} );
	}

	@Test
	public void test() {
		Statistics statistics = runInTransaction( session -> {

			Top top = session.get( Top.class, "TOP" );
			assertNotNull( top );
			assertEquals( "TOP", top.id );

			Left left = top.left.iterator().next();
			assertNotNull( left );
			assertEquals( "LEFT", left.id );

			Bottom bottom_left = left.bottom.iterator().next();
			assertNotNull( bottom_left );
			assertEquals( "BOTTOM_LEFT", bottom_left.id );

			Right right = top.right.iterator().next();
			assertNotNull( right );
			assertEquals( "RIGHT", right.id );

			Bottom bottom_right = right.bottom.iterator().next();
			assertNotNull( bottom_right );
			assertEquals( "BOTTOM_RIGHT", bottom_right.id );
		} );

		assertEquals( 1, statistics.getPrepareStatementCount() );
	}
}
