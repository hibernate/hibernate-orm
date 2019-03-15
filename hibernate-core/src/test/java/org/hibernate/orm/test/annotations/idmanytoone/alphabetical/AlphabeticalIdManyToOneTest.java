/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.idmanytoone.alphabetical;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@Disabled("Composite non-aggregated Id not yet implemented")
public class AlphabeticalIdManyToOneTest extends SessionFactoryBasedFunctionalTest {
	@Test
	public void testAlphabeticalTest() {
		//test through deployment
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { B.class, C.class, A.class };
	}
}
