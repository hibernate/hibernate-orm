/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationTests extends BaseSessionFactoryFunctionalTest {
	@Test
	@FailureExpected( "Still need execution test(s)" )
	public void testSerialInstantiations() {
		fail();
	}

	@Test
	@FailureExpected( "Still need execution test(s)" )
	public void testNestedInstantiations() {
		fail();
	}

	@Test
	@FailureExpected( "Still need execution test(s)" )
	public void testSerialInstantiationsWithNested() {
		fail();
	}

	@Test
	@FailureExpected( "Still need execution test(s)" )
	public void testEntityValuedArgumentsAsEntity() {
		fail();
	}

	@Test
	@FailureExpected( "Still need execution test(s)" )
	public void testEntityValuedArgumentsAsId() {
		fail();
	}
}
