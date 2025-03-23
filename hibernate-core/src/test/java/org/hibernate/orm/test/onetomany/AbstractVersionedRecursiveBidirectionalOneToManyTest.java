/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany;

import static org.junit.Assert.assertEquals;

/**
 * @author Burkhard Graves
 * @author Gail Badner
 */
public abstract class AbstractVersionedRecursiveBidirectionalOneToManyTest
		extends AbstractRecursiveBidirectionalOneToManyTest {

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/onetomany/VersionedNode.hbm.xml" };
	}

	@Override
	void check(boolean simplePropertyUpdated) {
		super.check( simplePropertyUpdated );
		inTransaction(
				session -> {
					Node node1 = session.get( Node.class, Integer.valueOf( 1 ) );
					Node node2 = session.get( Node.class, Integer.valueOf( 2 ) );
					Node node3 = session.get( Node.class, Integer.valueOf( 3 ) );
					assertEquals( 1, node1.getVersion() );
					assertEquals( 1, node2.getVersion() );
					assertEquals( 1, node3.getVersion() );
				}
		);
	}
}
