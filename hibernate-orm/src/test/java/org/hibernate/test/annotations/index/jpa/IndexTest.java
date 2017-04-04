/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.index.jpa;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class IndexTest extends AbstractJPAIndexTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class,
				Dealer.class,
				Importer.class
		};
	}
}
