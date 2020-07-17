/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.interfaces;


import org.hibernate.testing.orm.junit.DomainModel;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				ContactImpl.class, UserImpl.class
		}
)
public class InterfacesTest {
	@Test
	public void testInterface() {
		// test via SessionFactory building
	}

}
