/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs.named.basic;

/**
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 */
public class BasicAnnNamedEntityGraphTest extends AbstractNamedEntityGraphTest{
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

}
