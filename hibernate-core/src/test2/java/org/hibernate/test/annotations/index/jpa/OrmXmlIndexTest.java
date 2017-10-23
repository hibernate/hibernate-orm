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
public class OrmXmlIndexTest extends AbstractJPAIndexTest {
	@Override
	protected String[] getXmlFiles() {
		return new String[] { "org/hibernate/test/annotations/index/jpa/orm-index.xml" };
	}
}
