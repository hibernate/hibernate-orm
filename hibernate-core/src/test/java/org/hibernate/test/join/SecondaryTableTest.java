/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Copy of the model used in JoinTest, but using annotations rather than hbm.xml to look
 * for the duplicate joins warning
 *
 * @author Steve Ebersole
 */
public class SecondaryTableTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return null;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class, Employee.class, Customer.class, User.class};
	}

	@Test
	public void testIt() {
		// really we have nothing to tes
	}
}

